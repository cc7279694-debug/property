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
    // Reuse the already installed/research-verified toolchain; no SDK auto-upgrade.
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.cooper.lithic"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.1.0-m10"
        // Test-only runner delegates normal JUnit runs and supports explicit SIGKILL probes.
        testInstrumentationRunner = "com.cooper.lithic.data.media.M10ProcessBoundaryRunner"
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
    // Candidate native libraries/notices are verified from the locked source build.
    // No binary, SDK path or downloaded dependency is committed to this repository.
    sourceSets.getByName("main").jniLibs.srcDir(layout.buildDirectory.dir("generated/m10StrictJpeg/jniLibs"))
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/m10StrictJpeg/assets"))
}

val prepareStrictJpegCandidateNative by tasks.registering(Exec::class) {
    group = "build"
    description = "Verify the locked four-ABI JPEG candidate and generate JNI/license packaging inputs"
    // Every build checks the source/binary/ELF correspondence, including local caches.
    outputs.upToDateWhen { false }
    doFirst {
        fun required(name: String): String = providers.gradleProperty(name).orNull
            ?: throw GradleException("Missing -P$name; see app/src/main/cpp/README.md")
        commandLine(
            providers.gradleProperty("strictJpegPython").getOrElse("python"), "-B",
            rootProject.file("scripts/build_m10_worker_candidate.py").absolutePath,
            "--sdk", required("strictJpegSdk"),
            "--archive", required("strictJpegArchive"),
            "--native-root", required("strictJpegNativeRoot"),
            "--output", layout.buildDirectory.dir("generated/m10StrictJpeg").get().asFile.absolutePath,
        )
    }
}

tasks.named("preBuild") {
    dependsOn(prepareStrictJpegCandidateNative)
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
