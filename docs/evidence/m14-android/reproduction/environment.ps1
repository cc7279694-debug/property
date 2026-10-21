$env:JAVA_HOME='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1'
$env:ANDROID_HOME='C:/Users/CDD/AppData/Local/MirraAndroid/sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
Remove-Item Env:ANDROID_SDK_HOME -ErrorAction SilentlyContinue
Remove-Item Env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue
$env:ANDROID_USER_HOME='E:/CODEX/.codex/research-env/lithic-m14-android-20261021/android-user'
$env:ANDROID_AVD_HOME='E:/CODEX/.codex/research-env/lithic-m14-android-20261021/avd-registration'
$env:ANDROID_EMULATOR_HOME='E:/CODEX/.codex/research-env/lithic-m14-android-20261021/emulator-home'
