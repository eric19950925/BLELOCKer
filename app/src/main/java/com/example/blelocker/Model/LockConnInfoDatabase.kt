package com.example.blelocker.Model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.blelocker.entity.LockConnectionInformation

@Database(entities = arrayOf(LockConnectionInformation::class), version = 1, exportSchema = false)
public abstract class LockConnInfoDatabase : RoomDatabase(){
    abstract fun lockConnInfoDao(): LockConnInfoDAO

    companion object{
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: LockConnInfoDatabase?=null

        fun getDatabase(context: Context): LockConnInfoDatabase{
            return INSTANCE?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LockConnInfoDatabase::class.java,
                    "LockConnInfo_Database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}