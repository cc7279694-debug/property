plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

// Explicit desktop-only preparation for a separately installed M22 verification app.
// Leave the ordinary Debug/Release identities and their output files untouched.
val m22VerificationValue = providers.gradleProperty("m22DeviceVerification").orNull
require(m22VerificationValue == null || m22VerificationValue in setOf("true", "false")) {
    "m22DeviceVerification must be true or false"
}
val m22DeviceVerification = m22VerificationValue == "true"
// Opt-in personal trial: a normal production Application under its own package.
val privateTrialValue = providers.gradleProperty("lithicPrivateTrial").orNull
require(privateTrialValue == null || privateTrialValue in setOf("true", "false")) {
    "lithicPrivateTrial must be true or false"
}
val lithicPrivateTrial = privateTrialValue == "true"
// Explicit capture-only target. Separate outputs and a pre-launch plain Application;
// no production entry points or background components are registered in this build.
val c2VisualValue = providers.gradleProperty("c2VisualVerification").orNull
require(c2VisualValue == null || c2VisualValue in setOf("true", "false")) {
    "c2VisualVerification must be true or false"
}
val c2VisualVerification = c2VisualValue == "true"
// Opt-in synthetic isolation probes only. Never reuse a user or historical test identity.
val c6IsolationValue = providers.gradleProperty("c6IsolationVerification").orNull
require(c6IsolationValue == null || c6IsolationValue in setOf("true", "false")) {
    "c6IsolationVerification must be true or false"
}
val c6IsolationVerification = c6IsolationValue == "true"
// Explicit desktop-prepared acceptance pair: normal production App plus an isolated Test host.
val v1AcceptanceValue = providers.gradleProperty("v1AcceptanceVerification").orNull
require(v1AcceptanceValue == null || v1AcceptanceValue in setOf("true", "false")) {
    "v1AcceptanceVerification must be true or false"
}
val v1AcceptanceVerification = v1AcceptanceValue == "true"
if (v1AcceptanceVerification) {
    require(!lithicPrivateTrial && !m22DeviceVerification && !c2VisualVerification && !c6IsolationVerification) {
        "V1 acceptance cannot use private trial, M22, C3 visual or C6 isolation identities"
    }
    require(!providers.gradleProperty("m12T04ProcessDeath").isPresent) {
        "V1 acceptance cannot enable historical process-death experiments"
    }
    require(providers.gradleProperty("m13Isolated").orNull in setOf(null, "true")) {
        "V1 acceptance requires the plain Application M13IsolatedRunner"
    }
    layout.buildDirectory.set(layout.projectDirectory.dir("build-v1-acceptance"))
}
if (c6IsolationVerification) {
    require(!lithicPrivateTrial && !m22DeviceVerification && !c2VisualVerification) {
        "C6 isolation cannot use private trial, M22 or C3 visual identities"
    }
    require(!providers.gradleProperty("m12T04ProcessDeath").isPresent) {
        "C6 isolation cannot enable historical process-death experiments"
    }
    require(providers.gradleProperty("m13Isolated").orNull in setOf(null, "true")) {
        "C6 isolation requires the plain Application M13IsolatedRunner"
    }
    layout.buildDirectory.set(layout.projectDirectory.dir("build-c6-isolation"))
}
if (c2VisualVerification) {
    require(!lithicPrivateTrial && !m22DeviceVerification) {
        "C2 visual capture cannot use private trial or M22 verification identities"
    }
    require(!providers.gradleProperty("m12T04ProcessDeath").isPresent) {
        "C2 visual capture cannot enable historical process-death experiments"
    }
    require(providers.gradleProperty("m13Isolated").orNull in setOf(null, "true")) {
        "C2 visual capture requires the plain Application M13IsolatedRunner"
    }
    // Outside ordinary build/: later ordinary clean tasks must not remove capture artifacts.
    layout.buildDirectory.set(layout.projectDirectory.dir("build-c2-visual"))
}
if (lithicPrivateTrial) {
    require(!m22DeviceVerification) { "Private trial and M22 device verification are separate builds" }
    require(!providers.gradleProperty("m13Isolated").isPresent &&
        !providers.gradleProperty("m12T04ProcessDeath").isPresent) {
        "Private trial must not enable isolated or process-death test builds"
    }
    layout.buildDirectory.set(layout.projectDirectory.dir("build/private-trial"))
}
if (m22DeviceVerification) {
    require(providers.gradleProperty("m13Isolated").orNull == "true") {
        "M22 device verification requires the plain Application M13IsolatedRunner"
    }
    require(!providers.gradleProperty("m12T04ProcessDeath").isPresent) {
        "M22 device verification cannot enable historical process-death experiments"
    }
    layout.buildDirectory.set(layout.projectDirectory.dir("build/m22-device-verification"))
}

android {
    namespace = "com.cooper.lithic"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.cooper.lithic"
        minSdk = 26
        targetSdk = 36
        versionCode = when {
            lithicPrivateTrial -> 28
            v1AcceptanceVerification -> 1
            else -> 17
        }
        versionName = when {
            lithicPrivateTrial -> "0.1.0-preview.12"
            v1AcceptanceVerification -> "0.1.0-v1-a1"
            c6IsolationVerification -> "0.1.0-c6isolation"
            c2VisualVerification -> "0.1.0-c2visual"
            else -> "0.1.0-m22"
        }
        if (c2VisualVerification) testApplicationId = "com.cooper.lithic.c2visual.debug.test"
        if (c6IsolationVerification) testApplicationId = "com.cooper.lithic.c6isolation.debug.test"
        if (v1AcceptanceVerification) testApplicationId = "com.cooper.lithic.v1acceptance.debug.test"
        // T04 is an explicit test-only build; never start the default ledger for self-kill tests.
        testInstrumentationRunner = if (v1AcceptanceVerification || c6IsolationVerification || c2VisualVerification || providers.gradleProperty("m13Isolated").orNull == "true")
            "com.cooper.lithic.app.M13IsolatedRunner"
        else if (providers.gradleProperty("m12T04ProcessDeath").orNull == "true")
            "com.cooper.lithic.data.backup.M12ProcessDeathRunner"
        else "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = when {
                lithicPrivateTrial -> ".preview.debug"
                m22DeviceVerification -> ".m22verify.debug"
                c2VisualVerification -> ".c2visual.debug"
                c6IsolationVerification -> ".c6isolation.debug"
                v1AcceptanceVerification -> ".v1acceptance.debug"
                else -> ".debug"
            }
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
    if (lithicPrivateTrial) {
        // Never silently generate a replacement signing identity for trial updates.
        require(signingConfigs.getByName("debug").storeFile?.isFile == true) {
            "Private trial requires the existing local Debug signing key; do not generate a replacement"
        }
        sourceSets.getByName("debug").manifest.srcFile("src/privateTrial/AndroidManifest.xml")
    }
    if (c2VisualVerification) {
        require(signingConfigs.getByName("debug").storeFile?.isFile == true) {
            "C2 visual capture requires the existing Debug key; do not generate a replacement"
        }
        sourceSets.getByName("debug").manifest.srcFile("src/c2Visual/AndroidManifest.xml")
    }
    if (c6IsolationVerification) {
        require(signingConfigs.getByName("debug").storeFile?.isFile == true) {
            "C6 isolation requires the existing Debug key; do not generate a replacement"
        }
        sourceSets.getByName("debug").manifest.srcFile("src/c6Isolation/AndroidManifest.xml")
    }
    if (v1AcceptanceVerification) {
        require(signingConfigs.getByName("debug").storeFile?.isFile == true) {
            "V1 acceptance requires the existing Debug key; do not generate a replacement"
        }
        // Merge only an identifying label and a non-exported ComponentActivity host.
        // Production Application, launcher, providers and services remain in the main Manifest.
        sourceSets.getByName("debug").manifest.srcFile("src/v1Acceptance/AndroidManifest.xml")
    }
}

// The opt-in target is never a production Release application.
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant ->
        if (v1AcceptanceVerification || c2VisualVerification || c6IsolationVerification) variant.enable = false
    }
}

kotlin {
    jvmToolchain(17)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.work.runtime.ktx)
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
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
    // Only the Debug test-host activity; no test framework enters release runtime.
    if (!lithicPrivateTrial && !c6IsolationVerification && !v1AcceptanceVerification)
        debugImplementation(libs.androidx.compose.ui.test.manifest)
}
