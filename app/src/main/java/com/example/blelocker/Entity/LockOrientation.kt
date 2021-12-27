package com.example.blelocker.Entity

sealed class LockOrientation : Throwable() {
    object Right : LockOrientation()
    object Left : LockOrientation()
    object NotDetermined : LockOrientation()
}
