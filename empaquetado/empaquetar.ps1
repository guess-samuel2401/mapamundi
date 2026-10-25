<#
.SYNOPSIS
    Convierte Mapamundi en un programa de Windows (Mapamundi.exe con su propio Java incluido)
    y, con -Instalar, lo copia a la carpeta de programas del usuario con accesos directos.

    Con -Publicar genera además, en target\publicar, el instalador (Inno Setup) y un ZIP portable
    para subirlos a GitHub Releases.

.EXAMPLE
    .\empaquetado\empaquetar.ps1            # solo genera target\instalador\Mapamundi
    .\empaquetado\empaquetar.ps1 -Instalar  # además lo instala en %LOCALAPPDATA%\Programs\Mapamundi
    .\empaquetado\empaquetar.ps1 -Publicar  # instalador .exe + ZIP portable en target\publicar
#>
param([switch]$Instalar, [switch]$Publicar)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine') }
$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
$version = '3.4.0'

# Una copia abierta del programa empaquetado bloquearía la limpieza de target\.
$abiertos = Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.Path -like (Join-Path $raiz 'target\*') }
if ($abiertos) {
    $abiertos | Stop-Process -Force
    Start-Sleep -Seconds 3
}
# El antivirus suele revisar el .exe recién creado y lo bloquea unos segundos: reintentar el borrado.
$anterior = Join-Path $raiz 'target\instalador'
for ($i = 0; $i -lt 10 -and (Test-Path $anterior); $i++) {
    try { Remove-Item $anterior -Recurse -Force -ErrorAction Stop } catch { Start-Sleep -Seconds 2 }
}

Write-Host '1/3  Compilando y reuniendo las librerías...'
& .\mvnw.cmd -q -B clean package
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación' }

Write-Host '2/3  Creando Mapamundi.exe con jpackage...'
$destino = Join-Path $raiz 'target\instalador'
# Módulos de Java que necesita (jdeps) más los que JavaFX usa por reflexión.
$modulos = 'java.base,java.desktop,java.sql,java.logging,java.xml,jdk.jfr,jdk.unsupported'
& $jpackage `
    --type app-image `
    --name Mapamundi `
    --app-version $version `
    --vendor 'Mapamundi' `
    --description 'Juego de geografía: ubica cada país en el mapa del mundo.' `
    --icon (Join-Path $PSScriptRoot 'mapamundi.ico') `
    --input (Join-Path $raiz 'target\programa') `
    --main-jar mapamundi.jar `
    --main-class com.mapamundi.Lanzador `
    --add-modules $modulos `
    --jlink-options '--strip-debug --no-man-pages --no-header-files' `
    --java-options '--enable-native-access=ALL-UNNAMED' `
    --dest $destino
if ($LASTEXITCODE -ne 0) { throw 'Falló jpackage' }
$app = Join-Path $destino 'Mapamundi'
$mb = [math]::Round((Get-ChildItem $app -Recurse -File | Measure-Object Length -Sum).Sum / 1MB, 1)
Write-Host "     Programa listo en $app ($mb MB)"

if ($Publicar) {
    Write-Host '3/3  Creando el instalador y el ZIP portable...'
    $salida = Join-Path $raiz 'target\publicar'
    New-Item -ItemType Directory -Force $salida | Out-Null
    $iscc = @(
        (Join-Path $env:LOCALAPPDATA 'Programs\Inno Setup 6\ISCC.exe'),
        (Join-Path ${env:ProgramFiles(x86)} 'Inno Setup 6\ISCC.exe'),
        (Join-Path $env:ProgramFiles 'Inno Setup 6\ISCC.exe')
    ) | Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $iscc) { throw 'Falta Inno Setup 6 (winget install JRSoftware.InnoSetup)' }
    & $iscc /Q "/DVersion=$version" (Join-Path $PSScriptRoot 'instalador.iss')
    if ($LASTEXITCODE -ne 0) { throw 'Falló Inno Setup' }
    $zip = Join-Path $salida "Mapamundi-Sencillo-$version-windows-portable.zip"
    if (Test-Path $zip) { Remove-Item $zip }
    Compress-Archive -Path $app -DestinationPath $zip -CompressionLevel Optimal
    Get-ChildItem $salida | ForEach-Object { Write-Host ("     {0}  ({1} MB)" -f $_.Name, [math]::Round($_.Length / 1MB, 1)) }
} elseif ($Instalar) {
    Write-Host '3/3  Instalando para este usuario...'
    $instalado = Join-Path $env:LOCALAPPDATA 'Programs\Mapamundi Sencillo'
    Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "$instalado\*" } | Stop-Process -Force
    if (Test-Path $instalado) { Remove-Item $instalado -Recurse -Force }
    Copy-Item $app $instalado -Recurse
    $exe = Join-Path $instalado 'Mapamundi.exe'

    $shell = New-Object -ComObject WScript.Shell
    $carpetas = @(
        [Environment]::GetFolderPath('Desktop'),
        (Join-Path ([Environment]::GetFolderPath('Programs')) '')
    )
    foreach ($carpeta in $carpetas) {
        $acceso = $shell.CreateShortcut((Join-Path $carpeta 'Mapamundi Sencillo.lnk'))
        $acceso.TargetPath = $exe
        $acceso.WorkingDirectory = $instalado
        $acceso.IconLocation = "$exe,0"
        $acceso.Description = 'Mapamundi: juego de geografía'
        $acceso.Save()
    }
    Write-Host "     Instalado en $instalado"
    Write-Host '     Accesos directos en el Escritorio y en el menú Inicio.'
} else {
    Write-Host '3/3  (sin instalar; usa -Instalar o -Publicar)'
}
