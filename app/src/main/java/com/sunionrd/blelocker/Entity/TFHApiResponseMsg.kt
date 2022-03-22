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

