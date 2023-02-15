package com.ojhdtapp.parabox.core.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.data.remote.dto.onedrive.MsalApi
import javax.inject.Inject

class OnedriveUtil @Inject constructor(
    val context: Context,
    val msalApi: MsalApi
) {
    companion object {
        const val APP_ROOT_DIR = "approot"
        const val TOKEN_KEY = "Authorization"
        const val BASE_URL = "https://graph.microsoft.com/v1.0/"

        const val STATUS_SUCCESS = 1
        const val STATUS_ERROR = 2
        const val STATUS_CANCEL = 3
    }
    private var mSingleAccountApp : ISingleAccountPublicClientApplication? = null
    private var authInfo: IAuthenticationResult? = null
    private val scopes = listOf<String>("User.Read", "Files.ReadWrite.All")
    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.auth_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    loadAccounts()
                }

                override fun onError(exception: MsalException) {
                    exception.printStackTrace()
                }
            })
    }

    /**
     * Load currently signed-in accounts, if there's any.
     **/
    fun loadAccounts() {
        mSingleAccountApp?.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                if (activeAccount != null) {
                    getTokenByAccountInfo(activeAccount)
                }
                Log.d("MSAL", "Account loaded: " + activeAccount?.username)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                // Handle the change
                if (currentAccount != null) {
                    getTokenByAccountInfo(currentAccount)
                }
            }

            override fun onError(exception: MsalException) {
                exception.printStackTrace()
            }
        })
    }


    fun signIn(
        activity: Activity,
        onResult: (code: Int) -> Unit
    ){
        val callback = object: AuthenticationCallback{
            override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                authInfo = authenticationResult
                onResult(STATUS_SUCCESS)
            }

            override fun onError(exception: MsalException?) {
                exception?.printStackTrace()
                onResult(STATUS_ERROR)
            }

            override fun onCancel() {
                onResult(STATUS_CANCEL)
            }

        }
        mSingleAccountApp?.signIn(SignInParameters.builder()
            .withActivity(activity)
            .withScopes(scopes)
            .withCallback(callback)
            .build())
    }

    fun signOut(){
        mSingleAccountApp?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                authInfo = null
            }
            override fun onError(exception: MsalException) {
                // Exception occurred during sign-out
            }
        })
    }

    fun acquireToken(scopes: List<String>){
        val callback = object: AuthenticationCallback{
            override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                authenticationResult?.accessToken
            }

            override fun onError(exception: MsalException?) {
            }

            override fun onCancel() {
            }
        }
        mSingleAccountApp?.acquireTokenSilent(
            AcquireTokenSilentParameters.Builder()
                .withScopes(scopes)
                .withCallback(callback)
                .build()
        )
    }
    private fun getTokenByAccountInfo(account: IAccount) {
        mSingleAccountApp?.acquireTokenSilentAsync(
            AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .withScopes(scopes)
                .withCallback(object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                        authInfo = authenticationResult
                    }
                    override fun onError(exception: MsalException) {
                        exception.printStackTrace()
                        signOut()
                    }
                })
                .build()
        )
    }

    
}