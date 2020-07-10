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
SET NOISE_ALPHA=%7
SET NOISE_BETA=%8
SET FORCED_PLAYOUTS=%9
SHIFT
SET POLICY_PRUNING=%9
SHIFT
SET FPU_IS_LOSS=%9
SHIFT
SET FPU_RED=%9
SHIFT
SET PLAYOUT_CAP=%9

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

REM selfplay options
SET SCOPT=reuse_tree 0 backup_type 6 alphabeta_man_c 0 min_policy_value 0 playout_cap_rand %PLAYOUT_CAP% ^
          train_data_type %HEAD_TYPE% fpu_is_loss %FPU_IS_LOSS% fpu_red %FPU_RED% cpuct_init %CPUCT% ^
          rand_temp %RAND_TEMP% policy_temp %POL_TEMP% noise_frac %NOISE_FRAC% ^
          noise_alpha %NOISE_ALPHA% noise_beta %NOISE_BETA% forced_playouts %FORCED_PLAYOUTS% ^
          policy_pruning %POLICY_PRUNING%

CALL :get_selfplay_games
EXIT /B %ERRORLEVEL%

REM run multiple instances
:rungames
    CALL %MPICMD% %SC%\%EXE% nn_type 0 nn_path %NDIR% new %SCOPT% sv %SV% ^
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

