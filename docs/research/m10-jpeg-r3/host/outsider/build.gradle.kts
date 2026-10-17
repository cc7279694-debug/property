plugins { id("com.android.application") }
android {
    namespace = "com.cooper.lithic.m10r3.outsider"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig { applicationId = "com.cooper.lithic.m10r3.outsider"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "r3-outsider" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
