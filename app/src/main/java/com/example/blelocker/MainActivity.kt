package com.example.blelocker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.util.*
import androidx.navigation.findNavController

class MainActivity : AppCompatActivity() {

    //常數設置
    companion object {
        const val CIPHER_MODE = "AES/ECB/NoPadding"
        const val BARCODE_KEY = "SoftChefSunion65"
        val NOTIFICATION_CHARACTERISTIC = UUID.fromString("de915dce-3539-61ea-ade7-d44a2237601f")
        val SUNION_SERVICE_UUID = UUID.fromString("fc3d8cf8-4ddc-7ade-1dd9-2497851131d7")
        const val DATA = "DATA"
        const val MY_LOCK_QRCODE = "MY_LOCK_QRCODE"
        const val MY_LOCK_TOKEN = "MY_LOCK_KEYTWO"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            getSupportActionBar()?.hide()
    }

    override fun onSupportNavigateUp(): Boolean {
        //enter one lock page
        return findNavController(R.id.my_nav_host_fragment).navigateUp()
    }




}


