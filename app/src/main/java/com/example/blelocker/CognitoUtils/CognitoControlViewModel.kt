package com.example.blelocker.CognitoUtils

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazonaws.mobileconnectors.cognitoidentityprovider.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.regions.Regions
import kotlinx.coroutines.launch
import java.lang.Exception

import com.amazonaws.auth.CognitoCachingCredentialsProvider
import kotlinx.coroutines.Dispatchers
import java.util.*
import kotlin.collections.HashMap
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.mobileconnectors.iot.*
import java.io.UnsupportedEncodingException
import com.amazonaws.auth.AWSSessionCredentials
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler


class CognitoControlViewModel(val context: Context): ViewModel() {
    // ############################################################# Information about Cognito Pool Test
    private val poolIDTest = "us-east-2_48Mq3KjSR"
    private val clientIDTest = "78nqui84c8qpl2vspofv9bd34l"
    private val clientSecretTest = "15aigi3rdp7g9lr4pjibvrj8u6fnc03o6edif4iukqtqslt59jrh"
    private val awsRegionTest: Regions = Regions.US_EAST_2
    // 來自應用程式用戶端的設定
    // ############################################################# End of Information about Cognito Pool Test
    // ############################################################# Information about Cognito Pool
    private val poolID = "us-east-1_9H0qG2JDz"
    private val clientID = "ai4604ihkdbac53lpqs1kchlp"
    private val clientSecret = "13ems49degn68tfeq5o9q7aa4rbaku52oot35grahvpsdn6msrt0"
    private val awsRegion: Regions = Regions.US_EAST_1
    // ############################################################# End of Information about Cognito Pool
    private var userPool: CognitoUserPool? = null
    private var userAttributes // Used for adding attributes to the user
            : CognitoUserAttributes? = null
    private var credentialsProvider: CognitoCachingCredentialsProvider? = null
    private var credentialsProvider_test: CognitoCachingCredentialsProvider? = null

    // ############################################################# iot core
    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private val awsIotCoreEndPoint: String = "a3jcdl3hkiliu4-ats.iot.us-east-1.amazonaws.com"
    var mqttManager: AWSIotMqttManager? = null
    var awsCredentials: AWSCredentials? = null

    private var appContext: Context? = null
    private var userPassword: String? = null// Used for Login
    private var userID: String? = null// Used for Login
    private var userMFAcode: String? = null // Used for Login with MFA
    private var currentUser = MutableLiveData<CognitoUser>()
    var mJwtToken = MutableLiveData<String>()
    var mUserID = MutableLiveData<String>()

    val mLoginStatus = MutableLiveData<Int>()

    init {
        appContext = context
        userPool = CognitoUserPool(context, poolID, clientID, clientSecret, awsRegion)
        userAttributes = CognitoUserAttributes()

        // Initialize the Amazon Cognito credentials provider , pool id from mason

        credentialsProvider_test = CognitoCachingCredentialsProvider(
            context,
            "us-east-2:c5c0ecb7-09ef-41eb-8843-e6fa4baa387e",  // Identity pool ID
            Regions.US_EAST_2 // Region
        )

        credentialsProvider = CognitoCachingCredentialsProvider(
            context,
            "us-east-1:8266c1e3-feeb-4795-9e3b-9e6facb2f9ff",  // Identity pool ID
            Regions.US_EAST_1 // Region
        )
    }

    fun initialAWSIotClient() = viewModelScope.launch(Dispatchers.IO) {

        getAccessToken{
            //now mqtt can connect by CognitoCachingCredentialsProvider
            mqttConnect()
        }

        mqttManager = AWSIotMqttManager(UUID.randomUUID().toString(), awsIotCoreEndPoint)
    }

    fun signUpInBackground(userId: String?, password: String?) {
        userPool?.signUpInBackground(userId, password, userAttributes, null, signUpCallback)
        //userPool.signUp(userId, password, this.userAttributes, null, signUpCallback);
    }

    var signUpCallback: SignUpHandler = object : SignUpHandler {
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

    fun initLogin(userId: String?, handler: IdentityHandler){
        try{
            userID = userId
            currentUser.value = userPool?.getUser(userId)
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
            handleFailure(handler, "Validation error")
        }
    }
    fun LogOut(handler: LogOutHandler){
        currentUser.value?.signOut()
        handler(LogOutRequest.SUCCESS)
    /*object : GenericHandler {
            override fun onSuccess() {
                // User was successfully confirmed
                handler(LogOutRequest.SUCCESS)
            }

            override fun onFailure(exception: Exception) {
                // User confirmation failed. Check exception for the cause.
                handler(LogOutRequest.FAILURE)
                Log.d("TAG",exception.toString())
            }
        })*/
    }

    fun mqttConnect(){
        mqttManager?.connect(credentialsProvider, mAWSIotMqttClientStatusCallback)
    }

    private val mAWSIotMqttClientStatusCallback =
        AWSIotMqttClientStatusCallback { status, throwable ->
            when(status){
                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connecting -> {
                    Log.d("TAG","Mqtt Connecting")
                }
                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected -> {
                    Log.d("TAG","Mqtt Connected")
                    mqttSubscribe()
                }
                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.ConnectionLost -> {
                    Log.d("TAG","Mqtt ConnectionLost")
                }
                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Reconnecting -> {
                    Log.d("TAG","Mqtt Reconnecting")
                }
                else -> {
                    Log.d("TAG",throwable.toString())
                }
            }

            if(throwable!=null){
                Log.d("TAG",throwable.toString())
            }
        }

    fun mqttSubscribe(){
        try {
            mqttManager?.subscribeToTopic("\$aws/things/242779ea-6972-4e2d-b7b5-dbe490e29def/shadow/get/accepted", AWSIotMqttQos.QOS0, mAWSIotMqttNewMessageCallback)
        }catch (e: Exception){
            Log.e("TAG", "mqttSubscribe error.", e)
        }
    }

    private val mAWSIotMqttNewMessageCallback = 
        AWSIotMqttNewMessageCallback { topic: String?, data: ByteArray? ->
            try {
                val message = String(data?:return@AWSIotMqttNewMessageCallback, Charsets.UTF_8)
                Log.d("TAG", "Message arrived:")
                Log.d("TAG", "   Topic       : $topic")
                Log.d("TAG", "   Message     : $message")
//                tvLastMessage.setText(message)
            } catch (e: UnsupportedEncodingException) {
                Log.e("TAG", "Message encoding error.", e)
            }
        }

    fun mqttPublish(){
        try {
            mqttManager?.publishString("{}", "\$aws/things/242779ea-6972-4e2d-b7b5-dbe490e29def/shadow/get", AWSIotMqttQos.QOS0)
        }catch (e: Exception){
            Log.e("TAG", "mqttPublish error.", e)
        }
    }

    fun mqttDisconnect(){
        try {
            mqttManager?.disconnect()
            Log.e("TAG", "mqttDisconnect success.")
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

                    logins.put("cognito-idp.us-east-1.amazonaws.com/us-east-1_9H0qG2JDz", userSession?.idToken?.jwtToken.toString())
                    try{
                        credentialsProvider?.logins = logins
                        val mIdentityId = credentialsProvider?.identityId //use it to get aws service
                        val mCredentials = credentialsProvider?.credentials
                        Log.d("TAG",mIdentityId.toString())
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
