# Desktop adaptive-layer simulation only. No production raster edits, Android or SDK calls.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../..')).Path
$scenePath = Join-Path $root 'app/src/main/res/drawable-nodpi/ic_launcher_scene_p3.png'
$originalPath = Join-Path $root 'app/src/main/res/drawable-nodpi/ic_launcher_art.png'
$cutoutPath = Join-Path $root 'app/src/main/res/drawable-nodpi/ic_launcher_stone.png'
$namespace = 'http://schemas.android.com/apk/res/android'
[xml]$foregroundXml = Get-Content -Raw (Join-Path $root 'app/src/main/res/drawable/ic_launcher_foreground.xml')
[xml]$backgroundXml = Get-Content -Raw (Join-Path $root 'app/src/main/res/drawable/ic_launcher_background.xml')
$inset = $foregroundXml.inset.GetAttribute('inset', $namespace)
if ($inset -ne '16.666667%' -or $foregroundXml.inset.bitmap.GetAttribute('src', $namespace) -ne '@drawable/ic_launcher_scene_p3') { throw 'P3 source scene/inset mismatch.' }
$background = $backgroundXml.shape.solid.GetAttribute('color', $namespace)
if ($background -ne '#23241F') { throw 'P3 overscan surface mismatch.' }

function Set-Quality([Drawing.Graphics]$Graphics) {
    $Graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $Graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $Graphics.CompositingQuality = [Drawing.Drawing2D.CompositingQuality]::HighQuality
    $Graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
}
function New-Icon([Drawing.Bitmap]$Art, [double]$Inset, [string]$Color, [int]$Size, [bool]$Circle) {
    $side = $Size * 4
    $image = [Drawing.Bitmap]::new($side, $side, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [Drawing.Graphics]::FromImage($image)
    $path = [Drawing.Drawing2D.GraphicsPath]::new()
    $result = [Drawing.Bitmap]::new($Size, $Size, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $target = [Drawing.Graphics]::FromImage($result)
    try {
        Set-Quality $graphics; Set-Quality $target
        if ($Circle) { $path.AddEllipse(0, 0, $side, $side) }
        else {
            $arc = [single]($side * 0.46)
            $path.AddArc(0, 0, $arc, $arc, 180, 90)
            $path.AddArc(($side - $arc), 0, $arc, $arc, 270, 90)
            $path.AddArc(($side - $arc), ($side - $arc), $arc, $arc, 0, 90)
            $path.AddArc(0, ($side - $arc), $arc, $arc, 90, 90); $path.CloseFigure()
        }
        $graphics.Clear([Drawing.Color]::Transparent)
        $graphics.SetClip($path)
        $brush = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($Color))
        try { $graphics.FillRectangle($brush, 0, 0, $side, $side) } finally { $brush.Dispose() }
        # Explicit static 108/72 approximation. Never an OEM mask or motion claim.
        $extent = $side * 108.0 / 72.0 * (1 - 2 * $Inset)
        $offset = ($side - $extent) / 2
        $graphics.DrawImage($Art, [Drawing.RectangleF]::new($offset, $offset, $extent, $extent))
        $target.DrawImage($image, [Drawing.Rectangle]::new(0, 0, $Size, $Size))
        return $result
    } catch { $result.Dispose(); throw }
    finally { $graphics.Dispose(); $target.Dispose(); $path.Dispose(); $image.Dispose() }
}

$original = [Drawing.Bitmap]::FromFile($originalPath)
$cutout = [Drawing.Bitmap]::FromFile($cutoutPath)
$scene = [Drawing.Bitmap]::FromFile($scenePath)
$sheet = [Drawing.Bitmap]::new(990, 800)
$graphics = [Drawing.Graphics]::FromImage($sheet)
$title = [Drawing.Font]::new('Segoe UI', 14, [Drawing.FontStyle]::Bold)
$font = [Drawing.Font]::new('Segoe UI', 10)
$muted = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#596A61'))
$darkBrush = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#17221D'))
$labels = @('Original | baked frame', 'Preview13 | flat cutout', 'P3 | restored scene')
$sources = @($original, $cutout, $scene)
$insets = @((1.0 / 6), 0.23, (1.0 / 6))
$colors = @('#000000', '#282B28', $background)
try {
    Set-Quality $graphics
    $graphics.Clear([Drawing.ColorTranslator]::FromHtml('#F6F5F1'))
    $graphics.DrawString('LITHIC | original atmosphere, no baked border', $title, $darkBrush, 22, 14)
    $graphics.DrawString('DESKTOP SIMULATION ONLY | Android launcher, motion and actual scale NOT_RUN', $font, $muted, 22, 43)
    $dark = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#151C18'))
    try { $graphics.FillRectangle($dark, 0, 348, 990, 242) } finally { $dark.Dispose() }
    for ($column = 0; $column -lt 3; $column++) {
        $left = 22 + $column * 330
        $center = 165 + $column * 330
        $graphics.DrawString($labels[$column], $title, $darkBrush, $left, 81)
        $rounded = New-Icon $sources[$column] $insets[$column] $colors[$column] 192 $false
        $circle = New-Icon $sources[$column] $insets[$column] $colors[$column] 128 $true
        try {
            $graphics.DrawImageUnscaled($rounded, ($center - 96), 118)
            $graphics.DrawString('192px | rounded square | light desktop', $font, $muted, $left, 319)
            $graphics.DrawImageUnscaled($circle, ($center - 64), 381)
            $graphics.DrawString('128px | circle | dark desktop', $font, [Drawing.Brushes]::WhiteSmoke, $left, 531)
        } finally { $rounded.Dispose(); $circle.Dispose() }
        $x = $left + 14
        foreach ($size in @(32, 48, 64)) {
            $small = New-Icon $sources[$column] $insets[$column] $colors[$column] $size $false
            try { $graphics.DrawImageUnscaled($small, $x, (648 - $size / 2)) } finally { $small.Dispose() }
            $graphics.DrawString("$($size)px", $font, $muted, $x, 690)
            $x += 92
        }
    }
    $graphics.DrawString('P3 preserves a full opaque scene. Warm ground light and texture continue to every edge.', $font, $muted, 22, 737)
    $graphics.DrawString('Artwork editing derivative; original source unchanged. Static 108/72 model only.', $font, $muted, 22, 765)
    $sheet.Save((Join-Path $PSScriptRoot 'original-p2-p3-desktop-comparison.png'), [Drawing.Imaging.ImageFormat]::Png)
    $metadata = [ordered]@{
        evidence = 'DESKTOP_SIMULATION_ONLY'; androidLauncher = 'NOT_RUN'; model = 'Static adaptive 108/72, actual inset, full opaque scene, then mask'
        original = [ordered]@{ path = 'app/src/main/res/drawable-nodpi/ic_launcher_art.png'; sha256 = (Get-FileHash $originalPath).Hash.ToLowerInvariant() }
        p2 = [ordered]@{ path = 'app/src/main/res/drawable-nodpi/ic_launcher_stone.png'; sha256 = (Get-FileHash $cutoutPath).Hash.ToLowerInvariant() }
        scene = [ordered]@{ path = 'app/src/main/res/drawable-nodpi/ic_launcher_scene_p3.png'; width = $scene.Width; height = $scene.Height; bytes = (Get-Item $scenePath).Length; sha256 = (Get-FileHash $scenePath).Hash.ToLowerInvariant(); pixelFormat = $scene.PixelFormat.ToString(); background = 'OPAQUE_FULL_BLEED_SCENE'; cornerAlpha = @($scene.GetPixel(0,0).A,$scene.GetPixel(1253,0).A,$scene.GetPixel(0,1253).A,$scene.GetPixel(1253,1253).A) }
        insetPercent = 16.666667; overscanBackground = $background; actualPreviewSizesPx = @(32,48,64,128,192)
        subjectScaleEvidence = 'Visual side-by-side; no claim of pixel-identical source extraction or Android scale'
        editedBy = 'built-in image_gen'; promptFile = 'generation-prompt.txt'; productionPixelsEditedByPreview = $false; deviceOperations = 0
    }
    [IO.File]::WriteAllText((Join-Path $PSScriptRoot 'scene-metadata.json'), (($metadata | ConvertTo-Json -Depth 5) -replace "`r`n", "`n") + "`n", [Text.UTF8Encoding]::new($false))
    $metadata | ConvertTo-Json -Depth 5
} finally {
    $darkBrush.Dispose(); $muted.Dispose(); $font.Dispose(); $title.Dispose(); $graphics.Dispose(); $sheet.Dispose()
    $scene.Dispose(); $cutout.Dispose(); $original.Dispose()
}
