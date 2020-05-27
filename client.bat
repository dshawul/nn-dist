@ECHO OFF

IF EXIST Scorpio/bin/Windows/scorpio.bat (
  ECHO "Scorpio is already installed"
) ELSE (
  CALL %~dp0scripts/install.bat --no-egbb --no-lcnets --precision INT8
)

java -cp bin ConsoleInterface -debug -startClient
