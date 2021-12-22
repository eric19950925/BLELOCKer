package com.example.blelocker

import android.app.Application
import androidx.room.Room
import com.example.blelocker.Model.LockConnInfoDAO
import com.example.blelocker.Model.LockConnInfoDatabase
import com.example.blelocker.Model.LockConnInfoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {

    fun provideDatabase(application: Application): LockConnInfoDatabase {
        return Room.databaseBuilder(application, LockConnInfoDatabase::class.java, "database")
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
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

}