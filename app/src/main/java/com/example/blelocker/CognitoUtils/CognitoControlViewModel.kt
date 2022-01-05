package com.example.blelocker.CognitoUtils

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazonaws.mobileconnectors.cognitoidentityprovider.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.regions.Regions
import kotlinx.coroutines.launch
import java.lang.Exception

import com.amazonaws.auth.CognitoCachingCredentialsProvider
import kotlinx.coroutines.Dispatchers


class CognitoControlViewModel(val context: Context): ViewModel() {
    // ############################################################# Information about Cognito Pool
    private val poolID = "us-east-2_48Mq3KjSR"
    private val clientID = "78nqui84c8qpl2vspofv9bd34l"
    private val clientSecret = "15aigi3rdp7g9lr4pjibvrj8u6fnc03o6edif4iukqtqslt59jrh"
    private val awsRegion: Regions = Regions.US_EAST_2
    // ############################################################# End of Information about Cognito Pool
    private var userPool: CognitoUserPool? = null
    private var userAttributes // Used for adding attributes to the user
            : CognitoUserAttributes? = null
    private var appContext: Context? = null
    private var credentialsProvider: CognitoCachingCredentialsProvider? = null

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

        // Initialize the Amazon Cognito credentials provider , from mason

        credentialsProvider = CognitoCachingCredentialsProvider(
            context,
            "us-east-2:c5c0ecb7-09ef-41eb-8843-e6fa4baa387e",  // Identity pool ID
            Regions.US_EAST_2 // Region
        )
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

    fun confirmUser(userId: String?, code: String?) {
        val cognitoUser = userPool?.getUser(userId)
        cognitoUser?.confirmSignUpInBackground(code, false, confirmationCallback)
        //cognitoUser.confirmSignUp(code,false, confirmationCallback);
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

                override fun authenticationChallenge(continuation: ChallengeContinuation?) {
                }

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

    fun getAccessToken(function: () -> Unit) {
        currentUser.value?.getSessionInBackground(object :AuthenticationHandler{
            override fun onSuccess(userSession: CognitoUserSession?, newDevice: CognitoDevice?) {
                viewModelScope.launch(Dispatchers.Main) {
                    mJwtToken.value = userSession?.idToken?.jwtToken
                    mUserID.value = userSession?.username

                    //todo
//                    viewModelScope.launch(Dispatchers.IO){
//                        val logins: MutableMap<String, String> = HashMap()
//                        logins.put("cognito-idp.us-east-2.amazonaws.com/us-east-2_48Mq3KjSR", userSession?.idToken?.jwtToken.toString())
//                        credentialsProvider?.setLogins(logins)
//                        val mIdentityId = credentialsProvider?.identityId //use it to get aws service
//
//                        Log.d("TAG",mIdentityId.toString())
//                    }

                    function.invoke()
                }
            }

            override fun getAuthenticationDetails(
                authenticationContinuation: AuthenticationContinuation?,
                userId: String?
            ) {
            }

            override fun getMFACode(continuation: MultiFactorAuthenticationContinuation?) {
            }

            override fun authenticationChallenge(continuation: ChallengeContinuation?) {
            }

            override fun onFailure(exception: Exception?) {
            }

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
