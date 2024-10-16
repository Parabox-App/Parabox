plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlinx.atomicfu")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("dev.rikka.tools.refine")
}

android {
    namespace = "com.ojhdtapp.parabox"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ojhdtapp.parabox"
        minSdk = 26
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 15
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        javaCompileOptions {
            annotationProcessorOptions {
                arguments.put("room.schemaLocation", "$projectDir/schemas".toString())
                arguments.put("room.incremental", "true")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        jniLibs {
            excludes.add("META-INF/**")
        }
        resources {
            excludes.add("META-INF/**")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-b8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    lint {
        abortOnError = true
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(files("libs/onebot-kotlin-sdk.jar", "libs/imageViewer.aar"))

    // android core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-google-shortcuts:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.appcompat:appcompat-resources:1.7.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // ViewModel
    val lifecycleVersion = "2.6.2"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    // ViewModel utilities for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    // optional - helpers for implementing LifecycleOwner in a Service
    implementation("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
    // optional - ProcessLifecycleOwner provides a lifecycle for the whole application process
    implementation("androidx.lifecycle:lifecycle-process:$lifecycleVersion")

    // kotlinx
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.7.3")
    implementation("org.jetbrains.kotlinx:atomicfu:0.25.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // compose bom
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite")
    implementation("androidx.compose.material3:material3-window-size-class-android")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // google material
    implementation("com.google.android.material:material:1.12.0")

    // Accompanist
    val accVersion = "0.32.0"
    implementation("com.google.accompanist:accompanist-permissions:$accVersion")
    implementation("com.google.accompanist:accompanist-placeholder-material3:$accVersion")

    // Adaptive
    val adaptiveVersion = "1.0.0-alpha12"
    implementation("androidx.compose.material3.adaptive:adaptive:$adaptiveVersion")
    implementation("androidx.compose.material3.adaptive:adaptive-layout-android:$adaptiveVersion")
    implementation("androidx.compose.material3.adaptive:adaptive-navigation-android:$adaptiveVersion")

    // Navigation Compose
    val navVersion = "2.8.2"
    implementation("androidx.navigation:navigation-compose:$navVersion")

    // Lottie
    val lottieVersion = "5.0.3"
    implementation("com.airbnb.android:lottie-compose:$lottieVersion")

    // Decompose
    val decomposeVersion = "3.0.0"
    implementation("com.arkivanov.decompose:decompose:$decomposeVersion")
    implementation("com.arkivanov.decompose:extensions-compose:$decomposeVersion")

    // LazyColumnScrollbar
    implementation("com.github.nanihadesuka:LazyColumnScrollbar:1.9.0")

    // Constraint Layout
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    // DropdownMenu
    implementation("me.saket.cascade:cascade-compose:2.3.0")
    implementation("me.saket.telephoto:zoomable-image-coil:0.6.2")

    // Coil
    val coilVersion = "2.6.0"
    implementation("io.coil-kt.coil3:coil:3.0.0-rc01")
    implementation("io.coil-kt:coil-gif:$coilVersion")
    implementation("io.coil-kt:coil-svg:$coilVersion")
    implementation("io.coil-kt:coil-compose:$coilVersion")

    // FontAwesome Icon
    implementation("com.github.Gurupreet:FontAwesomeCompose:1.1.0")

    // Markdown
    val richtextVersion = "1.0.0-alpha01"
    implementation("com.halilibo.compose-richtext:richtext-ui:$richtextVersion")
    implementation("com.halilibo.compose-richtext:richtext-commonmark:$richtextVersion")

    // Swipe
    implementation("me.saket.swipe:swipe:1.3.0")

    // Paging
    val pagingVersion = "3.3.2"
    implementation("androidx.paging:paging-runtime-ktx:$pagingVersion")
    implementation("androidx.paging:paging-compose:$pagingVersion")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Window
    implementation("androidx.window:window:1.3.0")

    // hilt
    val hiltVersion = "2.51.1"
    implementation("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")
    val androidxHiltVersion = "1.2.0"
    implementation("androidx.hilt:hilt-navigation-compose:$androidxHiltVersion")
    implementation("androidx.hilt:hilt-work:$androidxHiltVersion")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    testImplementation("androidx.room:room-testing:$roomVersion")

    // Room DB backup
    implementation("de.raphaelebner:roomdatabasebackup:1.0.0-beta14")

    // log
    runtimeOnly("com.celeral:log4j2-android:1.0.0")

    // Jackson
    val jacksonVersion = "2.17.1"
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    // Ktor
    val ktorVersion = "2.3.12"
    compileOnly("io.ktor:ktor-client-core:$ktorVersion")
    compileOnly("io.ktor:ktor-client-okhttp:$ktorVersion")

    // Pine
    implementation("top.canyie.pine:core:0.2.9")

    // Pinyin
    implementation("com.belerweb:pinyin4j:2.5.1")

    // Play Core
    implementation("com.google.android.play:review:2.0.1")
    implementation("com.google.android.play:review-ktx:2.0.1")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // ML-Kit
    implementation("com.google.mlkit:entity-extraction:16.0.0-beta5")
    implementation("com.google.mlkit:smart-reply:17.0.4")
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.mlkit:language-id:17.0.6")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

    // Map & Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose:2.11.4")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    // Google Drive Api
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.api-client:google-api-client-android:1.26.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev136-1.25.0")

    // Onedrive
    implementation("com.microsoft.identity.client:msal:4.2.0")

    // Qiniu
    implementation("com.qiniu:qiniu-java-sdk:7.12.1")

    // Tencent COS
    implementation("com.qcloud.cos:cos-android-lite-nobeacon:5.9.8")


    // Extended Gestures
    implementation("com.github.SmartToolFactory:Compose-Extended-Gestures:3.0.0")

    // Amplituda
    implementation("com.github.lincollincol:amplituda:2.2.1")

    // Apache Commons
    implementation("commons-io:commons-io:2.15.1")

    // Chrome Custom Tab
    implementation("androidx.browser:browser:1.8.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Development Kit
    implementation(project(":Parabox Development Kit"))
    implementation("com.ojhdt:parabox-development-kit:1.0.6")

    // Refine
    compileOnly(project(":app:hidden-api"))
    implementation("dev.rikka.tools.refine:runtime:4.4.0")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
    ignoreList.add("keyToIgnore")
    ignoreList.add("sdk.*")
}