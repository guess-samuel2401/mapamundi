; Instalador de Mapamundi Sencillo para Windows (Inno Setup 6). Es un programa aparte de Mapamundi 2.x:
; tiene su propio AppId y su propia carpeta, así que se pueden tener los dos instalados.
; Lo compila empaquetar.ps1 -Publicar a partir de target\instalador\Mapamundi (jpackage).
; Se instala por usuario, sin permisos de administrador.

#ifndef Version
  #define Version "3.2.0"
#endif

[Setup]
AppId={{21D6F91F-9269-4C30-A239-353B12DF0545}
AppName=Mapamundi Sencillo
AppVersion={#Version}
AppVerName=Mapamundi Sencillo {#Version}
AppPublisher=guess-samuel2401
AppPublisherURL=https://github.com/guess-samuel2401/mapamundi
AppSupportURL=https://github.com/guess-samuel2401/mapamundi/issues
AppUpdatesURL=https://github.com/guess-samuel2401/mapamundi/releases
DefaultDirName={autopf}\Mapamundi Sencillo
DefaultGroupName=Mapamundi Sencillo
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
SetupIconFile=mapamundi.ico
UninstallDisplayIcon={app}\Mapamundi.exe
UninstallDisplayName=Mapamundi Sencillo
WizardStyle=modern
Compression=lzma2/ultra64
SolidCompression=yes
OutputDir=..\target\publicar
OutputBaseFilename=Mapamundi-Sencillo-{#Version}-instalador

[Languages]
Name: "es"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "escritorio"; Description: "Crear un acceso directo en el Escritorio"; GroupDescription: "Accesos directos:"

[Files]
Source: "..\target\instalador\Mapamundi\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\Mapamundi Sencillo"; Filename: "{app}\Mapamundi.exe"; Comment: "Juego de geografía"
Name: "{autodesktop}\Mapamundi Sencillo"; Filename: "{app}\Mapamundi.exe"; Comment: "Juego de geografía"; Tasks: escritorio

[Run]
Filename: "{app}\Mapamundi.exe"; Description: "Jugar a Mapamundi Sencillo ahora"; Flags: nowait postinstall skipifsilent
