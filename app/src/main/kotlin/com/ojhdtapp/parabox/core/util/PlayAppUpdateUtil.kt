package com.ojhdtapp.parabox.core.util

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.installStatus

class PlayAppUpdateUtil(
    context: Context,
    val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
    val installStateUpdatedListener:InstallStateUpdatedListener
) {
    val appUpdateManager by lazy {
        AppUpdateManagerFactory.create(context)
    }

    fun checkAndUpdate(
        onCheckResult: (hasUpdate: Boolean) -> Unit,
    ) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnCompleteListener {
            if (it.isSuccessful) {
                val appUpdateInfo = it.result
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                ) {
                    onCheckResult(true)
                    if (appUpdateInfo.updatePriority() > 4 && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activityResultLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                                .setAllowAssetPackDeletion(true)
                                .build())
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)){
                        appUpdateManager.registerListener(installStateUpdatedListener)
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activityResultLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE)
                                .setAllowAssetPackDeletion(true)
                                .build())
                    }
                } else {
                    onCheckResult(false)
                }
            } else {
                it.exception?.printStackTrace()
                onCheckResult(false)
            }
        }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    fun checkProcessingUpdate() {
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                // If the update is downloaded but not installed,
                // notify the user to complete the update.
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {

                }
            }
    }
}

val LocalPlayAppUpdateUtil = staticCompositionLocalOf<PlayAppUpdateUtil> {
    error("no PlayAppUpdateUtil provided")
}