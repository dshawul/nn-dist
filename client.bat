@ECHO OFF

IF EXIST Scorpio-train/bin/Windows/scorpio.bat (
  ECHO "Scorpio is already installed"
) ELSE (
  CALL %~dp0scripts/install.bat
)

java -cp bin ConsoleInterface -debug -startClient
