; Instalador de Mapamundi para Windows (Inno Setup 6).
; Lo compila empaquetar.ps1 -Publicar a partir de target\instalador\Mapamundi (jpackage).
; Se instala por usuario, sin permisos de administrador.

#ifndef Version
  #define Version "2.2.0"
#endif

[Setup]
AppId={{6F2C3B1E-8A47-4E0D-9C1B-5D3A7E2F9B41}
AppName=Mapamundi
AppVersion={#Version}
AppVerName=Mapamundi {#Version}
AppPublisher=guess-samuel2401
AppPublisherURL=https://github.com/guess-samuel2401/mapamundi
AppSupportURL=https://github.com/guess-samuel2401/mapamundi/issues
AppUpdatesURL=https://github.com/guess-samuel2401/mapamundi/releases
DefaultDirName={autopf}\Mapamundi
DefaultGroupName=Mapamundi
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
SetupIconFile=mapamundi.ico
UninstallDisplayIcon={app}\Mapamundi.exe
UninstallDisplayName=Mapamundi
WizardStyle=modern
Compression=lzma2/ultra64
SolidCompression=yes
OutputDir=..\target\publicar
OutputBaseFilename=Mapamundi-{#Version}-instalador

[Languages]
Name: "es"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "escritorio"; Description: "Crear un acceso directo en el Escritorio"; GroupDescription: "Accesos directos:"

[Files]
Source: "..\target\instalador\Mapamundi\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\Mapamundi"; Filename: "{app}\Mapamundi.exe"; Comment: "Juego de geografía"
Name: "{autodesktop}\Mapamundi"; Filename: "{app}\Mapamundi.exe"; Comment: "Juego de geografía"; Tasks: escritorio

[Run]
Filename: "{app}\Mapamundi.exe"; Description: "Jugar a Mapamundi ahora"; Flags: nowait postinstall skipifsilent
