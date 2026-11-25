package com.ojhdtapp.paraboxdevelopmentkit.v2

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import com.ojhdtapp.parabox.protocol.v2.ParaboxExtensionServiceGrpc
import io.grpc.Server
import io.grpc.Status
import io.grpc.binder.AndroidComponentAddress
import io.grpc.binder.BinderServerBuilder
import io.grpc.binder.IBinderReceiver
import io.grpc.binder.LifecycleOnDestroyHelper
import io.grpc.binder.SecurityPolicies
import io.grpc.binder.SecurityPolicy
import io.grpc.binder.ServerSecurityPolicy
import kotlinx.coroutines.runBlocking

abstract class ParaboxExtensionService : LifecycleService() {

    private val binderReceiver = IBinderReceiver()
    private var server: Server? = null
    private lateinit var engine: ParaboxExtensionGrpcEngine

    abstract fun provideExtensions(): List<ParaboxExtensionProvider>

    /**
     * Subclasses may return trusted Parabox host package -> SHA-256 signature bytes
     * to restrict which host apps are allowed to bind to this extension service in release builds.
     */
    open fun trustedHostSignatures(): Map<String, ByteArray> = emptyMap()

    /**
     * Override to customize gRPC Binder caller verification policy.
     * In debuggable builds, allows any host for convenient local development.
     */
    open fun buildServerSecurityPolicy(): ServerSecurityPolicy {
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val trusted = trustedHostSignatures()
        val policy: SecurityPolicy = when {
            isDebuggable || trusted.isEmpty() -> {
                if (!isDebuggable && trusted.isEmpty()) {
                    Log.w(TAG, "No trustedHostSignatures configured; allowing public Binder clients.")
                }
                object : SecurityPolicy() {
                    override fun checkAuthorization(uid: Int): Status = Status.OK
                }
            }
            else -> {
                val policies = trusted.map { (pkg, sha256) ->
                    SecurityPolicies.hasSignatureSha256Hash(packageManager, pkg, sha256)
                }
                SecurityPolicies.anyOf(*policies.toTypedArray())
            }
        }
        return ServerSecurityPolicy.newBuilder()
            .servicePolicy(ParaboxExtensionServiceGrpc.SERVICE_NAME, policy)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        val (versionName, versionCode) = resolvePackageVersion()
        engine = ParaboxExtensionGrpcEngine(
            providers = provideExtensions(),
            versionName = versionName,
            versionCode = versionCode,
        )
        val builtServer = BinderServerBuilder
            .forAddress(AndroidComponentAddress.forContext(this), binderReceiver)
            .addService(engine)
            .securityPolicy(buildServerSecurityPolicy())
            .build()
        LifecycleOnDestroyHelper.shutdownUponDestruction(lifecycle, builtServer)
        builtServer.start()
        server = builtServer
        Log.i(TAG, "ParaboxExtensionService started (${javaClass.name}, v$versionName/$versionCode)")
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return requireNotNull(binderReceiver.get()) {
            "gRPC BinderServer IBinder was not initialized before onBind"
        }
    }

    override fun onDestroy() {
        runBlocking {
            if (::engine.isInitialized) {
                engine.shutdownAllSessions()
            }
        }
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun resolvePackageVersion(): Pair<String, Long> {
        return try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageInfo(packageName, 0)
            }
            val versionName = pkgInfo.versionName ?: "1.0.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                pkgInfo.versionCode.toLong()
            }
            versionName to versionCode
        } catch (e: Exception) {
            "1.0.0" to 1L
        }
    }

    companion object {
        private const val TAG = "ParaboxExtService"
    }
}
