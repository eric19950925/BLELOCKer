package com.sunionrd.blelocker.mTFHApiUtils

class TFHApiRepository {
    fun getClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/get"
    fun getAcceptedClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/get/accepted"

    fun updateClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/update"
    fun updateAcceptedClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/update/accepted"
    fun updateRejectedClassicTopic(thingName: String) = "\$aws/things/${thingName}/shadow/update/rejected"

    fun apiPortalTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal"
    fun apiPortalAcceptedTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal/accepted"
    fun apiPortalRejectedTopic(identityPoolId: String) = "sunion/user/${identityPoolId}/api-portal/rejected"

    fun apiPayLoad(apiName: String, RequestBody: String, jwtToken: String) = "{\"API\":\"$apiName\",\"RequestBody\":{$RequestBody},\"Authorization\":\"$jwtToken\"}"
}