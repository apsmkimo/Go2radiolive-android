plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.apsmkimo.go2radiolive"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.apsmkimo.go2radiolive"
        minSdk = 26
        targetSdk = 35
        // SMCPKG_SUPPORT>>>Cursor010
        // versionCode = 1
        // versionName = "1.0.0"
        // SMCPKG_SUPPORT<<<Cursor010
        // SMCPKG_SUPPORT>>>Cursor038
        // versionCode = 2
        // versionName = "1.1.0"
        // versionCode = 3
        // versionName = "1.1.1"
        // SMCPKG_SUPPORT<<<Cursor038
        // SMCPKG_SUPPORT>>>Cursor052
        // versionCode = 4
        // versionName = "1.2.0"
        // SMCPKG_SUPPORT<<<Cursor052
        // SMCPKG_SUPPORT>>>Cursor077
        // versionCode = 5
        // versionName = "1.2.1"
        // SMCPKG_SUPPORT<<<Cursor077
        // SMCPKG_SUPPORT>>>Cursor081
        // versionCode = 6
        // versionName = "1.3.0"
        // SMCPKG_SUPPORT<<<Cursor081
        // SMCPKG_SUPPORT>>>Cursor087
        // versionCode = 7
        // versionName = "1.4.0"
        // SMCPKG_SUPPORT<<<Cursor087
        // SMCPKG_SUPPORT>>>Cursor094
        versionCode = 8
        versionName = "1.5.0"
        // SMCPKG_SUPPORT<<<Cursor094

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val releaseKeystore = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (!releaseKeystore.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // SMCPKG_SUPPORT>>>Cursor099
            signingConfig = when {
                !releaseKeystore.isNullOrBlank() -> signingConfigs.getByName("release")
                System.getenv("SIGN_RELEASE_WITH_DEBUG") == "1" -> signingConfigs.getByName("debug")
                else -> null
            }
            // SMCPKG_SUPPORT<<<Cursor099
            // SMCPKG_SUPPORT>>>Cursor078
            manifestPlaceholders["admobAppId"] = "ca-app-pub-6138247296137119~5720425997"
            buildConfigField(
                "String",
                "ADMOB_BANNER_ID",
                "\"ca-app-pub-6138247296137119/7955882105\"",
            )
            buildConfigField(
                "String",
                "ADMOB_NATIVE_ID",
                "\"ca-app-pub-6138247296137119/2601192653\"",
            )
            // SMCPKG_SUPPORT<<<Cursor078
            // SMCPKG_SUPPORT>>>Cursor088
            buildConfigField(
                "String",
                "ADMOB_APP_OPEN_ID",
                "\"ca-app-pub-6138247296137119/7661947645\"",
            )
            // SMCPKG_SUPPORT<<<Cursor088
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ""
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField(
                "String",
                "ADMOB_BANNER_ID",
                "\"ca-app-pub-3940256099942544/6300978111\"",
            )
            buildConfigField(
                "String",
                "ADMOB_NATIVE_ID",
                "\"ca-app-pub-3940256099942544/2247696110\"",
            )
            // SMCPKG_SUPPORT>>>Cursor088
            buildConfigField(
                "String",
                "ADMOB_APP_OPEN_ID",
                "\"ca-app-pub-3940256099942544/9257395921\"",
            )
            // SMCPKG_SUPPORT<<<Cursor088
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.media3.common.util.UnstableApi",
            "-opt-in=kotlin.RequiresOptIn",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // SMCPKG_SUPPORT>>>Cursor088
    implementation(libs.androidx.lifecycle.process)
    // SMCPKG_SUPPORT<<<Cursor088
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    implementation(libs.coil.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource)

    implementation(libs.datastore.preferences)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
