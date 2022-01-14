package com.example.blelocker.Exception

sealed class LockStatusException : Throwable() {
    class LockStatusNotRespondingException : LockStatusException()
    class AdminCodeNotSetException : LockStatusException()
    class LockOrientationException : LockStatusException()
}
