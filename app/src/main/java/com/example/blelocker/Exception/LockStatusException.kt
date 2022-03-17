package com.example.blelocker.Exception

import android.graphics.Bitmap
import android.graphics.Canvas

import android.graphics.PixelFormat

import android.graphics.drawable.Drawable




sealed class LockStatusException : Throwable() {
    class LockStatusNotRespondingException : LockStatusException()
    class AdminCodeNotSetException : LockStatusException()
    class LockOrientationException : LockStatusException()
}


fun drawableToBitmap(drawable: Drawable): Bitmap {
    val w = drawable.intrinsicWidth //獲取寬
    val h = drawable.intrinsicHeight //獲取高
    val btmConfig =
        if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
    val bitmap = Bitmap.createBitmap(w, h, btmConfig)
    //繪製新的bitmap
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    //返回bitmap
    return bitmap
}