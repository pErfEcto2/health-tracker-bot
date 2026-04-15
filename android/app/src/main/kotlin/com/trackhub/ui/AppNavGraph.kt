package com.trackhub.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trackhub.api.ProfilePayload
import com.trackhub.api.RecordType
import com.trackhub.data.AuthRepository
import com.trackhub.data.RecordsRepository
import com.trackhub.session.DekManager
import com.trackhub.ui.auth.BiometricUnlockScreen
import com.trackhub.ui.auth.ChangePasswordScreen
import com.trackhub.ui.auth.EnrollBiometricScreen
import com.trackhub.ui.auth.LoginScreen
import com.trackhub.ui.auth.RecoveryScreen
import com.trackhub.ui.auth.SetupScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val BIOMETRIC = "biometric"
    const val CHANGE_PW_FIRST = "change_pw/first"
    const val CHANGE_PW = "change_pw"
    const val RECOVERY = "recovery"
    const val ENROLL_BIO = "enroll_bio"
    const val SETUP = "setup"
    const val HOME = "home"
}

class AppBootViewModel : ViewModel(), KoinComponent {
    private val auth: AuthRepository by inject()
    private val records: RecordsRepository by inject()
    private val dekManager: DekManager by inject()

    sealed class Decision {
        object Loading : Decision()
        object GoLogin : Decision()
        object GoBiometric : Decision()       // session valid + DEK wrapped → unlock
        object GoChangePasswordFirst : Decision()
        object GoSetup : Decision()
        object GoHome : Decision()
    }

    private val _decision = MutableStateFlow<Decision>(Decision.Loading)
    val decision = _decision.asStateFlow()

    fun decide() {
        viewModelScope.launch {
            try {
                val me = auth.me()
                _decision.value = when {
                    me == null -> Decision.GoLogin
                    me.mustChangePassword -> Decision.GoChangePasswordFirst
                    !dekManager.hasMemoryDek() && dekManager.hasWrappedDek() &&
                        dekManager.canUseBiometric() -> Decision.GoBiometric
                    !dekManager.hasMemoryDek() -> Decision.GoLogin
                    !isProfileComplete() -> Decision.GoSetup
                    else -> Decision.GoHome
                }
            } catch (_: Exception) {
                _decision.value = Decision.GoLogin
            }
        }
    }

    suspend fun afterAuthDecision(): Decision = when {
        !dekManager.hasMemoryDek() -> Decision.GoLogin
        !isProfileComplete() -> Decision.GoSetup
        else -> Decision.GoHome
    }

    private suspend fun isProfileComplete(): Boolean {
        return try {
            val rec = records.list<ProfilePayload>(type = RecordType.PROFILE).firstOrNull()
            val p = rec?.payload ?: return false
            p.gender != null && p.weightKg != null && p.heightCm != null &&
                !p.birthDate.isNullOrBlank() && p.activityLevel != null
        } catch (_: Exception) { false }
    }
}

@Composable
fun AppNavGraph(
    nav: NavHostController = rememberNavController(),
    boot: AppBootViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) { boot.decide() }
    val decision by boot.decision.collectAsState()

    LaunchedEffect(decision) {
        val target = when (decision) {
            AppBootViewModel.Decision.Loading -> null
            AppBootViewModel.Decision.GoLogin -> Routes.LOGIN
            AppBootViewModel.Decision.GoBiometric -> Routes.BIOMETRIC
            AppBootViewModel.Decision.GoChangePasswordFirst -> Routes.CHANGE_PW_FIRST
            AppBootViewModel.Decision.GoSetup -> Routes.SETUP
            AppBootViewModel.Decision.GoHome -> Routes.HOME
        }
        if (target != null && nav.currentDestination?.route != target) {
            nav.navigate(target) {
                popUpTo(Routes.SPLASH) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { /* empty placeholder; boot decides */ }

        composable(Routes.LOGIN) {
            LoginScreen(
                onAuthenticated = {
                    boot.viewModelScope.launch {
                        when (boot.afterAuthDecision()) {
                            AppBootViewModel.Decision.GoSetup -> nav.navigateAndClear(Routes.SETUP)
                            AppBootViewModel.Decision.GoHome -> {
                                // Offer biometric enroll if available and not yet wrapped.
                                nav.navigateAndClear(Routes.ENROLL_BIO)
                            }
                            else -> nav.navigateAndClear(Routes.LOGIN)
                        }
                    }
                },
                onMustChangePassword = { nav.navigateAndClear(Routes.CHANGE_PW_FIRST) },
                onRecover = { nav.navigate(Routes.RECOVERY) },
            )
        }

        composable(Routes.BIOMETRIC) {
            BiometricUnlockScreen(
                onUnlocked = {
                    boot.viewModelScope.launch {
                        when (boot.afterAuthDecision()) {
                            AppBootViewModel.Decision.GoSetup -> nav.navigateAndClear(Routes.SETUP)
                            else -> nav.navigateAndClear(Routes.HOME)
                        }
                    }
                },
                onFallbackLogin = { nav.navigateAndClear(Routes.LOGIN) },
            )
        }

        composable(Routes.CHANGE_PW_FIRST) {
            ChangePasswordScreen(firstTime = true, onDone = { nav.navigateAndClear(Routes.ENROLL_BIO) })
        }
        composable(Routes.CHANGE_PW) {
            ChangePasswordScreen(firstTime = false, onDone = { nav.popBackStack() })
        }

        composable(Routes.RECOVERY) {
            RecoveryScreen(
                onRecovered = { nav.navigateAndClear(Routes.ENROLL_BIO) },
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.ENROLL_BIO) {
            EnrollBiometricScreen(
                onDone = {
                    boot.viewModelScope.launch {
                        when (boot.afterAuthDecision()) {
                            AppBootViewModel.Decision.GoSetup -> nav.navigateAndClear(Routes.SETUP)
                            else -> nav.navigateAndClear(Routes.HOME)
                        }
                    }
                },
                onSkip = {
                    boot.viewModelScope.launch {
                        when (boot.afterAuthDecision()) {
                            AppBootViewModel.Decision.GoSetup -> nav.navigateAndClear(Routes.SETUP)
                            else -> nav.navigateAndClear(Routes.HOME)
                        }
                    }
                },
            )
        }

        composable(Routes.SETUP) {
            SetupScreen(onDone = { nav.navigateAndClear(Routes.HOME) })
        }

        composable(Routes.HOME) {
            PlaceholderHome(onLoggedOut = { nav.navigateAndClear(Routes.LOGIN) })
        }
    }
}

private fun NavHostController.navigateAndClear(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
}
