package com.example.blelocker.View

sealed class LockStatusException : Throwable() {
    class LockStatusNotRespondingException : LockStatusException()
    class AdminCodeNotSetException : LockStatusException()
    class LockOrientationException : LockStatusException()
}
