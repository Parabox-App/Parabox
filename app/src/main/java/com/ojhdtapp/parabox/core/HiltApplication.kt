package com.ojhdtapp.parabox.core

import android.app.Application
import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.ojhdtapp.parabox.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import org.lsposed.hiddenapibypass.HiddenApiBypass
import top.canyie.pine.Pine
import top.canyie.pine.PineConfig
import top.canyie.pine.callback.MethodReplacement
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication : Application(), Configuration.Provider, ImageLoaderFactory {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader(this).newBuilder()
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.1)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .maxSizePercent(0.01)
                    .directory(cacheDir)
                    .build()
            }
            .logger(DebugLogger())
            .build()
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate() {
        pineHook()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("");
        }
        super.onCreate()
    }
}

private fun pineHook() {
    PineConfig.debug = true // Do we need to print more detailed logs?
    PineConfig.debuggable = BuildConfig.DEBUG // Is this process debuggable?

    // Hook the ThreePaneMotion class to customize the motion
    val cls = Class.forName("androidx.compose.material3.adaptive.layout.ThreePaneMotion\$Companion")
    val method1 = cls.getDeclaredMethod("slideInFromLeft", Int::class.java)
    val method2 = cls.getDeclaredMethod("slideOutToRight", Int::class.java)
    val method3 = cls.getDeclaredMethod("slideInFromRight", Int::class.java)
    val method4 = cls.getDeclaredMethod("slideOutToLeft", Int::class.java)
    Pine.hook(method1, object: MethodReplacement() {
        override fun replaceCall(callFrame: Pine.CallFrame?): Any {
            val spacerSize = callFrame!!.args[0] as Int
            return fadeIn(tween()) + slideInHorizontally(tween()) { -200 - spacerSize }
        }
    })
    Pine.hook(method2, object: MethodReplacement() {
        override fun replaceCall(callFrame: Pine.CallFrame?): Any {
            val spacerSize = callFrame!!.args[0] as Int
            return fadeOut(tween()) + slideOutHorizontally(tween()) { spacerSize + 200 }
        }
    })
    Pine.hook(method3, object: MethodReplacement() {
        override fun replaceCall(callFrame: Pine.CallFrame?): Any {
            val spacerSize = callFrame!!.args[0] as Int
            return fadeIn(tween()) + slideInHorizontally(tween()) { spacerSize + 200 }
        }
    })
    Pine.hook(method4, object: MethodReplacement() {
        override fun replaceCall(callFrame: Pine.CallFrame?): Any {
            val spacerSize = callFrame!!.args[0] as Int
            return fadeOut(tween()) + slideOutHorizontally(tween()) { -200 - spacerSize }
        }
    })
}