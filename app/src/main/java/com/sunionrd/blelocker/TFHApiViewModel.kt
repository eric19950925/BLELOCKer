package com.sunionrd.blelocker

import android.util.Log
import androidx.lifecycle.ViewModel
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.sunionrd.blelocker.mTFHApiUtils.TFHApiRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sunionrd.blelocker.CognitoUtils.CognitoControlViewModel
import com.sunionrd.blelocker.Entity.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.io.UnsupportedEncodingException

class TFHApiViewModel(private val repo: TFHApiRepository): ViewModel(){
    private val client = OkHttpClient()
    private val gson = Gson()
    private val MEDIA_TYPE_JSON = CognitoControlViewModel.CONTENT_TYPE_JSON.toMediaType()

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
        try {
            mqttManager.publishString("{}", repo.getClassicTopic("242779ea-6972-4e2d-b7b5-dbe490e29def"), AWSIotMqttQos.QOS0)
        }catch (e: Exception){
            Log.d("TAG",e.toString()
            )
        }
    }
    fun unSubscribeGetTopic(mqttManager: AWSIotMqttManager, function: () -> Unit){
        mqttManager.unsubscribeTopic(repo.getAcceptedClassicTopic("242779ea-6972-4e2d-b7b5-dbe490e29def"))
        function.invoke()
    }
    fun subPubUpdateClassicShadow(mqttManager: AWSIotMqttManager, power: Boolean, callback: (power: Boolean)-> Unit){
        mqttManager.subscribeToTopic(repo.updateAcceptedClassicTopic("242779ea-6972-4e2d-b7b5-dbe490e29def"), AWSIotMqttQos.QOS0, AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
            try {
                val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
                Log.d("TAG", "Message arrived:")
                Log.d("TAG", "   Topic       : $topic")
                Log.d("TAG", "   Message     : $message")
                val shadow = Gson().fromJson(message, ClassicShadow::class.java)
//                Log.d("TAG",shadow.toString())
                callback(shadow.state.desired.power == "on")
            } catch (e: UnsupportedEncodingException) {
                Log.e("TAG", "Message encoding error.", e)
            }
        })
        mqttManager.subscribeToTopic(repo.updateRejectedClassicTopic("242779ea-6972-4e2d-b7b5-dbe490e29def"), AWSIotMqttQos.QOS0, AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
            try {
                val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
                Log.d("TAG", "Message arrived:")
                Log.d("TAG", "   Topic       : $topic")
                Log.d("TAG", "   Message     : $message")
//                val shadow = Gson().fromJson(message, TFHApiResponse::class.java)
//                Log.d("TAG",shadow.state.responseBody.toString())
//                callback(shadow.state.responseBody.message?:"Unknown Error.")
            } catch (e: UnsupportedEncodingException) {
                Log.e("TAG", "Message encoding error.", e)
            }
        })
        val currentPower = if(!power)"on" else "off"
        mqttManager.publishString("{\"state\":{\"desired\":{\"welcome\":\"321\",\"color\":\"red\",\"power\":\"${currentPower}\"}}}", repo.updateClassicTopic("242779ea-6972-4e2d-b7b5-dbe490e29def"), AWSIotMqttQos.QOS0)
    }
    fun subPubUserDelete(mqttManager: AWSIotMqttManager, identityPoolId: String, jwtToken: String, callback: (response: String)-> Unit){
        mqttManager.subscribeToTopic(repo.apiPortalAcceptedTopic(identityPoolId = identityPoolId), AWSIotMqttQos.QOS0, AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
            try {
                val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
                Log.d("TAG", "Message arrived:")
                Log.d("TAG", "   Topic       : $topic")
                Log.d("TAG", "   Message     : $message")
                val shadow = Gson().fromJson(message, TFHApiResponseMsg::class.java)
                Log.d("TAG",shadow.ResponseBody.toString())
                callback(shadow.ResponseBody.clientToken)
            } catch (e: UnsupportedEncodingException) {
                Log.e("TAG", "Message encoding error.", e)
            }
        })
        mqttManager.subscribeToTopic(repo.apiPortalRejectedTopic(identityPoolId = identityPoolId), AWSIotMqttQos.QOS0, AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
            try {
                val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
                Log.d("TAG", "Message arrived:")
                Log.d("TAG", "   Topic       : $topic")
                Log.d("TAG", "   Message     : $message")
                val shadow = Gson().fromJson(message, TFHApiResponseMsg::class.java)
                Log.d("TAG",shadow.ResponseBody.toString())
                callback(shadow.ResponseBody.message?:"Unknown Error.")
            } catch (e: UnsupportedEncodingException) {
                Log.e("TAG", "Message encoding error.", e)
            }
        })
        mqttManager.publishString(
            repo.apiPayLoad("user/delete","\"clientToken\":\"AAA\"", jwtToken = jwtToken),
            repo.apiPortalTopic(identityPoolId = identityPoolId),
            AWSIotMqttQos.QOS0
        )
    }
    fun subPubGetTime(mqttManager: AWSIotMqttManager, identityPoolId: String, jwtToken: String, callback: (response: String)-> Unit){
        mqttManager.subscribeToTopic(repo.apiPortalAcceptedTopic(identityPoolId = identityPoolId), AWSIotMqttQos.QOS0, AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
            try {
                val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
                Log.d("TAG", "Message arrived:")
                Log.d("TAG", "   Topic       : $topic")
                Log.d("TAG", "   Message     : $message")

                Log.d("TAG",message)
                // get JSONObject from JSON file
                val obj = JSONObject(message)
                val responseBody: JSONObject = obj.getJSONObject("ResponseBody")
                val clientToken = responseBody.getString("clientToken")
                callback(clientToken)
            } catch (e: UnsupportedEncodingException) {
                Log.e("TAG", "Message encoding error.", e)
            }
        })
        mqttManager.subscribeToTopic(repo.apiPortalRejectedTopic(identityPoolId = identityPoolId), AWSIotMqttQos.QOS0, AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
            try {
                val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
                Log.d("TAG", "Message arrived:")
                Log.d("TAG", "   Topic       : $topic")
                Log.d("TAG", "   Message     : $message")
//                val shadow = Gson().fromJson(message, TFHApiResponseMsg::class.java)
//                Log.d("TAG",shadow.ResponseBody.toString())
//                callback(shadow.ResponseBody.message?:"Unknown Error.")
                callback(message)
            } catch (e: UnsupportedEncodingException) {
                Log.e("TAG", "Message encoding error.", e)
            }
        })
        mqttManager.publishString(
            repo.apiPayLoad("time","\"TimeZone\":\"Asia/Taipei\",\"clientToken\":\"AAA\"", jwtToken = jwtToken),
            repo.apiPortalTopic(identityPoolId = identityPoolId),
            AWSIotMqttQos.QOS0
        )
    }
    fun setFCM(enable: Boolean, FCMtoken: String, jwtToken: String, callback: (response: String)-> Unit){
        val mSetNotifyPayload = SetNotifyPayload(
            Type = 0,
            Token = FCMtoken,
            ApplicationID = "Sunion_20200617",
            Enable = enable,
            LanguageLocalisation = "en-US",
            clientToken = "AAA"
        )
        val postBody = gson.toJson(mSetNotifyPayload).toString()
        Log.d("TAG", postBody)

        val request = customRequest("app-notification", jwtToken, postBody)

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("TAG", e.toString())
                callback(e.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("TAG", "response: ${response.body?.string()}")
//                callback("response: ${response.body?.string()}")
//                https://stackoverflow.com/questions/58094772/okhttp-response-fail-java-lang-illegalstateexception-closed
            }
        })

    }

    fun deleteAccount(jwtToken: String, callback: (response: String)-> Unit){
        val mDeleteAccountPayload = "{\"clientToken\":\"AAA\"}"
        val postBody = gson.toJson(mDeleteAccountPayload).toString()
        Log.d("TAG", postBody)

        val request = customRequest("user/delete", jwtToken, "{}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("TAG", "api error msg: $e")
                callback(e.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("TAG", "response: ${response.body?.string()}")
//                val res = response.body?.string()?:""
//                val obj = JSONObject(res)
//                Log.d("TAG", "response: $obj")
//                val clientToken = obj.getString("message")
                callback("")
//                https://stackoverflow.com/questions/58094772/okhttp-response-fail-java-lang-illegalstateexception-closed
            }
        })

    }

    private fun customRequest(apiName: String, jwtToken: String, postBody: String): Request = Request.Builder()
        .url("https://api.ikey-lock.com/v1/$apiName")
        .header("Authorization", "Bearer $jwtToken")
        .post(postBody.toRequestBody(MEDIA_TYPE_JSON))
        .build()
}

data class SetNotifyPayload(
    val Type: Int,
    val Token: String,
    val ApplicationID: String,
    val Enable: Boolean,
    val LanguageLocalisation: String,
    val clientToken: String
)