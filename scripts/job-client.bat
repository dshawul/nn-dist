@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION

REM setup parameters for selfplay
SET SC=%cd%\Scorpio\bin\Windows
SET EXE=scorpio.bat
SET G=512
SET SV=%1
SET CPUCT=%2
SET POL_TEMP=%3
SET NOISE_FRAC=%4
SET HEAD_TYPE=%5
SET RAND_TEMP=%6

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

REM selfplay options
SET SCOPT=train_data_type %HEAD_TYPE% alphabeta_man_c 0 min_policy_value 0 ^
          nn_type 0 reuse_tree 0 fpu_is_loss 0 fpu_red 0 cpuct_init %CPUCT% ^
          backup_type 6 rand_temp %RAND_TEMP% policy_temp %POL_TEMP% noise_frac %NOISE_FRAC%

REM run multiple instances
:rungames
    CALL %MPICMD% %SC%\%EXE% nn_path %NDIR% %SCOPT% new sv %SV% ^
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

