package com.example.blelocker

import android.util.Log
import androidx.lifecycle.ViewModel
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.example.blelocker.Entity.ClassicShadow
import com.example.blelocker.mTFHApiUtils.TFHApiRepository
import com.google.gson.Gson
import java.io.UnsupportedEncodingException

class TFHApiViewModel(private val repo: TFHApiRepository): ViewModel(){

    fun subPubGetClassicShadow(mqttManager: AWSIotMqttManager, callback: (power: Boolean)-> Unit){
        mqttManager.subscribeToTopic(repo.getAcceptedClassicTopic("242779ea-6972-4e2d-b7b5-dbe490e29def"), AWSIotMqttQos.QOS0, AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
            try {
                val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
                Log.d("TAG", "Message arrived:")
                Log.d("TAG", "   Topic       : $topic")
                Log.d("TAG", "   Message     : $message")
                val shadow = Gson().fromJson(message, ClassicShadow::class.java)
                Log.d("TAG",shadow.toString())
                callback(shadow.state.desired.power == "on")
            } catch (e: UnsupportedEncodingException) {
                Log.e("TAG", "Message encoding error.", e)
            }
        })
        mqttManager.publishString("{}", repo.getClassicTopic("242779ea-6972-4e2d-b7b5-dbe490e29def"), AWSIotMqttQos.QOS0)
    }
    fun subPubUpdateClassicShadow(mqttManager: AWSIotMqttManager, power: Boolean, callback: (power: Boolean)-> Unit){
        mqttManager.subscribeToTopic(repo.updateAcceptedClassicTopic("242779ea-6972-4e2d-b7b5-dbe490e29def"), AWSIotMqttQos.QOS0, AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
            try {
                val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
                Log.d("TAG", "Message arrived:")
                Log.d("TAG", "   Topic       : $topic")
                Log.d("TAG", "   Message     : $message")
                val shadow = Gson().fromJson(message, ClassicShadow::class.java)
                Log.d("TAG",shadow.toString())
                callback(shadow.state.desired.power == "on")
            } catch (e: UnsupportedEncodingException) {
                Log.e("TAG", "Message encoding error.", e)
            }
        })
        val currentPower = if(!power)"on" else "off"
        mqttManager.publishString("{\"state\":{\"desired\":{\"welcome\":\"321\",\"color\":\"red\",\"power\":\"${currentPower}\"}}}", repo.updateClassicTopic("242779ea-6972-4e2d-b7b5-dbe490e29def"), AWSIotMqttQos.QOS0)
    }

}