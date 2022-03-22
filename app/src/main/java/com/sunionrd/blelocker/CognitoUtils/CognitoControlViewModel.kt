package com.sunionrd.blelocker.CognitoUtils

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazonaws.mobileconnectors.cognitoidentityprovider.*
import com.amazonaws.regions.Regions
import kotlinx.coroutines.launch
import java.lang.Exception
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import kotlinx.coroutines.Dispatchers
import java.util.*
import kotlin.collections.HashMap
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.mobileconnectors.iot.*
import com.amazonaws.auth.AWSSessionCredentials
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.*
import com.sunionrd.blelocker.Entity.MqttStatus
import kotlinx.coroutines.Job
import okhttp3.*
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody


class CognitoControlViewModel(val context: Context, val mqttManager: AWSIotMqttManager): ViewModel() {
    private val poolIDTest = "us-east-2_48Mq3KjSR"
    private val clientIDTest = "78nqui84c8qpl2vspofv9bd34l"
    private val clientSecretTest = "15aigi3rdp7g9lr4pjibvrj8u6fnc03o6edif4iukqtqslt59jrh"
    private var credentialsproviderTest: CognitoCachingCredentialsProvider? = null

    companion object {
        const val USER_POOL_ID = "us-east-1_9H0qG2JDz"
        const val USER_POOL_ADDRESS = "cognito-idp.us-east-1.amazonaws.com/us-east-1_9H0qG2JDz"
        const val USER_POOL_CLIENT_ID = "ai4604ihkdbac53lpqs1kchlp"
        const val USER_POOL_CLIENT_SECRET = "13ems49degn68tfeq5o9q7aa4rbaku52oot35grahvpsdn6msrt0"
        const val IDENTITY_POOL_ID = "us-east-1:8266c1e3-feeb-4795-9e3b-9e6facb2f9ff"
        const val AWS_IOT_CORE_END_POINT: String = "a3jcdl3hkiliu4-ats.iot.us-east-1.amazonaws.com"
        const val CONTENT_TYPE_JSON = "application/json; charset=utf-8"
    }

    private var appContext: Context? = null
    private var userPool: CognitoUserPool? = null
//    var mqttManager: AWSIotMqttManager? = null
    // userAttributes is used for adding attributes to the user
    private var userAttributes: CognitoUserAttributes? = null
    private var credentialsProvider: CognitoCachingCredentialsProvider? = null

    private var userPassword: String? = null// Used for Login
    private var userID: String? = null// Used for Login
    private var userMFAcode: String? = null // Used for Login with MFA

    var currentUser = MutableLiveData<CognitoUser>()
    var mIdentityPoolId = MutableLiveData<String>()
    var mJwtToken = MutableLiveData<String>()
    val mMqttStatus = MutableLiveData<Int>()
    var mUserID = MutableLiveData<String>()


    init {
        appContext = context
        userPool = CognitoUserPool(context, USER_POOL_ID, USER_POOL_CLIENT_ID, USER_POOL_CLIENT_SECRET, Regions.US_EAST_1)
        userAttributes = CognitoUserAttributes()

//        credentialsproviderTest = CognitoCachingCredentialsProvider(context, "us-east-2:c5c0ecb7-09ef-41eb-8843-e6fa4baa387e",Regions.US_EAST_2 )

        credentialsProvider = CognitoCachingCredentialsProvider(context, IDENTITY_POOL_ID, Regions.US_EAST_1)

    }

    override fun onCleared() {
        closeCognitoCache()
        super.onCleared()
    }

    fun closeCognitoCache() {
        credentialsProvider?.clear()
        credentialsProvider?.clearCredentials()
    }

    fun initMQTTbyAWSIotCore() = viewModelScope.launch(Dispatchers.IO) {
//        viewModelScope.launch {
//            mMqttStatus.value = MqttStatus.UNCONNECT
//        }
        getAccessToken{
            //Now mqtt can connect by CognitoCachingCredentialsProvider
            mqttConnect()
        }

//        mqttManager = AWSIotMqttManager(UUID.randomUUID().toString(), AWS_IOT_CORE_END_POINT)
    }

    fun signUpInBackground(userId: String?, password: String?) {
        userPool?.signUpInBackground(userId, password, userAttributes, null, signUpCallback)
        //userPool.signUp(userId, password, this.userAttributes, null, signUpCallback);
    }

    private var signUpCallback: SignUpHandler = object : SignUpHandler {
        override fun onSuccess(
            cognitoUser: CognitoUser,
            userConfirmed: Boolean,
            cognitoUserCodeDeliveryDetails: CognitoUserCodeDeliveryDetails
        ) {
            // Sign-up was successful
            Log.d(ContentValues.TAG, "Sign-up success")
            Toast.makeText(appContext, "Sign-up success", Toast.LENGTH_LONG).show()
            // Check if this user (cognitoUser) needs to be confirmed
            if (!userConfirmed) {
                // This user must be confirmed and a confirmation code was sent to the user
                // cognitoUserCodeDeliveryDetails will indicate where the confirmation code was sent
                // Get the confirmation code from user
            } else {
                Toast.makeText(appContext, "Error: User Confirmed before", Toast.LENGTH_LONG).show()
                // The user has already been confirmed
            }
        }

        override fun onFailure(exception: Exception) {
            Toast.makeText(appContext, "Sign-up failed", Toast.LENGTH_LONG).show()
            Log.d(ContentValues.TAG, "Sign-up failed: $exception")
        }
    }

    fun forgotPasswordInBackground(userId: String?, newPassword: String?, handler: IdentityHandler){
        val cognitoUser = userPool?.getUser(userId)

        cognitoUser?.forgotPasswordInBackground(object: ForgotPasswordHandler {
            override fun onSuccess() {
                viewModelScope.launch {
                    handler(IdentityRequest.SUCCESS,null) {}
                }
            }

            override fun getResetCode(continuation: ForgotPasswordContinuation?) {
                val mContinuation = checkNotNull(continuation) { "Invalid continuation handler" }
                viewModelScope.launch {
                    handler(IdentityRequest.NEED_NEWPASSWORD, null) { r -> run {
                        val response = checkNotNull(r) { "Invalid identity response" }
                        val verifyCode = response["Code"] ?: ""
                        userPassword = response["Code"] ?: ""

                        mContinuation.setPassword(newPassword)
                        mContinuation.setVerificationCode(verifyCode)
                        mContinuation.continueTask()
                    }}
                }
            }

            override fun onFailure(exception: Exception?) {
                handleFailure(handler, exception?.toString())
                handler(IdentityRequest.FAILURE, mapOf("exception" to exception)){}
            }
        })
    }

    fun confirmUser(userId: String?, code: String?) {
        val cognitoUser = userPool?.getUser(userId)
        cognitoUser?.confirmSignUpInBackground(code, false, confirmationCallback)
    }

    // Callback handler for confirmSignUp API
    var confirmationCallback: GenericHandler = object : GenericHandler {
        override fun onSuccess() {
            // User was successfully confirmed
            Toast.makeText(appContext, "User Confirmed", Toast.LENGTH_LONG).show()
        }

        override fun onFailure(exception: Exception) {
            // User confirmation failed. Check exception for the cause.
        }
    }
    fun addAttribute(key: String?, value: String?) {
        userAttributes?.addAttribute(key, value)
    }

    /**
     * Before any action to do mqtt, need to check if has been global sign out by getDetail()
     */
    fun getUserDetails(onSuccess: (userId: String) -> Job, onFailure: () -> Job) = viewModelScope.launch(Dispatchers.IO) {
        currentUser.value?.getDetails(object : GetDetailsHandler{
            override fun onSuccess(cognitoUserDetails: CognitoUserDetails?) {
                Log.d("TAG",cognitoUserDetails.toString())
                val userAtts: MutableMap<String, String>? = cognitoUserDetails?.attributes?.attributes
                val userName = userAtts?.get("name").toString()
                viewModelScope.launch(Dispatchers.Main) {
                    mUserID.value = userName
                }
                onSuccess(userName)
            }

            override fun onFailure(exception: Exception?) {
                //been global sign out
                Log.d("TAG",exception.toString())
                currentUser.value?.signOut()
                onFailure.invoke()
            }
        })
    }

    fun initLogin(userId: String?, handler: IdentityHandler){
        try{
            userID = userId
            viewModelScope.launch(Dispatchers.Main) {
                currentUser.value = userPool?.getUser(userId)
            }
            currentUser.value?.getSessionInBackground(object : AuthenticationHandler{
                override fun onSuccess(
                    userSession: CognitoUserSession?,
                    newDevice: CognitoDevice?
                ) {
                    viewModelScope.launch {
                        handler(IdentityRequest.SUCCESS,null,{})
                    }
                }

                override fun getAuthenticationDetails(
                    authenticationContinuation: AuthenticationContinuation?,
                    userId: String?
                ) {
                    val continuation = checkNotNull(authenticationContinuation) { "Invalid continuation handler" }
                    viewModelScope.launch {
                        handler(IdentityRequest.NEED_CREDENTIALS, null) { r -> run {
                            val response = checkNotNull(r) { "Invalid identity response" }
//                            val username = toUsername((response["username"] ?: ""))
                            val password = response["password"] ?: ""
                            userPassword = response["password"] ?: ""
//                            check(username.isNotEmpty()) { "username is empty" }
//                            check(password.isNotEmpty()) { "password is empty" }

                            continuation.setAuthenticationDetails(AuthenticationDetails(userId, password, null))
                            continuation.continueTask()
                        }}
                    }
                }

                override fun getMFACode(continuation: MultiFactorAuthenticationContinuation?) {
                    val continuation = checkNotNull(continuation) { "Invalid MFA continuation" }
                    viewModelScope.launch {
                        handler(IdentityRequest.NEED_MULTIFACTORCODE, null ){
                            val response = checkNotNull(it) { "Invalid MFA response" }
                            continuation.setMfaCode(response["mfaCode"] ?: "")
                            continuation.continueTask()
                        }
                    }

                }

                override fun authenticationChallenge(continuation: ChallengeContinuation?) {}

                override fun onFailure(exception: Exception?) {
                    handleFailure(handler, exception?.toString())
                    handler(IdentityRequest.FAILURE, mapOf("exception" to exception)){}
                }

            })

        }catch (exception: Exception){
            handleFailure(handler, exception.toString())
        }
    }

    fun autoLogin(userId: String?, handler: IdentityHandler){
        try{
            userID = userId
            viewModelScope.launch(Dispatchers.Main) {
                currentUser.value = userPool?.getUser(userId)
            }
            userPool?.getUser(userId)?.getSession(object : AuthenticationHandler{
                override fun onSuccess(
                    userSession: CognitoUserSession?,
                    newDevice: CognitoDevice?
                ) {
                    viewModelScope.launch {
                        handler(IdentityRequest.SUCCESS,null) {}
                    }
                }

                override fun getAuthenticationDetails(authenticationContinuation: AuthenticationContinuation?, userId: String?) {
                    handler(IdentityRequest.FAILURE, null){}
                    Log.d("TAG","need pw")
                }

                override fun getMFACode(continuation: MultiFactorAuthenticationContinuation?) {
                    handler(IdentityRequest.FAILURE, null){}
                    Log.d("TAG","need MFACode")
                }

                override fun authenticationChallenge(continuation: ChallengeContinuation?) {
                    handler(IdentityRequest.FAILURE, null){}
                    Log.d("TAG","need authenticationChallenge")
                }

                override fun onFailure(exception: Exception?) {
                    handleFailure(handler, exception?.toString())
                    handler(IdentityRequest.FAILURE, mapOf("exception" to exception)){}
                }

            })


        }catch (exception: Exception){
            handleFailure(handler, exception.toString())
        }
    }

    fun LogOut(handler: LogOutHandler){
//        currentUser.value?.signOut()
//        handler(LogOutRequest.SUCCESS)

        currentUser.value?.globalSignOut(object : GenericHandler {
            override fun onSuccess() {
                // User was successfully confirmed
                handler(LogOutRequest.SUCCESS)
            }

            override fun onFailure(exception: Exception) {
                // User confirmation failed. Check exception for the cause.
                handler(LogOutRequest.FAILURE)
                Log.d("TAG",exception.toString())
            }
        })
    }

    private fun mqttConnect(){
        mqttManager?.connect(credentialsProvider, mAWSIotMqttClientStatusCallback)
    }

    private val mAWSIotMqttClientStatusCallback =
        AWSIotMqttClientStatusCallback { status, throwable ->
            when(status){
                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connecting -> {
                    Log.d("TAG","Mqtt Connecting")
                    viewModelScope.launch {
                        mMqttStatus.value = MqttStatus.CONNECTTING
                    }
                }
                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected -> {
                    Log.d("TAG","Mqtt Connected")
                    viewModelScope.launch {
                        mMqttStatus.value = MqttStatus.CONNECTED
                    }
                }
                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.ConnectionLost -> {
                    Log.d("TAG","Mqtt ConnectionLost")
                    viewModelScope.launch {
                        mMqttStatus.value = MqttStatus.CONNECTION_LOST
                    }
                }
                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Reconnecting -> {
                    Log.d("TAG","Mqtt Reconnecting")
                    viewModelScope.launch {
                        mMqttStatus.value = MqttStatus.RECONNECTING
                    }
                }
                else -> {
                    Log.d("TAG",throwable.toString())
                    viewModelScope.launch {
                        mMqttStatus.value = MqttStatus.UNCONNECT
                    }
                }
            }

            if(throwable!=null){
                Log.d("TAG",throwable.toString())
            }
        }

    fun mqttDisconnect(){
        try {
            mqttManager?.disconnect()
            Log.d("TAG", "mqttDisconnect success.")
        }catch (e: Exception){
            Log.e("TAG", "mqttDisconnect error.", e)
        }
    }
    
    fun getUserInfo(function: () -> Unit) {
        currentUser.value?.getSessionInBackground(object :AuthenticationHandler{
            override fun onSuccess(userSession: CognitoUserSession?, newDevice: CognitoDevice?) {
                viewModelScope.launch(Dispatchers.Main) {
                    mJwtToken.value = userSession?.idToken?.jwtToken
                    mUserID.value = userSession?.username
                    function.invoke()
                }
            }

            override fun getAuthenticationDetails(authenticationContinuation: AuthenticationContinuation?, userId: String?) {}

            override fun getMFACode(continuation: MultiFactorAuthenticationContinuation?) {}

            override fun authenticationChallenge(continuation: ChallengeContinuation?) {}

            override fun onFailure(exception: Exception?) {}

        })
    }

    fun getAccessToken(function: (mCredentials: AWSSessionCredentials?) -> Unit) {
        currentUser.value?.getSessionInBackground(object :AuthenticationHandler{
            override fun onSuccess(userSession: CognitoUserSession?, newDevice: CognitoDevice?) {

                //todo: logins name should change to right Region !!
                viewModelScope.launch(Dispatchers.IO){
                    val logins: MutableMap<String, String> = HashMap()

                    logins.put(USER_POOL_ADDRESS, userSession?.idToken?.jwtToken.toString())
                    Log.d("TAG","jwtToken: "+userSession?.idToken?.jwtToken.toString())
                    try{
                        credentialsProvider?.logins = logins
                        val mIdentityId = credentialsProvider?.identityId?:"" //use it to get aws service
                        viewModelScope.launch(Dispatchers.Main){
                            mIdentityPoolId.value = mIdentityId
                        }
                        val mCredentials = credentialsProvider?.credentials
                        Log.d("TAG", "mIdentityId: $mIdentityId")
                        function(mCredentials)
                    }catch (e: Exception){
                        Log.e("TAG",e.toString())
                    }
                }
            }

            override fun getAuthenticationDetails(authenticationContinuation: AuthenticationContinuation?, userId: String?) {}

            override fun getMFACode(continuation: MultiFactorAuthenticationContinuation?) {}

            override fun authenticationChallenge(continuation: ChallengeContinuation?) {}

            override fun onFailure(exception: Exception?) {}

        })
    }

    fun setAttachPolicy(){
        getIdentityId { jwtToken ->
            val client = OkHttpClient()
            val postBody = "{\"clientToken\":\"AAA\"}"
            val MEDIA_TYPE_JSON = CONTENT_TYPE_JSON.toMediaType()

            val request = Request.Builder()
                .url("https://api.ikey-lock.com/v1/acl/attach-policy")
                .header("Authorization", "Bearer $jwtToken")
                .post(postBody.toRequestBody(MEDIA_TYPE_JSON))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("TAG", e.toString())
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("TAG", "response: ${response.body?.string()}")
                }
            })
        }
    }

    private fun getIdentityId(function: (token: String) -> Unit) {
        currentUser.value?.getSessionInBackground(object :AuthenticationHandler{
            override fun onSuccess(userSession: CognitoUserSession?, newDevice: CognitoDevice?) {
                viewModelScope.launch(Dispatchers.IO){
                    try{
                        function(userSession?.idToken?.jwtToken.toString())
                    }catch (e: Exception){
                        Log.e("TAG",e.toString())
                    }
                }
            }

            override fun getAuthenticationDetails(authenticationContinuation: AuthenticationContinuation?, userId: String?) {}

            override fun getMFACode(continuation: MultiFactorAuthenticationContinuation?) {}

            override fun authenticationChallenge(continuation: ChallengeContinuation?) {}

            override fun onFailure(exception: Exception?) {}

        })
    }


    fun handleFailure(
            handler: (IdentityRequest, Map<String, Exception>?, IdentityResponse: (Map<String, String>?) -> Unit) -> Unit,
            message: String?
        ) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("TAG",message.toString())
        }
    }
}

enum class IdentityRequest {
    NEED_SIGNUP,
    NEED_CREDENTIALS,
    NEED_NEWPASSWORD,
    NEED_MULTIFACTORCODE,
    SUCCESS,
    FAILURE
}

enum class LogOutRequest {
    SUCCESS,
    FAILURE
}
//typealias IdentityResponse = (Map<String, String>?) -> Unit
typealias IdentityHandler = (IdentityRequest, Map<String,Exception?>?, IdentityResponse: (Map<String, String>?) -> Unit) -> Unit
typealias LogOutHandler = (LogOutRequest) -> Unit
