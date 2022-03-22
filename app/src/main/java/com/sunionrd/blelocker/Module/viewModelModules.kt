package com.sunionrd.blelocker

import android.app.Application
import androidx.room.Room
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.sunionrd.blelocker.BluetoothUtils.BleCmdRepository
import com.sunionrd.blelocker.BluetoothUtils.BleControlViewModel
import com.sunionrd.blelocker.BluetoothUtils.BleConnectUseCase
import com.sunionrd.blelocker.CognitoUtils.CognitoControlViewModel
import com.sunionrd.blelocker.Model.LockConnInfoDAO
import com.sunionrd.blelocker.Model.LockConnInfoDatabase
import com.sunionrd.blelocker.Model.LockConnInfoRepository
import com.sunionrd.blelocker.View.AddLock.AddAdminCodeViewModel
import com.sunionrd.blelocker.View.AddLock.AdminCodeUseCase
import com.sunionrd.blelocker.View.LockSettingUseCase
import com.sunionrd.blelocker.View.Settings.AutoUnLock.AutoUnlockService
import com.sunionrd.blelocker.View.Settings.AutoUnLock.GeofencingViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.polidea.rxandroidble2.RxBleClient
import java.util.*

val cognitoModule = module {
    single { AWSIotMqttManager(UUID.randomUUID().toString(), CognitoControlViewModel.AWS_IOT_CORE_END_POINT) }
    viewModel { CognitoControlViewModel(androidContext(),get()) }
}

val bluetoothModule = module {
    single { BleCmdRepository() }
    viewModel { BleConnectUseCase(RxBleClient.create(androidContext()),get(),get()) }
    viewModel { BleControlViewModel(androidContext(),get(),get()) }
}


val geofencingModule = module {
    viewModel { GeofencingViewModel(androidContext()) }
    single { AutoUnlockService() }
}

val databaseModule = module {

    //每次開app只在此產生一次實例，直到結束app之前都是使用同一實例 = Singleton pattern
    fun provideDatabase(application: Application): LockConnInfoDatabase {
        return Room.databaseBuilder(application, LockConnInfoDatabase::class.java, "database")
            .fallbackToDestructiveMigration()
//            .allowMainThreadQueries()
// Exception: Cannot access database on the main thread since it may potentially lock the UI for a long period of time.
// Room不允許在主線程做操作，allowMainThreadQueries()是一種霸道的解法 正解需使用coroutines管理thread
            .build()
    }


    fun provideDao(database: LockConnInfoDatabase): LockConnInfoDAO {
        return database.lockConnInfoDao()
    }

    single { provideDatabase(androidApplication()) }
    single { provideDao(get()) }
}

val repositoryModule = module {
    fun provideUserRepository(dao: LockConnInfoDAO): LockConnInfoRepository {
        return LockConnInfoRepository(dao)
    }

    single { provideUserRepository(get()) }
}
val viewModelModules = module {
    // ViewModel for One Lock View

    viewModel { OneLockViewModel(get()) }
    single { LockSettingUseCase(get()) }
    viewModel { HomeViewModel(get()) }
    single { AdminCodeUseCase(get()) }
    viewModel { AddAdminCodeViewModel(get(),get()) }

}