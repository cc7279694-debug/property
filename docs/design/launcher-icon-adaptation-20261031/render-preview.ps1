# Desktop layer/mask simulation only. Does not edit production assets or contact Android.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$iconRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../..')).Path
$androidNamespace = 'http://schemas.android.com/apk/res/android'
[xml]$foregroundXml = Get-Content -Raw -LiteralPath (Join-Path $iconRoot 'app/src/main/res/drawable/ic_launcher_foreground.xml')
[xml]$backgroundXml = Get-Content -Raw -LiteralPath (Join-Path $iconRoot 'app/src/main/res/drawable/ic_launcher_background.xml')
$insetText = $foregroundXml.DocumentElement.GetAttribute('inset', $androidNamespace)
if ($insetText -notmatch '^(\d+(?:\.\d+)?)%$') { throw 'Expected one percentage inset on the foreground.' }
$foregroundInset = [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture) / 100
if ($foregroundInset -lt 0 -or $foregroundInset -ge 0.5) { throw 'Unsupported foreground inset.' }
$artReference = $foregroundXml.DocumentElement.SelectSingleNode('bitmap').GetAttribute('src', $androidNamespace)
if ($artReference -notmatch '^@drawable/([a-z0-9_]+)$') { throw 'Unexpected foreground drawable reference.' }
$foregroundPath = Join-Path $iconRoot ('app/src/main/res/drawable-nodpi/' + $Matches[1] + '.png')
$backgroundText = $backgroundXml.DocumentElement.SelectSingleNode('solid').GetAttribute('color', $androidNamespace)
if ($backgroundText -notmatch '^#[0-9A-Fa-f]{6}$') { throw 'Expected an opaque RGB background color.' }
$backgroundColor = [Drawing.ColorTranslator]::FromHtml($backgroundText)
$legacyPath = Join-Path $iconRoot 'app/src/main/res/drawable-nodpi/ic_launcher_art.png'

function Set-PreviewQuality([Drawing.Graphics]$Graphics) {
    $Graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $Graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $Graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $Graphics.CompositingQuality = [Drawing.Drawing2D.CompositingQuality]::HighQuality
}

function New-PreviewMask([single]$Side, [bool]$Circle) {
    $path = [Drawing.Drawing2D.GraphicsPath]::new()
    if ($Circle) { $path.AddEllipse([single]0, [single]0, $Side, $Side) }
    else {
        $arc = [single]($Side * 0.46)
        $path.AddArc([single]0, [single]0, $arc, $arc, 180, 90)
        $path.AddArc([single]($Side - $arc), [single]0, $arc, $arc, 270, 90)
        $path.AddArc([single]($Side - $arc), [single]($Side - $arc), $arc, $arc, 0, 90)
        $path.AddArc([single]0, [single]($Side - $arc), $arc, $arc, 90, 90)
        $path.CloseFigure()
    }
    return $path
}

function New-LayeredPreview([Drawing.Image]$Art, [double]$Inset, [Drawing.Color]$Background,
    [int]$Size, [bool]$Circle) {
    # 108/72 is an explicit desktop approximation, not an OEM launcher contract.
    # Background fills the layer. Only the alpha foreground receives the XML inset.
    $side = $Size * 4
    $layer = [Drawing.Bitmap]::new($side, $side, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $layerGraphics = [Drawing.Graphics]::FromImage($layer)
    $masked = [Drawing.Bitmap]::new($side, $side, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $maskedGraphics = [Drawing.Graphics]::FromImage($masked)
    $result = [Drawing.Bitmap]::new($Size, $Size, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $resultGraphics = [Drawing.Graphics]::FromImage($result)
    $mask = $null
    $texture = $null
    try {
        Set-PreviewQuality $layerGraphics
        Set-PreviewQuality $maskedGraphics
        Set-PreviewQuality $resultGraphics
        $layerGraphics.Clear($Background)
        $extent = $side * (108.0 / 72.0) * (1 - 2 * $Inset)
        $offset = ($side - $extent) / 2
        $layerGraphics.DrawImage($Art, [Drawing.RectangleF]::new([single]$offset, [single]$offset,
            [single]$extent, [single]$extent))
        $maskedGraphics.Clear([Drawing.Color]::Transparent)
        $mask = New-PreviewMask ([single]$side) $Circle
        $texture = [Drawing.TextureBrush]::new($layer)
        $maskedGraphics.FillPath($texture, $mask)
        $resultGraphics.Clear([Drawing.Color]::Transparent)
        $resultGraphics.DrawImage($masked, [Drawing.Rectangle]::new(0, 0, $Size, $Size))
        return $result
    } catch { $result.Dispose(); throw }
    finally {
        if ($texture) { $texture.Dispose() }
        if ($mask) { $mask.Dispose() }
        $resultGraphics.Dispose(); $maskedGraphics.Dispose(); $masked.Dispose()
        $layerGraphics.Dispose(); $layer.Dispose()
    }
}

function Get-AlphaBounds([Drawing.Bitmap]$Bitmap, [double]$VisibleScale) {
    if (-not [Drawing.Image]::IsAlphaPixelFormat($Bitmap.PixelFormat)) { throw 'Foreground must contain a real alpha channel.' }
    $rectangle = [Drawing.Rectangle]::new(0, 0, $Bitmap.Width, $Bitmap.Height)
    $locked = $Bitmap.LockBits($rectangle, [Drawing.Imaging.ImageLockMode]::ReadOnly, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $pixels = [byte[]]::new($Bitmap.Width * $Bitmap.Height * 4)
    try {
        for ($y = 0; $y -lt $Bitmap.Height; $y++) {
            [Runtime.InteropServices.Marshal]::Copy([IntPtr]::Add($locked.Scan0, $y * $locked.Stride),
                $pixels, $y * $Bitmap.Width * 4, $Bitmap.Width * 4)
        }
    } finally { $Bitmap.UnlockBits($locked) }
    $minX = $Bitmap.Width; $minY = $Bitmap.Height; $maxX = -1; $maxY = -1; $alphaPixels = 0
    $opaqueMinX = $Bitmap.Width; $opaqueMinY = $Bitmap.Height; $opaqueMaxX = -1; $opaqueMaxY = -1
    $subjectMinX = $Bitmap.Width; $subjectMinY = $Bitmap.Height; $subjectMaxX = -1; $subjectMaxY = -1
    $circleClippedAlpha = 0; $circleClippedSubject = 0; $circleClippedOpaque = 0; $maxClippedAlpha = 0
    $xSquared = [double[]]::new($Bitmap.Width)
    for ($x = 0; $x -lt $Bitmap.Width; $x++) {
        $distance = (($x + 0.5) / $Bitmap.Width - 0.5) * $VisibleScale
        $xSquared[$x] = $distance * $distance
    }
    for ($y = 0; $y -lt $Bitmap.Height; $y++) {
        $row = $y * $Bitmap.Width * 4
        $yDistance = (($y + 0.5) / $Bitmap.Height - 0.5) * $VisibleScale
        $ySquared = $yDistance * $yDistance
        for ($x = 0; $x -lt $Bitmap.Width; $x++) {
            $alpha = $pixels[$row + $x * 4 + 3]
            if ($alpha -eq 0) { continue }
            $alphaPixels++
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
            if ($alpha -ge 128) {
                if ($x -lt $opaqueMinX) { $opaqueMinX = $x }
                if ($x -gt $opaqueMaxX) { $opaqueMaxX = $x }
                if ($y -lt $opaqueMinY) { $opaqueMinY = $y }
                if ($y -gt $opaqueMaxY) { $opaqueMaxY = $y }
            }
            if ($alpha -ge 32) {
                if ($x -lt $subjectMinX) { $subjectMinX = $x }
                if ($x -gt $subjectMaxX) { $subjectMaxX = $x }
                if ($y -lt $subjectMinY) { $subjectMinY = $y }
                if ($y -gt $subjectMaxY) { $subjectMaxY = $y }
            }
            if ($xSquared[$x] + $ySquared -gt 0.25) {
                $circleClippedAlpha++
                if ($alpha -ge 32) { $circleClippedSubject++ }
                if ($alpha -ge 128) { $circleClippedOpaque++ }
                if ($alpha -gt $maxClippedAlpha) { $maxClippedAlpha = $alpha }
            }
        }
    }
    if ($maxX -lt 0) { throw 'Foreground is entirely transparent.' }
    if ($opaqueMaxX -lt 0) { throw 'Foreground has no alpha >= 128 subject.' }
    return [PSCustomObject]@{
        Left = $minX; Top = $minY; Right = $maxX; Bottom = $maxY; Count = $alphaPixels
        OpaqueLeft = $opaqueMinX; OpaqueTop = $opaqueMinY; OpaqueRight = $opaqueMaxX; OpaqueBottom = $opaqueMaxY
        SubjectLeft = $subjectMinX; SubjectTop = $subjectMinY; SubjectRight = $subjectMaxX; SubjectBottom = $subjectMaxY
        CircleClippedNonzeroAlphaPixels = $circleClippedAlpha; CircleClippedAlpha128Pixels = $circleClippedOpaque
        CircleClippedAlpha32Pixels = $circleClippedSubject
        MaximumClippedAlpha = $maxClippedAlpha
    }
}

function Draw-CenteredLabel([Drawing.Graphics]$Graphics, [string]$Text, [Drawing.Font]$Font,
    [Drawing.Brush]$Brush, [single]$CenterX, [single]$Y) {
    $format = [Drawing.StringFormat]::new()
    try {
        $format.Alignment = [Drawing.StringAlignment]::Center
        $Graphics.DrawString($Text, $Font, $Brush, [Drawing.PointF]::new($CenterX, $Y), $format)
    } finally { $format.Dispose() }
}

$foreground = [Drawing.Bitmap]::new($foregroundPath)
$legacy = [Drawing.Bitmap]::new($legacyPath)
$font = [Drawing.Font]::new('Segoe UI', 10)
$titleFont = [Drawing.Font]::new('Segoe UI', 12, [Drawing.FontStyle]::Bold)
$labelFont = [Drawing.Font]::new('Segoe UI', 10, [Drawing.FontStyle]::Bold)
$sheetPath = Join-Path $PSScriptRoot 'small-size-layer-comparison.png'
$singlePath = Join-Path $PSScriptRoot 'new-icon-128px-desktop-simulation.png'
$comparisonPath = Join-Path $PSScriptRoot 'before-after-192px-desktop-simulation.png'
$geometryPath = Join-Path $PSScriptRoot 'geometry.json'
$light = [Drawing.ColorTranslator]::FromHtml('#F6F5F1')
$dark = [Drawing.ColorTranslator]::FromHtml('#171B18')
try {
    $visibleScale = (108.0 / 72.0) * (1 - 2 * $foregroundInset)
    $bounds = Get-AlphaBounds $foreground $visibleScale
    $visibleWidth = ($bounds.OpaqueRight - $bounds.OpaqueLeft + 1) / $foreground.Width * $visibleScale
    $visibleHeight = ($bounds.OpaqueBottom - $bounds.OpaqueTop + 1) / $foreground.Height * $visibleScale
    $subjectWidth = ($bounds.SubjectRight - $bounds.SubjectLeft + 1) / $foreground.Width * $visibleScale
    $subjectHeight = ($bounds.SubjectBottom - $bounds.SubjectTop + 1) / $foreground.Height * $visibleScale
    # If the entire axis-aligned alpha rectangle fits the circle, every alpha pixel fits it.
    $maxDx = [Math]::Max([Math]::Abs(($bounds.Left / $foreground.Width - 0.5) * $visibleScale),
        [Math]::Abs((($bounds.Right + 1) / $foreground.Width - 0.5) * $visibleScale))
    $maxDy = [Math]::Max([Math]::Abs(($bounds.Top / $foreground.Height - 0.5) * $visibleScale),
        [Math]::Abs((($bounds.Bottom + 1) / $foreground.Height - 0.5) * $visibleScale))
    $circleRetainsBounds = $maxDx * $maxDx + $maxDy * $maxDy -le 0.25
    $roundedRadius = 0.23
    $roundedDx = [Math]::Max(0, $maxDx - (0.5 - $roundedRadius))
    $roundedDy = [Math]::Max(0, $maxDy - (0.5 - $roundedRadius))
    $roundedRetainsBounds = $maxDx -le 0.5 -and $maxDy -le 0.5 -and
        $roundedDx * $roundedDx + $roundedDy * $roundedDy -le $roundedRadius * $roundedRadius

    $sheet = [Drawing.Bitmap]::new(1060, 1438)
    $graphics = [Drawing.Graphics]::FromImage($sheet)
    try {
        Set-PreviewQuality $graphics
        $graphics.Clear($light)
        $graphics.DrawString('LITHIC | original scale, separated adaptive layers', $titleFont, [Drawing.Brushes]::Black, 20, 16)
        $graphics.DrawString('DESKTOP SIMULATION ONLY | actual Android launcher / splash NOT_RUN', $font, [Drawing.Brushes]::DimGray, 20, 43)
        $sizes = @(32, 48, 64, 96, 128)
        $centers = @(300, 430, 570, 730, 925)
        for ($scenario = 0; $scenario -lt 4; $scenario++) {
            $circle = $scenario % 2 -eq 1
            $darkDesktop = $scenario -ge 2
            for ($version = 0; $version -lt 2; $version++) {
                $top = 82 + ($scenario * 2 + $version) * 158
                $rowColor = if ($darkDesktop) { $dark } else { $light }
                $rowInk = if ($darkDesktop) { [Drawing.Brushes]::WhiteSmoke } else { [Drawing.Brushes]::DimGray }
                $rowBrush = [Drawing.SolidBrush]::new($rowColor)
                try { $graphics.FillRectangle($rowBrush, 0, $top, 1060, 158) } finally { $rowBrush.Dispose() }
                $versionLabel = if ($version -eq 0) { 'Before | baked tile' } else { 'After | separate layers' }
                $maskLabel = if ($circle) { 'Circle mask' } else { 'Rounded-square mask' }
                $desktopLabel = if ($darkDesktop) { 'Dark desktop' } else { 'Light desktop' }
                $graphics.DrawString($versionLabel, $labelFont, $rowInk, 20, $top + 42)
                $graphics.DrawString($maskLabel, $font, $rowInk, 20, $top + 65)
                $graphics.DrawString($desktopLabel, $font, $rowInk, 20, $top + 86)
                for ($i = 0; $i -lt $sizes.Count; $i++) {
                    $size = $sizes[$i]
                    $preview = if ($version -eq 0) { New-LayeredPreview $legacy (1.0 / 6.0) ([Drawing.Color]::Black) $size $circle }
                        else { New-LayeredPreview $foreground $foregroundInset $backgroundColor $size $circle }
                    try { $graphics.DrawImageUnscaled($preview, [int]($centers[$i] - $size / 2), [int]($top + 70 - $size / 2)) }
                    finally { $preview.Dispose() }
                    Draw-CenteredLabel $graphics "$($size)px" $font $rowInk $centers[$i] ($top + 136)
                }
            }
        }
        $graphics.DrawString(('Subject (alpha >= 32): {0:P1} width / {1:P1} height | XML inset {2}' -f $subjectWidth, $subjectHeight, $insetText),
            $font, [Drawing.Brushes]::DimGray, 20, 1364)
        $graphics.DrawString(('Circle: {0} clipped alpha >= 32 pixels; {1} exterior alpha <= {2} pixels | 108/72 desktop only.' -f
            $bounds.CircleClippedAlpha32Pixels, $bounds.CircleClippedNonzeroAlphaPixels, $bounds.MaximumClippedAlpha),
            $font, [Drawing.Brushes]::DimGray, 20, 1391)
        $sheet.Save($sheetPath, [Drawing.Imaging.ImageFormat]::Png)
    } finally { $graphics.Dispose(); $sheet.Dispose() }

    # Keep the icon itself at exactly 128px, with provenance outside its pixels.
    $single = [Drawing.Bitmap]::new(284, 232)
    $graphics = [Drawing.Graphics]::FromImage($single)
    $icon = New-LayeredPreview $foreground $foregroundInset $backgroundColor 128 $false
    try {
        Set-PreviewQuality $graphics
        $graphics.Clear($light)
        Draw-CenteredLabel $graphics 'New | 128px icon' $titleFont ([Drawing.Brushes]::Black) 142 13
        $graphics.DrawImageUnscaled($icon, 78, 52)
        Draw-CenteredLabel $graphics 'DESKTOP SIMULATION ONLY' $font ([Drawing.Brushes]::DimGray) 142 196
        $single.Save($singlePath, [Drawing.Imaging.ImageFormat]::Png)
    } finally { $icon.Dispose(); $graphics.Dispose(); $single.Dispose() }

    $comparison = [Drawing.Bitmap]::new(700, 320)
    $graphics = [Drawing.Graphics]::FromImage($comparison)
    $oldIcon = New-LayeredPreview $legacy (1.0 / 6.0) ([Drawing.Color]::Black) 192 $false
    $newIcon = New-LayeredPreview $foreground $foregroundInset $backgroundColor 192 $false
    try {
        Set-PreviewQuality $graphics
        $graphics.Clear($light)
        Draw-CenteredLabel $graphics 'Before | 192px' $titleFont ([Drawing.Brushes]::Black) 192 26
        Draw-CenteredLabel $graphics 'After | 192px' $titleFont ([Drawing.Brushes]::Black) 508 26
        $graphics.DrawImageUnscaled($oldIcon, 96, 67)
        $graphics.DrawImageUnscaled($newIcon, 412, 67)
        Draw-CenteredLabel $graphics 'DESKTOP SIMULATION ONLY | Android NOT_RUN' $font ([Drawing.Brushes]::DimGray) 350 286
        $comparison.Save($comparisonPath, [Drawing.Imaging.ImageFormat]::Png)
    } finally { $newIcon.Dispose(); $oldIcon.Dispose(); $graphics.Dispose(); $comparison.Dispose() }

    $geometry = [PSCustomObject]@{
        Evidence = 'DESKTOP_SIMULATION_ONLY'; Android = 'NOT_RUN'
        Model = 'Static 108/72 expansion, XML inset, opaque full-bleed background, then mask. Not an OEM/runtime proof.'
        Foreground = [IO.Path]::GetRelativePath($iconRoot, $foregroundPath)
        ForegroundSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $foregroundPath).Hash.ToLowerInvariant()
        ForegroundSize = "$($foreground.Width)x$($foreground.Height)"
        NonzeroAlphaEnvelopeInclusive = "$($bounds.Left),$($bounds.Top)..$($bounds.Right),$($bounds.Bottom)"
        Alpha32SubjectBoundsInclusive = "$($bounds.SubjectLeft),$($bounds.SubjectTop)..$($bounds.SubjectRight),$($bounds.SubjectBottom)"
        Alpha128BoundsInclusive = "$($bounds.OpaqueLeft),$($bounds.OpaqueTop)..$($bounds.OpaqueRight),$($bounds.OpaqueBottom)"
        NonzeroAlphaPixels = $bounds.Count; Inset = $insetText; Background = $backgroundText
        VisibleAlpha32SubjectWidthPercent = [Math]::Round($subjectWidth * 100, 2)
        VisibleAlpha32SubjectHeightPercent = [Math]::Round($subjectHeight * 100, 2)
        VisibleAlpha128WidthPercent = [Math]::Round($visibleWidth * 100, 2)
        VisibleAlpha128HeightPercent = [Math]::Round($visibleHeight * 100, 2)
        StaticCircleRetainsEntireAlphaBounds = $circleRetainsBounds
        StaticCircleClippedNonzeroAlphaPixels = $bounds.CircleClippedNonzeroAlphaPixels
        StaticCircleClippedAlpha32Pixels = $bounds.CircleClippedAlpha32Pixels
        StaticCircleClippedAlpha128Pixels = $bounds.CircleClippedAlpha128Pixels
        MaximumClippedAlpha = $bounds.MaximumClippedAlpha
        StaticRoundedSquareRetainsEntireAlphaBounds = $roundedRetainsBounds
        Files = @($sheetPath, $singlePath, $comparisonPath) | ForEach-Object { [IO.Path]::GetRelativePath($iconRoot, $_) }
    }
    $geometryJson = $geometry | ConvertTo-Json -Depth 3
    [IO.File]::WriteAllText($geometryPath, $geometryJson + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))
    Write-Output $geometryJson
    Write-Output $geometryPath
} finally {
    $labelFont.Dispose(); $titleFont.Dispose(); $font.Dispose(); $legacy.Dispose(); $foreground.Dispose()
}
