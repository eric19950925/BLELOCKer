package com.example.blelocker

import android.app.Application
import androidx.room.Room
import com.example.blelocker.BluetoothUtils.BleCmdRepository
import com.example.blelocker.BluetoothUtils.BleControlViewModel
import com.example.blelocker.BluetoothUtils.BleScanUseCase
import com.example.blelocker.CognitoUtils.CognitoControlViewModel
import com.example.blelocker.Model.LockConnInfoDAO
import com.example.blelocker.Model.LockConnInfoDatabase
import com.example.blelocker.Model.LockConnInfoRepository
import com.example.blelocker.View.AddLock.AddAdminCodeViewModel
import com.example.blelocker.View.AddLock.AdminCodeUseCase
import com.example.blelocker.View.Settings.AutoUnLock.AutoUnlockService
import com.example.blelocker.View.Settings.AutoUnLock.GeofencingViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.polidea.rxandroidble2.RxBleClient

val cognitoModule = module {
//    single { BleCmdRepository() }
    viewModel { CognitoControlViewModel(androidContext()/*,get()*/) }
}

val bluetoothModule = module {
    single { BleCmdRepository() }
    single { BleScanUseCase(RxBleClient.create(androidContext()),get()) }
    viewModel { BleControlViewModel(androidContext(),get(),get(),get()) }
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
    single { AdminCodeUseCase(get()) }
    viewModel { AddAdminCodeViewModel(get(),get()) }

}