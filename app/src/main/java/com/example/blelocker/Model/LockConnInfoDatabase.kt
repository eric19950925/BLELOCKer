package com.example.blelocker.Model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.blelocker.entity.LockConnectionInformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = arrayOf(LockConnectionInformation::class), version = 1, exportSchema = false)
public abstract class LockConnInfoDatabase : RoomDatabase(){
    abstract fun lockConnInfoDao(): LockConnInfoDAO
    private class LockConnInfoDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    var lockDao = database.lockConnInfoDao()

                    // Delete all content here.
//                    lockDao.deleteAll()

                    // Add sample lock.
                    var sample_lock = LockConnectionInformation(
                        macAddress = "it.macAddress",
                        displayName = "it.displayName",
                        keyOne = "it.keyOne",
                        keyTwo = "it.keyTwo",
                        oneTimeToken = "it.oneTimeToken",
                        permanentToken = "it.permanentToken",
                        isOwnerToken = true,
                        tokenName = "T",
                        sharedFrom = "it.sharedFrom",
                        index = 0,
                        adminCode = "0000"
                    )
                    lockDao.insert(sample_lock)
                }
            }
        }
    }
    companion object{
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: LockConnInfoDatabase?=null

        fun getDatabase(context: Context,scope: CoroutineScope): LockConnInfoDatabase{
            return INSTANCE?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LockConnInfoDatabase::class.java,
                    "LockConnInfo_Database"
                )
                    .addCallback(LockConnInfoDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}