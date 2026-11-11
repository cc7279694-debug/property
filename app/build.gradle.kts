import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.HexFormat
import javax.naming.ldap.LdapName

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
// Disposable PHOTO-LITE experiment only. Never opens media writing in ordinary apps.
val photoLiteValue = providers.gradleProperty("photoLiteExperiment").orNull
require(photoLiteValue == null || photoLiteValue in setOf("true", "false")) {
    "photoLiteExperiment must be true or false"
}
val photoLiteExperiment = photoLiteValue == "true"
// Normal production entry points under a disposable, explicitly selected SAF acceptance identity.
val b1QaValue = providers.gradleProperty("b1QaVerification").orNull
require(b1QaValue == null || b1QaValue in setOf("true", "false")) {
    "b1QaVerification must be true or false"
}
val b1QaVerification = b1QaValue == "true"
val p2A1Value = providers.gradleProperty("p2A1Verification").orNull
require(p2A1Value == null || p2A1Value in setOf("true", "false")) {
    "p2A1Verification must be true or false"
}
val p2A1Verification = p2A1Value == "true"
if (p2A1Verification) {
    require(!(lithicPrivateTrial || m22DeviceVerification || c2VisualVerification ||
        c6IsolationVerification || v1AcceptanceVerification || photoLiteExperiment || b1QaVerification)) {
        "P2-A1 requires its own verification identity"
    }
    require(!providers.gradleProperty("m12T04ProcessDeath").isPresent &&
        providers.gradleProperty("m13Isolated").orNull in setOf(null, "true")) {
        "P2-A1 requires M13IsolatedRunner and disallows historical process-death tests"
    }
    require(providers.environmentVariable("LITHIC_PRIVATE_APK_SIGNING").orNull != "true") {
        "P2-A1 does not support private Release signing"
    }
    layout.buildDirectory.set(layout.projectDirectory.dir("build-p2-a1"))
}
if (b1QaVerification) {
    require(!lithicPrivateTrial && !m22DeviceVerification && !c2VisualVerification &&
        !c6IsolationVerification && !v1AcceptanceVerification && !photoLiteExperiment) {
        "B1 QA cannot share private trial or historical verification identities"
    }
    require(!providers.gradleProperty("m13Isolated").isPresent &&
        !providers.gradleProperty("m12T04ProcessDeath").isPresent) {
        "B1 QA is a normal Application, not an isolated or process-death test host"
    }
    require(gradle.startParameter.taskNames.none {
        val task = it.substringAfterLast(':').lowercase()
        task.contains("androidtest") || task.startsWith("connected") || task.contains("manageddevice")
    }) { "B1 QA only prepares a normal user App; Android test tasks are not supported" }
    layout.buildDirectory.set(layout.projectDirectory.dir("build-b1-qa"))
}
if (photoLiteExperiment) {
    require(!lithicPrivateTrial && !m22DeviceVerification && !c2VisualVerification &&
        !c6IsolationVerification && !v1AcceptanceVerification) {
        "PHOTO-LITE cannot share private trial or historical verification identities"
    }
    require(!providers.gradleProperty("m12T04ProcessDeath").isPresent) {
        "PHOTO-LITE cannot enable historical process-death experiments"
    }
    require(providers.gradleProperty("m13Isolated").orNull in setOf(null, "true")) {
        "PHOTO-LITE tests require the plain Application M13IsolatedRunner"
    }
    layout.buildDirectory.set(layout.projectDirectory.dir("build-photo-lite"))
}
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

// Release compilation and lint do not need signing material. Only an owner-authorized
// local signing session should set this environment flag.
val privateApkSigning = providers.environmentVariable("LITHIC_PRIVATE_APK_SIGNING").orNull == "true"

android {
    namespace = "com.cooper.lithic"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.cooper.lithic"
        minSdk = 26
        targetSdk = 36
        versionCode = when {
            lithicPrivateTrial -> 35
            b1QaVerification -> 1
            v1AcceptanceVerification || p2A1Verification -> 1
            photoLiteExperiment -> 1
            else -> 17
        }
        versionName = when {
            lithicPrivateTrial -> "0.1.0-preview.19"
            b1QaVerification -> "0.1.0-b1qa.1"
            v1AcceptanceVerification -> "0.1.0-v1-a1"
            photoLiteExperiment -> "0.1.0-photo-lite.p1"
            c6IsolationVerification -> "0.1.0-c6isolation"
            c2VisualVerification -> "0.1.0-c2visual"
            p2A1Verification -> "0.1.0-p2a1"
            else -> "0.1.0-m22"
        }
        if (p2A1Verification) testApplicationId = "com.cooper.lithic.p2a1.debug.test"
        if (c2VisualVerification) testApplicationId = "com.cooper.lithic.c2visual.debug.test"
        if (c6IsolationVerification) testApplicationId = "com.cooper.lithic.c6isolation.debug.test"
        if (v1AcceptanceVerification) testApplicationId = "com.cooper.lithic.v1acceptance.debug.test"
        if (photoLiteExperiment) testApplicationId = "com.cooper.lithic.photolite.debug.test"
        buildConfigField("boolean", "PHOTO_LITE_EXPERIMENT", "false")
        buildConfigField("boolean", "PHOTO_LITE_PRODUCTION", "false")
        // T04 is an explicit test-only build; never start the default ledger for self-kill tests.
        testInstrumentationRunner = if (photoLiteExperiment || v1AcceptanceVerification || c6IsolationVerification || c2VisualVerification || p2A1Verification || providers.gradleProperty("m13Isolated").orNull == "true")
            "com.cooper.lithic.app.M13IsolatedRunner"
        else if (providers.gradleProperty("m12T04ProcessDeath").orNull == "true")
            "com.cooper.lithic.data.backup.M12ProcessDeathRunner"
        else "androidx.test.runner.AndroidJUnitRunner"
    }

    val privateReleaseSigning = if (privateApkSigning) {
        val request = gradle.startParameter
        check(request.taskNames == listOf(":app:assembleRelease") &&
            request.excludedTaskNames.isEmpty() && request.isOffline && !request.isDryRun) {
            "Private Release signing requires exactly the offline :app:assembleRelease task"
        }
        check(!lithicPrivateTrial && !m22DeviceVerification && !b1QaVerification &&
            !photoLiteExperiment && !v1AcceptanceVerification && !c2VisualVerification &&
            !c6IsolationVerification && !providers.gradleProperty("m13Isolated").isPresent &&
            !providers.gradleProperty("m12T04ProcessDeath").isPresent) {
            "Private Release signing cannot be combined with a trial or test identity"
        }
        check(!gradle.startParameter.isConfigurationCacheRequested && !gradle.startParameter.isBuildCacheEnabled) {
            "Private Release signing requires disabled configuration and build caches"
        }
        val injectedSigningFields = listOf(
            "store.file", "store.type", "store.password", "key.alias", "key.password",
            "v1-enabled", "v2-enabled", "v3-enabled", "v4-enabled",
        )
        check(injectedSigningFields.none { providers.gradleProperty("android.injected.signing.$it").isPresent }) {
            "Private Release signing forbids Android signing overrides"
        }

        fun requiredSigningEnv(name: String): String =
            providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
                ?: error("Private Release signing input is missing: $name")

        val repositoryPath = rootProject.projectDir.toPath().toRealPath()

        fun externalSigningPath(name: String): Path {
            val supplied = requiredSigningEnv(name)
            val path = runCatching {
                Path.of(supplied).takeIf { it.isAbsolute }?.toRealPath()
            }.getOrNull() ?: error("Private Release signing input requires an existing absolute path: $name")
            check(!path.startsWith(repositoryPath)) {
                "Private Release signing files and output must be outside the repository"
            }
            return path
        }

        val keyStorePath = externalSigningPath("LITHIC_RELEASE_KEYSTORE")
        val releaseBuildPath = externalSigningPath("LITHIC_RELEASE_BUILD_DIR")
        val emptyBuildDirectory = runCatching {
            Files.newDirectoryStream(releaseBuildPath).use { !it.iterator().hasNext() }
        }.getOrDefault(false)
        check(emptyBuildDirectory && !repositoryPath.startsWith(releaseBuildPath) &&
            !keyStorePath.startsWith(releaseBuildPath)) {
            "Private Release signing requires a separate, empty owner-controlled build directory"
        }
        val expectedSha = requiredSigningEnv("LITHIC_RELEASE_CERT_SHA256").lowercase()
        check(expectedSha.matches(Regex("[0-9a-f]{64}")) &&
            expectedSha != "29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9") {
            "Private Release requires an approved certificate fingerprint distinct from the trial Debug certificate"
        }
        val alias = requiredSigningEnv("LITHIC_RELEASE_KEY_ALIAS")
        val storeSecret = requiredSigningEnv("LITHIC_RELEASE_STORE_PASSWORD")
        val keySecret = requiredSigningEnv("LITHIC_RELEASE_KEY_PASSWORD")
        val verifiedStoreType = runCatching {
            val keyStore = KeyStore.getInstance(keyStorePath.toFile(), storeSecret.toCharArray())
            val certificate = keyStore.getCertificate(alias) as X509Certificate
            certificate.checkValidity()
            check(LdapName(certificate.subjectX500Principal.name).rdns.none {
                it.type.equals("CN", ignoreCase = true) &&
                    it.value.toString().equals("Android Debug", ignoreCase = true)
            })
            val certSha = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(certificate.encoded),
            )
            check(certSha == expectedSha)
            val key = keyStore.getKey(alias, keySecret.toCharArray()) as PrivateKey
            val algorithm = when (key.algorithm) {
                "RSA" -> "SHA256withRSA"
                "EC" -> "SHA256withECDSA"
                else -> error("Unsupported private signing key algorithm")
            }
            val challenge = "Lithic private Release signing preflight".encodeToByteArray()
            val proof = Signature.getInstance(algorithm)
            proof.initSign(key)
            proof.update(challenge)
            val signature = proof.sign()
            proof.initVerify(certificate)
            proof.update(challenge)
            check(proof.verify(signature))
            keyStore.type
        }.getOrNull() ?: error("Private Release signing preflight failed: check credentials, alias and certificate")

        layout.buildDirectory.set(releaseBuildPath.toFile())
        signingConfigs.create("privateApkRelease") {
            storeFile = keyStorePath.toFile()
            storeType = verifiedStoreType
            keyAlias = alias
            storePassword = storeSecret
            keyPassword = keySecret
        }
    } else null

    buildTypes {
        debug {
            // Keep the trial and disposable verification identities separate from production.
            buildConfigField("boolean", "PHOTO_LITE_EXPERIMENT", (photoLiteExperiment || lithicPrivateTrial || b1QaVerification).toString())
            applicationIdSuffix = when {
                lithicPrivateTrial -> ".preview.debug"
                b1QaVerification -> ".b1qa.debug"
                m22DeviceVerification -> ".m22verify.debug"
                c2VisualVerification -> ".c2visual.debug"
                p2A1Verification -> ".p2a1.debug"
                c6IsolationVerification -> ".c6isolation.debug"
                v1AcceptanceVerification -> ".v1acceptance.debug"
                photoLiteExperiment -> ".photolite.debug"
                else -> ".debug"
            }
            versionNameSuffix = "-debug"
        }
        release {
            buildConfigField("boolean", "PHOTO_LITE_PRODUCTION", "true")
            privateReleaseSigning?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Exact exported schema fixtures are test-only assets; never packaged in the app APK.
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
    if (b1QaVerification) {
        require(signingConfigs.getByName("debug").storeFile?.isFile == true) {
            "B1 QA requires the existing Debug key; do not generate a replacement"
        }
        sourceSets.getByName("debug").manifest.srcFile("src/b1Qa/AndroidManifest.xml")
    }
    if (lithicPrivateTrial) {
        // Never silently generate a replacement signing identity for trial updates.
        require(signingConfigs.getByName("debug").storeFile?.isFile == true) {
            "Private trial requires the existing local Debug signing key; do not generate a replacement"
        }
        sourceSets.getByName("debug").manifest.srcFile("src/privateTrial/AndroidManifest.xml")
    }
    if (p2A1Verification) {
        require(signingConfigs.getByName("debug").storeFile?.isFile == true) {
            "P2-A1 requires the existing Debug key"
        }
        sourceSets.getByName("debug").manifest.srcFile("src/p2A1/AndroidManifest.xml")
        sourceSets.getByName("androidTest").java.srcDir("src/p2A1/androidTest/java")
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
    if (photoLiteExperiment) {
        require(signingConfigs.getByName("debug").storeFile?.isFile == true) {
            "PHOTO-LITE requires the existing Debug key; do not generate a replacement"
        }
        sourceSets.getByName("debug").manifest.srcFile("src/photoLite/AndroidManifest.xml")
    }
}

// The opt-in target is never a production Release application.
androidComponents {
    beforeVariants(selector().withBuildType("debug")) { variant ->
        if (b1QaVerification) variant.enableAndroidTest = false
    }
    beforeVariants(selector().withBuildType("release")) { variant ->
        if (lithicPrivateTrial || m22DeviceVerification || b1QaVerification || photoLiteExperiment ||
            v1AcceptanceVerification || c2VisualVerification || c6IsolationVerification || p2A1Verification) variant.enable = false
    }
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.versionName.set("1.0.0")
            output.versionCode.set(36)
        }
    }
}

// Guard the actual packaging tasks, including indirect invocations.
val verifyPrivateApkSigning by tasks.registering {
    doLast {
        check(privateApkSigning) { "Formal APK packaging requires an approved private Release signer" }
    }
}
val rejectReleaseBundle by tasks.registering {
    doLast { error("Lithic private Release supports direct APK delivery only") }
}
tasks.configureEach {
    when (name) {
        "packageRelease" -> {
            dependsOn(verifyPrivateApkSigning)
            doFirst {
                check(privateApkSigning) { "Formal APK packaging requires an approved private Release signer" }
            }
        }
        "bundleRelease", "packageReleaseBundle", "signReleaseBundle",
        "makeApkFromBundleForRelease", "packageReleaseUniversalApk" -> {
            dependsOn(rejectReleaseBundle)
            doFirst { error("Lithic private Release supports direct APK delivery only") }
        }
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
    if (!b1QaVerification && !photoLiteExperiment && !lithicPrivateTrial && !c6IsolationVerification && !v1AcceptanceVerification && !p2A1Verification)
        debugImplementation(libs.androidx.compose.ui.test.manifest)
}
