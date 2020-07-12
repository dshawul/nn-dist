@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION

REM setup parameters for selfplay
SET SC=%cd%\Scorpio\bin\Windows
SET EXE=scorpio.bat
SET G=512
SET SCOPT=%*

REM launch multiple jobs with mpi
SET RANKS=1
IF %RANKS% GEQ 2 (
   SET MPICMD="mpiexec -n %RANKS%"
) ELSE (
   SET MPICMD=
)

REM check if Scorpio directory exists
IF NOT EXIST %SC%\%EXE% (
    exit 0
)

REM check for nvidia GPU
WHERE nvcuda.dll >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
  SET GPUS=0
  SET NDIR=%cd%\net.pb
) ELSE (
  SET GPUS=1
  SET NDIR=%cd%\net.uff
)
SET CPUS=%NUMBER_OF_PROCESSORS%

CALL :get_selfplay_games
EXIT /B %ERRORLEVEL%

REM run multiple instances
:rungames
    CALL %MPICMD% %SC%\%EXE% nn_type 0 nn_path %NDIR% new %SCOPT% ^
         pvstyle 1 selfplayp %~1 games.pgn train.epd quit
    echo "All jobs finished"
EXIT /B %ERRORLEVEL%

REM get selfplay games
:get_selfplay_games
    DEL cgames.pgn ctrain.epd >nul 2>&1
    SET MCWD=%cd%
    cd %SC%
    CALL :rungames %G%
    type games*.pgn* > cgames.pgn
    type train*.epd* > ctrain.epd
    DEL games*.pgn* train*.epd*
    cd %MCWD%
    MOVE %SC%\cgames.pgn %MCWD% >nul 2>&1
    MOVE %SC%\ctrain.epd %MCWD% >nul 2>&1
EXIT /B %ERRORLEVEL%

