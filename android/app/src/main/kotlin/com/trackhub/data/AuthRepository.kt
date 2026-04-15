package com.trackhub.data

import com.trackhub.api.ApiService
import com.trackhub.api.ChangePasswordRequest
import com.trackhub.api.LoginRequest
import com.trackhub.api.LoginResponse
import com.trackhub.api.MeResponse
import com.trackhub.api.RecoverCompleteRequest
import com.trackhub.api.RecoverStartRequest
import com.trackhub.api.SaltRequest
import com.trackhub.crypto.Crypto
import com.trackhub.crypto.RecoveryKey
import com.trackhub.session.DekManager
import retrofit2.HttpException

/**
 * Bridges API + crypto: implements the whole client side of the zero-knowledge
 * auth protocol. Keeps DekManager synced with the in-memory DEK.
 */
class AuthRepository(
    private val api: ApiService,
    private val dekManager: DekManager,
) {

    sealed class LoginResult {
        /** Account already set up. DEK is in memory. */
        data class Authenticated(val dek: ByteArray) : LoginResult()

        /** Server reports must_change_password=true. Caller routes to ChangePasswordScreen. */
        object MustChangePassword : LoginResult()
    }

    suspend fun me(): MeResponse? = try {
        api.me()
    } catch (e: HttpException) {
        if (e.code() == 401) null else throw e
    }

    suspend fun saltAndLogin(username: String, password: String): LoginResult {
        val saltResp = api.salt(SaltRequest(username))
        val salt = Crypto.fromHex(saltResp.saltHex)
        val derived = Crypto.deriveKeysFromPassword(password, salt)

        val resp = api.login(LoginRequest(username = username, authKeyHex = derived.authKeyHex))
        return finishLogin(resp, derived.kek)
    }

    private fun finishLogin(resp: LoginResponse, kek: ByteArray): LoginResult {
        if (resp.mustChangePassword && resp.wrappedDekPasswordHex == null) {
            return LoginResult.MustChangePassword
        }
        val wrappedHex = resp.wrappedDekPasswordHex
            ?: throw IllegalStateException("server reports normal login but no wrapped DEK")
        val dek = Crypto.unwrapBytes(Crypto.fromHex(wrappedHex), kek)
        dekManager.setDekFromUnlock(dek)
        return LoginResult.Authenticated(dek)
    }

    /** First-time password set: generate DEK + recovery key, upload wrapped copies. */
    data class BootstrapResult(val dek: ByteArray, val recoveryKey: ByteArray, val recoveryFormatted: String)

    suspend fun bootstrapAccount(newPassword: String): BootstrapResult {
        val newSalt = Crypto.generateSalt()
        val derived = Crypto.deriveKeysFromPassword(newPassword, newSalt)

        val dek = Crypto.generateDek()
        val recoveryKey = Crypto.generateRecoveryKey()
        val recDerived = Crypto.deriveRecoveryKeys(recoveryKey)

        val wrappedPw = Crypto.wrapBytes(dek, derived.kek)
        val wrappedRec = Crypto.wrapBytes(dek, recDerived.kek)

        api.changePassword(
            ChangePasswordRequest(
                newSaltHex = Crypto.toHex(newSalt),
                newAuthKeyHex = derived.authKeyHex,
                wrappedDekPasswordHex = Crypto.toHex(wrappedPw),
                wrappedDekRecoveryHex = Crypto.toHex(wrappedRec),
                recoveryAuthKeyHex = recDerived.authKeyHex,
            ),
        )
        dekManager.setDekFromUnlock(dek)
        return BootstrapResult(dek, recoveryKey, RecoveryKey.format(recoveryKey))
    }

    /** Subsequent password change. Does NOT touch the recovery wrap. */
    suspend fun changePassword(currentDek: ByteArray, newPassword: String) {
        val newSalt = Crypto.generateSalt()
        val derived = Crypto.deriveKeysFromPassword(newPassword, newSalt)
        val wrappedPw = Crypto.wrapBytes(currentDek, derived.kek)
        api.changePassword(
            ChangePasswordRequest(
                newSaltHex = Crypto.toHex(newSalt),
                newAuthKeyHex = derived.authKeyHex,
                wrappedDekPasswordHex = Crypto.toHex(wrappedPw),
            ),
        )
    }

    suspend fun recoverAccount(username: String, recoveryKeyInput: String, newPassword: String): ByteArray {
        val recoveryKey = RecoveryKey.parse(recoveryKeyInput)
        val recDerived = Crypto.deriveRecoveryKeys(recoveryKey)

        val start = api.recoverStart(RecoverStartRequest(username))
        val dek = Crypto.unwrapBytes(Crypto.fromHex(start.wrappedDekRecoveryHex), recDerived.kek)

        val newSalt = Crypto.generateSalt()
        val derived = Crypto.deriveKeysFromPassword(newPassword, newSalt)
        val wrappedPw = Crypto.wrapBytes(dek, derived.kek)

        api.recoverComplete(
            RecoverCompleteRequest(
                username = username,
                recoveryAuthKeyHex = recDerived.authKeyHex,
                newSaltHex = Crypto.toHex(newSalt),
                newAuthKeyHex = derived.authKeyHex,
                wrappedDekPasswordHex = Crypto.toHex(wrappedPw),
            ),
        )
        dekManager.setDekFromUnlock(dek)
        return dek
    }

    suspend fun logout() {
        runCatching { api.logout() }
        dekManager.clear()
    }

    suspend fun deleteAccount() {
        api.deleteAccount()
        dekManager.clear()
    }
}
