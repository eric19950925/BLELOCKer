package com.example.blelocker.mTFHApiUtils

class TFHApiRepository {
    fun getClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/get"
    fun getAcceptedClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/get/accepted"

    fun updateClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/update"
    fun updateAcceptedClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/update/accepted"

}