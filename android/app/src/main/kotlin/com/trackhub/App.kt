package com.trackhub

import android.app.Application
import com.trackhub.api.ApiClient
import com.trackhub.api.ApiService
import com.trackhub.api.CookieJarStore
import com.trackhub.session.DekManager
import com.trackhub.session.SessionStore
import org.koin.android.ext.koin.androidContext
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
}
