# Keep TDLib JNI bridge classes and fields intact for libtdjni.so native lookups
-keep class org.drinkless.tdlib.** { *; }
-keepclassmembers class org.drinkless.tdlib.** { *; }

# Keep Parabox Extension Service entry point
-keep class com.ojhdt.parabox.extension.tdlib.TdExtensionService { *; }
