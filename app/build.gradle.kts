plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.cooper.lithic"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.cooper.lithic"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.1.0-m13"
        // T04 is an explicit test-only build; never start the default ledger for self-kill tests.
        testInstrumentationRunner = if (providers.gradleProperty("m13Isolated").orNull == "true")
            "com.cooper.lithic.app.M13IsolatedRunner"
        else if (providers.gradleProperty("m12T04ProcessDeath").orNull == "true")
            "com.cooper.lithic.data.backup.M12ProcessDeathRunner"
        else "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    // Exact exported schema fixtures are test-only assets; never packaged in the app APK.
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

kotlin {
    jvmToolchain(17)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    // Already present transitively at this exact version; used directly by backup format v1.
    implementation(libs.kotlinx.serialization.json)
    // Explicit for the manifest override; retain the already resolved runtime version.
    implementation(libs.androidx.startup.runtime)
    testImplementation(libs.junit)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
    // Only the Debug test-host activity; no test framework enters release runtime.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
