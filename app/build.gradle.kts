plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties

android {
    namespace = "com.jusdots.juscalc"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.jusdots.juscalc"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Upload-key signing from untracked ../keystore.properties (back it up
    // with juscalc-upload.jks). Absent on CI → falls back to the debug key
    // so tag builds keep working without secrets.
    val keyProps = Properties()
    val keyFile = rootProject.file("keystore.properties")
    if (keyFile.exists()) keyFile.inputStream().use { keyProps.load(it) }
    signingConfigs {
        create("release") {
            val propsFile = keyProps.getProperty("releaseStoreFile")
            if (propsFile != null) {
                storeFile = file(propsFile)
                storePassword = keyProps.getProperty("releaseStorePassword")
                keyAlias = keyProps.getProperty("releaseKeyAlias")
                keyPassword = keyProps.getProperty("releaseKeyPassword")
            } else {
                initWith(signingConfigs.getByName("debug"))
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.kyant.shapes)
    implementation(libs.evalex)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}