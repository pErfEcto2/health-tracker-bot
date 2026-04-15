package com.trackhub

import android.app.Application
import com.trackhub.api.ApiClient
import com.trackhub.api.ApiService
import com.trackhub.api.CookieJarStore
import com.trackhub.data.AuthRepository
import com.trackhub.data.RecordsRepository
import com.trackhub.session.DekManager
import com.trackhub.session.SessionStore
import com.trackhub.ui.AppBootViewModel
import com.trackhub.ui.PlaceholderHomeViewModel
import com.trackhub.ui.auth.BiometricUnlockViewModel
import com.trackhub.ui.auth.ChangePasswordViewModel
import com.trackhub.ui.auth.EnrollBiometricViewModel
import com.trackhub.ui.auth.LoginViewModel
import com.trackhub.ui.auth.RecoveryViewModel
import com.trackhub.ui.auth.SetupViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
    }
}

val appModule = module {
    single { CookieJarStore(SessionStore.cookies(get())) }
    single<ApiService> { ApiClient.create(get(), debug = BuildConfig.DEBUG) }
    single { DekManager(get()) }
    single { AuthRepository(get(), get()) }
    single { RecordsRepository(get(), get()) }

    viewModel { AppBootViewModel() }
    viewModel { LoginViewModel() }
    viewModel { ChangePasswordViewModel() }
    viewModel { RecoveryViewModel() }
    viewModel { SetupViewModel() }
    viewModel { BiometricUnlockViewModel() }
    viewModel { EnrollBiometricViewModel() }
    viewModel { PlaceholderHomeViewModel() }
}
