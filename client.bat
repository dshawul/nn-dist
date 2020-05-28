@ECHO OFF

WHERE nvcuda.dll >nul 2>nul
IF %ERRORLEVEL% NEQ 0 ( 
  ECHO "No GPUs detected. Please try again when you have one. Thanks!"
  EXIT /B
)

IF EXIST Scorpio/bin/Windows/scorpio.bat (
  ECHO "Scorpio is already installed"
) ELSE (
  CALL %~dp0scripts/install.bat --no-egbb --no-lcnets --precision INT8
)

java -cp bin ConsoleInterface -debug -startClient
