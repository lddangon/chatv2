Add-Type -AssemblyName System.Drawing
$bitmap = New-Object System.Drawing.Bitmap 32, 32
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::FromArgb(40, 40, 60))
$brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(233, 69, 96))
$font = New-Object System.Drawing.Font('Segoe UI', 14, [System.Drawing.FontStyle]::Bold)
$graphics.DrawString('C', $font, $brush, 6, 6)
$bitmap.Save('D:\code\Chatv2\chat-client\src\main\resources\images\chat-icon.png', [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$bitmap.Dispose()
Write-Host "Icon created successfully"
