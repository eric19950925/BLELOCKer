package com.sunionrd.blelocker.Entity

data class TFHApiResponseMsg (
    val API: String, //name must as same as json
    val ResponseBody: TFHApiResponseBody
)

data class TFHApiResponseBody (
    val clientToken: String,
    val timestamp: Long?,
    val message: String?
)

data class AWSNotification (
    val data: AWSNotificationData
    //One Day will add other members
)

data class AWSNotificationData (
    val message: String
    //One Day will add other members
)
