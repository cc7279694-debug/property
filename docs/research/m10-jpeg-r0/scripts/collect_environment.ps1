param([string]$OutputPath = 'build/m10-jpeg-r0/environment.json')
$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$tools = @()
foreach ($name in @('cmake', 'ninja', 'clang', 'clang-cl', 'gcc', 'cl', 'python', 'gh', 'agent-reach', 'mcporter')) {
    $command = Get-Command $name -ErrorAction SilentlyContinue
    $tools += [ordered]@{ name = $name; found = [bool]$command; path = $(if ($command) { $command.Source } else { $null }) }
}
$sdkRoots = @('C:/Users/CDD/AppData/Local/MirraAndroid/sdk', 'C:/Users/CDD/AppData/Local/Android/Sdk')
$sdk = @()
foreach ($sdkRoot in $sdkRoots) {
    foreach ($part in @('ndk', 'ndk-bundle', 'cmake', 'build-tools', 'cmdline-tools')) {
        $path = Join-Path $sdkRoot $part
        $sdk += [ordered]@{
            path = $path; exists = (Test-Path -LiteralPath $path)
            children = @($(if (Test-Path -LiteralPath $path -PathType Container) { Get-ChildItem -LiteralPath $path -Directory | ForEach-Object { $_.Name } }))
        }
    }
}
$other = @()
foreach ($path in @('C:/Program Files/CMake/bin/cmake.exe', 'C:/Program Files (x86)/Microsoft Visual Studio/Installer/vswhere.exe', 'C:/Program Files/Microsoft Visual Studio', 'C:/Program Files (x86)/Microsoft Visual Studio', 'C:/BuildTools', 'C:/msys64', 'C:/mingw64', 'E:/Android/ndk', 'E:/Android/cmake', 'E:/AndroidSDK', 'E:/SDK', 'C:/Program Files/Git/usr/bin/gpg.exe')) {
    $other += [ordered]@{ path = $path; exists = (Test-Path -LiteralPath $path) }
}
$declared = [ordered]@{}
foreach ($name in @('ANDROID_HOME', 'ANDROID_SDK_ROOT', 'ANDROID_NDK_HOME', 'ANDROID_NDK_ROOT', 'NDK_HOME', 'CMAKE_HOME')) {
    $declared[$name] = [Environment]::GetEnvironmentVariable($name)
}
$result = [ordered]@{
    kind = 'ENVIRONMENT_PREFLIGHT_ONLY'; recordedUtc = [DateTime]::UtcNow.ToString('o')
    worktree = $root; branch = (& git branch --show-current); sourceHead = (& git rev-parse HEAD)
    tools = $tools; sdkLocations = $sdk; otherLocations = $other; selectedEnvironmentPaths = $declared
    androidNativePoc = 'BLOCKED'; hostNativePoc = 'BLOCKED'
    reason = 'No NDK/CMake in configured SDK locations, no C/C++ compiler in PATH or checked conventional locations.'
    scope = 'Targeted inventory, not a whole-disk proof that no compiler exists anywhere. No installation, build, device operation or broad environment dump.'
}
$resolved = [IO.Path]::GetFullPath((Join-Path $root $OutputPath))
if (-not $resolved.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Output must remain inside this research worktree.' }
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($resolved)) | Out-Null
$json = $result | ConvertTo-Json -Depth 8
[IO.File]::WriteAllText($resolved, $json + "`n", [Text.UTF8Encoding]::new($false))
Write-Output $json
