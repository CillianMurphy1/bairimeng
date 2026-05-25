$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Add-Type -AssemblyName System.Drawing

function New-LauncherIcon($Size, $OutPath) {
    $bitmap = New-Object System.Drawing.Bitmap($Size, $Size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    $rect = New-Object System.Drawing.Rectangle(0, 0, $Size, $Size)
    $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect,
        [System.Drawing.Color]::FromArgb(255, 207, 238, 235),
        [System.Drawing.Color]::FromArgb(255, 244, 250, 231),
        22
    )
    $graphics.FillRectangle($bg, $rect)

    $mist1 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(76, 255, 255, 255))
    $mist2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(48, 124, 205, 190))
    for ($i = 0; $i -lt 9; $i++) {
        $x = [int]($Size * ($i / 8.0))
        $graphics.FillRectangle($mist1, $x, 0, [int]($Size * 0.07), $Size)
    }
    $graphics.FillEllipse($mist2, [int]($Size * -0.12), [int]($Size * 0.67), [int]($Size * 0.72), [int]($Size * 0.24))

    $body = New-Object System.Drawing.Drawing2D.GraphicsPath
    $body.AddBezier(
        [int]($Size * 0.38), [int]($Size * 0.48),
        [int]($Size * 0.54), [int]($Size * 0.43),
        [int]($Size * 0.72), [int]($Size * 0.60),
        [int]($Size * 0.74), [int]($Size * 0.95)
    )
    $body.AddLine([int]($Size * 0.28), [int]($Size * 0.98), [int]($Size * 0.35), [int]($Size * 0.55))
    $body.CloseFigure()
    $bodyBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(120, 79, 138, 132))
    $graphics.FillPath($bodyBrush, $body)

    $hairBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(150, 50, 71, 67))
    $skinBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(108, 214, 178, 149))
    $graphics.FillEllipse($hairBrush, [int]($Size * 0.42), [int]($Size * 0.20), [int]($Size * 0.27), [int]($Size * 0.24))
    $graphics.FillEllipse($hairBrush, [int]($Size * 0.37), [int]($Size * 0.29), [int]($Size * 0.16), [int]($Size * 0.14))
    $graphics.FillEllipse($skinBrush, [int]($Size * 0.55), [int]($Size * 0.29), [int]($Size * 0.17), [int]($Size * 0.22))

    $ribbonPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(210, 196, 67, 55), [Math]::Max(4, [int]($Size * 0.035)))
    $ribbonPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $ribbonPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $graphics.DrawBezier($ribbonPen,
        [int]($Size * 0.40), [int]($Size * 0.36),
        [int]($Size * 0.30), [int]($Size * 0.31),
        [int]($Size * 0.29), [int]($Size * 0.43),
        [int]($Size * 0.42), [int]($Size * 0.39)
    )
    $graphics.DrawLine($ribbonPen, [int]($Size * 0.42), [int]($Size * 0.39), [int]($Size * 0.34), [int]($Size * 0.86))
    $graphics.DrawLine($ribbonPen, [int]($Size * 0.43), [int]($Size * 0.40), [int]($Size * 0.48), [int]($Size * 0.86))

    $veil = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(118, 207, 238, 235))
    $graphics.FillRectangle($veil, 0, 0, $Size, $Size)

    $dir = Split-Path $OutPath -Parent
    New-Item -ItemType Directory -Force $dir | Out-Null
    $bitmap.Save($OutPath, [System.Drawing.Imaging.ImageFormat]::Png)

    $graphics.Dispose()
    $bitmap.Dispose()
}

$icons = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($entry in $icons.GetEnumerator()) {
    $dir = Join-Path $Root "app\src\main\res\$($entry.Key)"
    New-LauncherIcon $entry.Value (Join-Path $dir "ic_launcher.png")
    New-LauncherIcon $entry.Value (Join-Path $dir "ic_launcher_round.png")
}
