# Desktop mask/size simulation only. Does not edit the original artwork or use a device.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$iconRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../..')).Path
$iconArt = Join-Path $iconRoot 'app/src/main/res/drawable-nodpi/ic_launcher_art.png'
$iconOutput = Join-Path $PSScriptRoot 'small-size-preview.png'
$iconSource = [System.Drawing.Image]::FromFile($iconArt)
$iconSheet = [System.Drawing.Bitmap]::new(680, 724)
$iconGraphics = [System.Drawing.Graphics]::FromImage($iconSheet)
$iconFont = [System.Drawing.Font]::new('Segoe UI', 10)
$iconTitleFont = [System.Drawing.Font]::new('Segoe UI', 12, [System.Drawing.FontStyle]::Bold)
$iconGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$iconGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$iconGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$iconGraphics.Clear([System.Drawing.Color]::FromArgb(246,245,241))
$iconGraphics.DrawString('LITHIC | original artwork, no zoom', $iconTitleFont, [System.Drawing.Brushes]::Black, 20, 14)
$iconGraphics.DrawString('DESKTOP SIMULATION ONLY | not an Android screenshot', $iconFont, [System.Drawing.Brushes]::DimGray, 20, 40)
$iconSizes = @(32,48,64,96,128)
$iconCenters = @(175,260,350,455,590)
for ($iconRow=0; $iconRow -lt 4; $iconRow++) {
    $iconTop = 74 + $iconRow * 160
    $iconDark = $iconRow -ge 2
    $iconCircle = $iconRow % 2 -eq 1
    $iconBack = if ($iconDark) { [System.Drawing.Color]::FromArgb(25,28,27) } else { [System.Drawing.Color]::FromArgb(246,245,241) }
    $iconInk = if ($iconDark) { [System.Drawing.Brushes]::WhiteSmoke } else { [System.Drawing.Brushes]::DimGray }
    $iconBrush = [System.Drawing.SolidBrush]::new($iconBack)
    $iconGraphics.FillRectangle($iconBrush, 0, $iconTop, 680, 160)
    $iconLabel = if ($iconCircle) { 'Circle mask' } else { 'Rounded mask' }
    $iconGraphics.DrawString($iconLabel, $iconFont, $iconInk, 20, $iconTop+32)
    for ($iconIndex=0; $iconIndex -lt $iconSizes.Count; $iconIndex++) {
        $iconSize = $iconSizes[$iconIndex]
        $iconVisibleSize = $iconSize
        $iconX = $iconCenters[$iconIndex] - $iconVisibleSize/2
        $iconY = $iconTop + (128-$iconVisibleSize)/2
        $iconPath = [System.Drawing.Drawing2D.GraphicsPath]::new()
        if ($iconCircle) {
            $iconPath.AddEllipse([single]$iconX,[single]$iconY,[single]$iconVisibleSize,[single]$iconVisibleSize)
        } else {
            $iconArc = [single]($iconVisibleSize * 0.46)
            $iconPath.AddArc([single]$iconX,[single]$iconY,$iconArc,$iconArc,180,90)
            $iconPath.AddArc([single]($iconX+$iconVisibleSize-$iconArc),[single]$iconY,$iconArc,$iconArc,270,90)
            $iconPath.AddArc([single]($iconX+$iconVisibleSize-$iconArc),[single]($iconY+$iconVisibleSize-$iconArc),$iconArc,$iconArc,0,90)
            $iconPath.AddArc([single]$iconX,[single]($iconY+$iconVisibleSize-$iconArc),$iconArc,$iconArc,90,90)
            $iconPath.CloseFigure()
        }
        $iconState = $iconGraphics.Save()
        $iconGraphics.SetClip($iconPath)
        # Foreground inset cancels the adaptive 108/72 static viewport expansion.
        $iconGraphics.DrawImage($iconSource, [System.Drawing.RectangleF]::new($iconX,$iconY,$iconVisibleSize,$iconVisibleSize))
        $iconGraphics.Restore($iconState)
        $iconGraphics.DrawString("$($iconSize)px", $iconFont, $iconInk, [single]($iconX-2), [single]($iconTop+132))
        $iconPath.Dispose()
    }
    $iconBrush.Dispose()
}
$iconSheet.Save($iconOutput, [System.Drawing.Imaging.ImageFormat]::Png)
$iconTile = $iconSheet.Clone([System.Drawing.Rectangle]::new(318,106,64,64), [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$iconTile.Save((Join-Path $PSScriptRoot 'icon-at-64px.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$iconTile.Dispose()
$iconTitleFont.Dispose()
$iconFont.Dispose()
$iconGraphics.Dispose()
$iconSheet.Dispose()
$iconSource.Dispose()
Write-Output $iconOutput
