param(
    [string]$Apk = 'app/build/private-trial/outputs/apk/debug/app-debug.apk',
    [string]$Aapt2 = 'C:/Users/CDD/AppData/Local/MirraAndroid/sdk/build-tools/36.0.0/aapt2.exe',
    [ValidatePattern('^icon(?:-[0-9]+)?$')][string]$EvidenceName = 'icon'
)
# Desktop artifact inspection only. Never contacts Android or edits production resources.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
$repo = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../..')).Path
if (-not [IO.Path]::IsPathRooted($Apk)) { $Apk = Join-Path $repo $Apk }
$Apk = (Resolve-Path -LiteralPath $Apk).Path
if (-not (Test-Path -LiteralPath $Aapt2 -PathType Leaf)) { throw 'Existing aapt2 is unavailable; do not install a replacement.' }
$output = Join-Path $PSScriptRoot $EvidenceName
if (Test-Path -LiteralPath $output) { throw 'Icon evidence directory already exists; preserve it without overwriting.' }
[void](New-Item -ItemType Directory -Path $output)
$commands = [Collections.Generic.List[object]]::new()
$utf8 = [Text.UTF8Encoding]::new($false)

function Get-BytesSha([byte[]]$Bytes) {
    [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($Bytes)).ToLowerInvariant()
}
function Write-Json($Value, [string]$Name) {
    [IO.File]::WriteAllText((Join-Path $output $Name), (($Value | ConvertTo-Json -Depth 8) -replace "`r`n", "`n") + "`n", $utf8)
}
function Invoke-Aapt([string]$Label, [string[]]$Arguments, [bool]$SaveFullOutput = $true) {
    $start = [Diagnostics.ProcessStartInfo]::new($Aapt2)
    $start.UseShellExecute = $false; $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true; $start.RedirectStandardError = $true
    foreach ($argument in $Arguments) { $start.ArgumentList.Add($argument) }
    $process = [Diagnostics.Process]::new(); $process.StartInfo = $start
    $stdout = [IO.MemoryStream]::new(); $stderr = [IO.MemoryStream]::new()
    $began = [DateTime]::UtcNow.ToString('o')
    try {
        if (-not $process.Start()) { throw 'Could not start aapt2.' }
        $outCopy = $process.StandardOutput.BaseStream.CopyToAsync($stdout)
        $errCopy = $process.StandardError.BaseStream.CopyToAsync($stderr)
        $timedOut = -not $process.WaitForExit(30000)
        if ($timedOut) { $process.Kill(); $process.WaitForExit() } # Only this newly started desktop reader.
        $outCopy.GetAwaiter().GetResult(); $errCopy.GetAwaiter().GetResult()
        $outBytes = $stdout.ToArray(); $errBytes = $stderr.ToArray()
        if ($SaveFullOutput) { [IO.File]::WriteAllBytes((Join-Path $output "$Label.stdout.txt"), $outBytes) }
        [IO.File]::WriteAllBytes((Join-Path $output "$Label.stderr.txt"), $errBytes)
        $commands.Add([ordered]@{
            label = $Label; argv = @($Aapt2) + $Arguments; startedUtc = $began
            exitCode = $process.ExitCode; timedOut = $timedOut
            stdoutBytes = $outBytes.Length; stdoutSha256 = Get-BytesSha $outBytes
            stdoutSavedComplete = $SaveFullOutput
            stderrBytes = $errBytes.Length; stderrSha256 = Get-BytesSha $errBytes
        })
        Write-Json $commands.ToArray() 'commands.json'
        if ($timedOut -or $process.ExitCode -ne 0) { throw "aapt2 $Label failed; its evidence is preserved." }
        return $utf8.GetString($outBytes)
    } finally { $stdout.Dispose(); $stderr.Dispose(); $process.Dispose() }
}
function Require-UniqueEntry($Zip, [string]$Pattern) {
    $found = @($Zip.Entries | Where-Object { $_.FullName -match $Pattern })
    if ($found.Count -ne 1) { throw "Expected exactly one APK entry matching $Pattern; found $($found.Count)." }
    return $found[0]
}
function Read-ZipBytes($Entry) {
    $input = $Entry.Open(); $memory = [IO.MemoryStream]::new()
    try { $input.CopyTo($memory); return ,$memory.ToArray() }
    finally { $input.Dispose(); $memory.Dispose() }
}
function Read-ElementAttributes([string]$Text, [string]$Name) {
    $found = [regex]::Matches($Text, '(?ms)^\s*E:\s*' + [regex]::Escape($Name) + '\b[^\r\n]*\r?\n(?<body>.*?)(?=^\s*E:|\z)')
    if ($found.Count -ne 1) { throw "Expected one $Name XML element." }
    return $found[0].Groups['body'].Value
}
function Require-Reference([string]$Attributes, [string]$Name, [string]$Id) {
    $found = [regex]::Matches($Attributes, ':' + [regex]::Escape($Name) + '(?:\(0x[0-9a-fA-F]+\))?\s*=\s*@(?<id>0x[0-9a-fA-F]+)\b')
    if ($found.Count -ne 1 -or $found[0].Groups['id'].Value.ToLowerInvariant() -ne $Id) {
        throw "Compiled $Name does not uniquely reference $Id."
    }
}
function Get-BinaryInset([byte[]]$Bytes) {
    # Read the typed fraction itself. aapt2 prints 0.23% for a stored 0.23 fraction;
    # its human-readable suffix must not be mistaken for 0.23 percent of the canvas.
    if ($Bytes.Length -lt 8 -or [BitConverter]::ToUInt16($Bytes, 0) -ne 3 -or
        [BitConverter]::ToUInt32($Bytes, 4) -ne $Bytes.Length) { throw 'Invalid binary XML envelope.' }
    $map = @(); $found = @(); $position = [int][BitConverter]::ToUInt16($Bytes, 2)
    while ($position -lt $Bytes.Length) {
        if ($position + 8 -gt $Bytes.Length) { throw 'Truncated XML chunk.' }
        $type = [BitConverter]::ToUInt16($Bytes, $position)
        $header = [int][BitConverter]::ToUInt16($Bytes, $position + 2)
        $size = [int][BitConverter]::ToUInt32($Bytes, $position + 4)
        if ($header -lt 8 -or $size -lt $header -or $size -gt $Bytes.Length - $position) { throw 'Invalid XML chunk bounds.' }
        if ($type -eq 0x180) {
            if ($map.Count -ne 0 -or ($size - $header) % 4 -ne 0) { throw 'Ambiguous XML resource map.' }
            $map = @(for ($offset = $header; $offset -lt $size; $offset += 4) { [BitConverter]::ToUInt32($Bytes, $position + $offset) })
        } elseif ($type -eq 0x102) {
            $extension = $position + $header
            if ($extension + 20 -gt $position + $size) { throw 'Truncated XML element.' }
            $start = [int][BitConverter]::ToUInt16($Bytes, $extension + 8)
            $width = [int][BitConverter]::ToUInt16($Bytes, $extension + 10)
            $count = [int][BitConverter]::ToUInt16($Bytes, $extension + 12)
            if ($start -lt 20 -or $width -ne 20 -or $extension + $start + $count * $width -gt $position + $size) { throw 'Invalid XML attributes.' }
            for ($i = 0; $i -lt $count; $i++) {
                $attribute = $extension + $start + $i * $width
                $name = [BitConverter]::ToUInt32($Bytes, $attribute + 4)
                if ($name -ge $map.Count -or $map[$name] -ne 0x010104b5) { continue }
                $dataType = $Bytes[$attribute + 15]; $data = [BitConverter]::ToUInt32($Bytes, $attribute + 16)
                if ([BitConverter]::ToUInt16($Bytes, $attribute + 12) -ne 8 -or $dataType -ne 6 -or ($data % 16) -ne 0) { throw 'Inset must be a base fraction.' }
                $radix = [int](($data -shr 4) -band 3)
                $mantissa = [long]$data - ($data % 256)
                if ($mantissa -ge 2147483648L) { $mantissa -= 4294967296L }
                $multipliers = @((1.0 / 256), (1.0 / 32768), (1.0 / 8388608), (1.0 / 2147483648))
                $found += [ordered]@{ type = $dataType; rawComplexHex = ('0x{0:x8}' -f $data); unit = 0; fraction = $mantissa * $multipliers[$radix]; percent = $mantissa * $multipliers[$radix] * 100 }
            }
        }
        $position += $size
    }
    if ($found.Count -ne 1) { throw 'Expected one binary android:inset attribute.' }
    return $found[0]
}
function Get-PixelReceipt([byte[]]$Bytes) {
    if ($Bytes.Length -lt 8 -or [Convert]::ToHexString($Bytes[0..7]) -ne '89504E470D0A1A0A') { throw 'Expected PNG bytes.' }
    $stream = [IO.MemoryStream]::new($Bytes, $false); $bitmap = $null; $bits = $null
    try {
        $bitmap = [Drawing.Bitmap]::new($stream)
        if ($bitmap.Width -ne 1254 -or $bitmap.Height -ne 1254) { throw 'Unexpected icon dimensions.' }
        $bits = $bitmap.LockBits([Drawing.Rectangle]::new(0, 0, $bitmap.Width, $bitmap.Height),
            [Drawing.Imaging.ImageLockMode]::ReadOnly, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $row = [byte[]]::new($bitmap.Width * 4)
        $canonical = [byte[]]::new($bitmap.Width * $bitmap.Height * 4)
        $transparent = 0; $nonzero = 0
        for ($y = 0; $y -lt $bitmap.Height; $y++) {
            [Runtime.InteropServices.Marshal]::Copy([IntPtr]::Add($bits.Scan0, $y * $bits.Stride), $row, 0, $row.Length)
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                $at = $x * 4; $destination = ($y * $bitmap.Width + $x) * 4; $alpha = $row[$at + 3]
                if ($alpha -eq 0) { $transparent++ } # RGB remains zero for fully transparent pixels.
                else {
                    $nonzero++; $canonical[$destination] = $row[$at + 2]
                    $canonical[$destination + 1] = $row[$at + 1]; $canonical[$destination + 2] = $row[$at]
                    $canonical[$destination + 3] = $alpha
                }
            }
        }
        if ($transparent -eq 0 -or $nonzero -eq 0) { throw 'Icon must contain both transparent and visible pixels.' }
        return [ordered]@{ width = $bitmap.Width; height = $bitmap.Height; bytes = $Bytes.Length
            encodedSha256 = Get-BytesSha $Bytes; canonicalRgbaSha256 = Get-BytesSha $canonical
            transparentPixels = $transparent; visiblePixels = $nonzero
            canonicalPolicy = 'RGBA scanlines; RGB zeroed only when alpha is zero; no resize or pixel edits' }
    } finally {
        if ($null -ne $bits) { $bitmap.UnlockBits($bits) }
        if ($null -ne $bitmap) { $bitmap.Dispose() }; $stream.Dispose()
    }
}

$zip = $null
try {
    $apkSha = (Get-FileHash -Algorithm SHA256 -LiteralPath $Apk).Hash.ToLowerInvariant()
    $zip = [IO.Compression.ZipFile]::OpenRead($Apk)
    $entries = [ordered]@{
        launcher = Require-UniqueEntry $zip '^res/mipmap[^/]*/ic_launcher\.xml$'
        round = Require-UniqueEntry $zip '^res/mipmap[^/]*/ic_launcher_round\.xml$'
        foreground = Require-UniqueEntry $zip '^res/drawable[^/]*/ic_launcher_foreground\.xml$'
        background = Require-UniqueEntry $zip '^res/drawable[^/]*/ic_launcher_background\.xml$'
        stone = Require-UniqueEntry $zip '^res/drawable[^/]*/ic_launcher_stone\.png$'
    }
    $resources = Invoke-Aapt 'resources' @('dump', 'resources', $Apk) $false
    $ids = @{}
    foreach ($resource in @('drawable/ic_launcher_background','drawable/ic_launcher_foreground','drawable/ic_launcher_stone','mipmap/ic_launcher','mipmap/ic_launcher_round')) {
        $matchesForName = [regex]::Matches($resources, '(?m)^\s*resource\s+(?<id>0x[0-9a-fA-F]+)\s+(?:[\w.]+:)?' + [regex]::Escape($resource) + '\s*$')
        if ($matchesForName.Count -ne 1) { throw "Cannot uniquely resolve compiled resource $resource." }
        $ids[$resource] = $matchesForName[0].Groups['id'].Value.ToLowerInvariant()
    }
    $resourceLines = @($resources -split '\r?\n' | Where-Object { $_ -match 'ic_launcher' })
    [IO.File]::WriteAllText((Join-Path $output 'launcher-resources-only.txt'), ($resourceLines -join "`n") + "`n", $utf8)
    $xml = @{}
    foreach ($name in @('launcher','round','foreground','background')) {
        $xml[$name] = Invoke-Aapt $name @('dump', 'xmltree', $Apk, '--file', $entries[$name].FullName)
    }
    foreach ($name in @('launcher','round')) {
        [void](Read-ElementAttributes $xml[$name] 'adaptive-icon')
        Require-Reference (Read-ElementAttributes $xml[$name] 'background') 'drawable' $ids['drawable/ic_launcher_background']
        Require-Reference (Read-ElementAttributes $xml[$name] 'foreground') 'drawable' $ids['drawable/ic_launcher_foreground']
    }
    $inset = Read-ElementAttributes $xml.foreground 'inset'
    $fraction = [regex]::Matches($inset, ':inset(?:\(0x[0-9a-fA-F]+\))?\s*=\s*(?<value>\d+(?:\.\d+)?)%\s*$')
    $binaryInset = Get-BinaryInset (Read-ZipBytes $entries.foreground)
    if ($fraction.Count -ne 1 -or [Math]::Abs([double]$binaryInset.percent - 23) -gt 0.001 -or
        [Math]::Abs([double]::Parse($fraction[0].Groups['value'].Value,
        [Globalization.CultureInfo]::InvariantCulture) - [double]$binaryInset.fraction) -gt 0.000001) { throw 'Binary inset must be 23%, consistent with aapt2 fraction output.' }
    Require-Reference (Read-ElementAttributes $xml.foreground 'bitmap') 'src' $ids['drawable/ic_launcher_stone']
    $backgroundElements = @([regex]::Matches($xml.background, '(?m)^\s*E:\s*(?<name>[\w-]+)\b') | ForEach-Object { $_.Groups['name'].Value })
    if (($backgroundElements -join ',') -ne 'shape,solid') { throw 'Background must be a plain shape and solid without an inner frame.' }
    $solid = Read-ElementAttributes $xml.background 'solid'
    if ($solid -notmatch ':color(?:\(0x[0-9a-fA-F]+\))?\s*=\s*#ff282b28\b') { throw 'Compiled background color must be opaque #282B28.' }
    $shape = Read-ElementAttributes $xml.background 'shape'
    if ($shape -notmatch ':shape(?:\(0x[0-9a-fA-F]+\))?\s*=\s*(?:0x0|0)\s*$') { throw 'Compiled background must be a rectangle; the launcher supplies its outer mask.' }
    $manifest = Invoke-Aapt 'manifest' @('dump', 'xmltree', $Apk, '--file', 'AndroidManifest.xml')
    $application = Read-ElementAttributes $manifest 'application'
    Require-Reference $application 'icon' $ids['mipmap/ic_launcher']
    Require-Reference $application 'roundIcon' $ids['mipmap/ic_launcher_round']
    $sourcePath = Join-Path $repo 'app/src/main/res/drawable-nodpi/ic_launcher_stone.png'
    $source = Get-PixelReceipt ([IO.File]::ReadAllBytes($sourcePath))
    $compiled = Get-PixelReceipt (Read-ZipBytes $entries.stone)
    if ($source.canonicalRgbaSha256 -ne $compiled.canonicalRgbaSha256) { throw 'Compiled icon pixels differ from the new source foreground.' }
    if ((Get-FileHash -Algorithm SHA256 -LiteralPath $Apk).Hash.ToLowerInvariant() -ne $apkSha) { throw 'APK changed during icon inspection.' }
    $result = [ordered]@{ status = 'PASS_STATIC_ARTIFACT_ONLY'; apkSha256 = $apkSha; resourceIds = $ids
        source = $source; compiled = $compiled; canonicalPixelsEqual = $true
        encodedBytesEqual = ($source.encodedSha256 -eq $compiled.encodedSha256)
        compiledEntries = @($entries.Values | ForEach-Object { $_.FullName })
        insetPercent = $binaryInset.percent; binaryInset = $binaryInset; background = '#282B28'; manifestIconBinding = 'PASS'
        androidLauncherVisual = 'NOT_RUN'; deviceOperations = 0 }
    Write-Json $result 'result.json'
    $result | ConvertTo-Json -Depth 8
} catch {
    Write-Json ([ordered]@{ status = 'FAILED'; error = $_.Exception.Message; androidLauncherVisual = 'NOT_RUN'; deviceOperations = 0 }) 'failure.json'
    throw
} finally { if ($null -ne $zip) { $zip.Dispose() } }
