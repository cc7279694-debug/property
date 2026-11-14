$ErrorActionPreference='Stop'
$env:JAVA_HOME='E:/CODEX/MirraAndroid/jdk17/jdk-17.0.20.1+1'
$env:ANDROID_HOME='E:/CODEX/MirraAndroid/sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$gradleArgs=@('--offline','--no-daemon','--console=plain','--max-workers=1','-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8','-Pandroid.overridePathCheck=true','-Pkotlin.compiler.execution.strategy=in-process',':app:compileDebugKotlin',':app:testDebugUnitTest','--tests','*ProjectCostEngineTest','--tests','*ProjectLifecycleRulesTest','--tests','*C2ProjectDetailPresentationTest','--tests','*C2R1ProjectEventLabelsTest','--tests','*C5MoneyMetricPresentationTest','--tests','*CoverPreview*Test',':app:compileDebugAndroidTestKotlin',':app:assembleDebug',':app:assembleDebugAndroidTest',':app:lintDebug')
$log=Join-Path $PSScriptRoot 'desktop-build-final.log'
& .\gradlew.bat @gradleArgs *> $log
$code=$LASTEXITCODE
Set-Content -LiteralPath (Join-Path $PSScriptRoot 'desktop-build-final.exit.txt') -Value $code
Get-Content -LiteralPath $log -Tail 28
exit $code
