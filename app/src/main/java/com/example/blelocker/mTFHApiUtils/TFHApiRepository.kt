package com.example.blelocker.mTFHApiUtils

class TFHApiRepository {
    fun getClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/get"
    fun getAcceptedClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/get/accepted"

    fun updateClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/update"
    fun updateAcceptedClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/update/accepted"
    fun updateRejectedClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/update/rejected"

    fun userDeleteTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal"
    fun userDeleteAcceptedTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal/accepted"
    fun userDeleteRejectedTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal/rejected"
}