@ECHO OFF
SETLOCAL
SET VERSION=3.0
SET VR=30
SET OSD=windows
SET DEV=cpu
SET nn_type=0

REM --------- Nvidia GPUS
WHERE nvcuda.dll >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
  SET GPUS=0
  SET DEV=cpu
  SET nn_type=0
  SET /a mt=%NUMBER_OF_PROCESSORS%*4
) ELSE (
  SET GPUS=1
  SET DEV=gpu
  SET nn_type=1
  SET mt=128
)

SET EGBB=nnprobe-%OSD%-%DEV%
SET LNK=http://github.com/dshawul/Scorpio/releases/download

REM --------- create directory
SET SCORPIO=Scorpio-train
mkdir %SCORPIO%
cd %SCORPIO%
SET CWD=%cd%\

REM --------- download nnprobe
SET FILENAME=%EGBB%.zip
bitsadmin /transfer mydownload /dynamic /download /priority FOREGROUND "%LNK%/%VERSION%/%FILENAME%" %CWD%%FILENAME%
powershell Expand-Archive %CWD%%FILENAME% -DestinationPath %CWD%
DEL %CWD%%FILENAME%

REM --------- download scorpio binary
SET FILENAME=scorpio%VR%-mcts-nn.zip
bitsadmin /transfer mydownload /dynamic /download /priority FOREGROUND "%LNK%/%VERSION%/%FILENAME%" %CWD%%FILENAME%
powershell Expand-Archive %CWD%%FILENAME% -DestinationPath %CWD%
DEL %CWD%%FILENAME%

REM --------- download networks
for %%N in ( nets-scorpio.zip nets-lczero.zip nets-maddex.zip ) DO (
    bitsadmin /transfer mydownload /dynamic /download /priority FOREGROUND "%LNK%/%VERSION%/%%N" %CWD%%%N
    powershell Expand-Archive %CWD%%%N -DestinationPath %CWD%
    DEL %CWD%%%N
)

cd %EGBB%
icacls "*.*" /grant %USERNAME%:F
cd ..

REM ---------- paths
SET egbbp=%CWD%%EGBB%
SET EXE="%CWD%bin/Windows/scorpio.bat"

IF %nn_type% NEQ 0 (
  SET nnp=%CWD%nets-maddex/net-maddex.uff
) ELSE (
  SET nnp=%CWD%nets-scorpio/net-6x64.pb
)

REM ---------- edit scorpio.ini
cd "%CWD%bin/Windows"
SETLOCAL ENABLEDELAYEDEXPANSION
IF EXIST output.txt DEL /F output.txt
for /F "delims=" %%A in (scorpio.ini) do (
   SET LMN=%%A
   IF /i "!LMN:~0,9!"=="egbb_path" (
     echo egbb_path                %egbbp%>> output.txt
   ) ELSE IF /i "!LMN:~0,7!"=="nn_path" (
     echo nn_path                  %nnp%>> output.txt
   ) ELSE IF /i "!LMN:~0,7!"=="nn_type" (
     echo nn_type                  %nn_type% >> output.txt
   ) ELSE IF /i "!LMN:~0,10!"=="float_type" (
     echo float_type                  HALF >> output.txt
   ) ELSE IF /i "!LMN:~0,2!"=="mt" (
     echo mt                  %mt% >> output.txt
   ) ELSE IF /i "!LMN:~0,11!"=="device_type" (
     IF %nn_type% NEQ 0 (
        echo device_type              GPU>> output.txt
     ) ELSE (
        echo device_type              CPU>> output.txt
     )
   ) ELSE IF /i "!LMN:~0,9!"=="n_devices" (
     IF %nn_type% NEQ 0 (
        echo n_devices                %GPUS% >> output.txt
     ) ELSE (
        echo n_devices                1 >> output.txt
     )
   ) ELSE (
     echo %%A>> output.txt
   )
)
MOVE output.txt scorpio.ini
cd ../..

REM ---------- test
CALL %EXE% go quit
