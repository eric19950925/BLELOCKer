package com.sunionrd.blelocker

import android.app.Application
import com.sunionrd.blelocker.Module.TFHApiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApplication: Application() {


    override fun onCreate() {
        super.onCreate()
        startKoin {
            // Koin Android logger
            androidLogger()
            //inject Android context
            androidContext(this@MyApplication)
            // use modules
            modules(listOf(
                databaseModule,
                repositoryModule,
                viewModelModules,
                bluetoothModule,
                GithubModule,
                cognitoModule,
                geofencingModule,
                TFHApiModule
            ))
        }
    }
}