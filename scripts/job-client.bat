@ECHO OFF

REM setup parameters for selfplay
SET SC=%cd%\Scorpio-train\bin\Windows
SET EXE=scorpio.bat
SET G=256
SET SV=%1
SET CPUCT=%2
SET POL_TEMP=%3
SET NOISE_FRAC=%4

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
  DEL *.trt >nul 2>&1
)
SET CPUS=%NUMBER_OF_PROCESSORS%

CALL :get_selfplay_games
EXIT /B 0

REM selfplay options
SET SCOPT=nn_type 0 reuse_tree 0 fpu_is_loss 0 fpu_red 0 cpuct_init %CPUCT% ^
          policy_temp %POL_TEMP% noise_frac %NOISE_FRAC%

:rungames
REM    if %GPUS% EQU 0 (
        CALL %SC%\%EXE% nn_path %NDIR% %SCOPT% new sv %SV% ^
             pvstyle 1 selfplayp %~1 games0.pgn train0.epd quit
REM    ) else (
REM        SET /a I=%CPUS%/%GPUS%
REM        DEL *.pid
REM        for /L %%k IN (1,1,%GPUS%) DO START (
REM            echo %%k > %%k.pid
REM            SET /a m=%%k-1
REM            SET CUDA_VISIBLE_DEVICES=%%k
REM            CALL %SC%\%EXE% nn_path %NDIR% %SCOPT% new sv %SV% ^
REM                 pvstyle 1 selfplayp %~1 games%m%.pgn train%m%.epd quit
REM            DEL %%k.pid
REM        )
REM        REM wait for processes to end
REM        :wait
REM        IF EXIST *.pid goto wait
REM    )
    echo "All jobs finished"
EXIT /B 0

REM get selfplay games
:get_selfplay_games
    DEL cgames.pgn ctrain.epd >nul 2>&1
    SET MCWD=%cd%
    cd %SC%
    CALL :rungames %G%
    type games*.pgn > cgames.pgn
    type train*.epd > ctrain.epd
    DEL games*.pgn train*.epd
    cd %MCWD%
    MOVE %SC%\cgames.pgn %MCWD% >nul 2>&1
    MOVE %SC%\ctrain.epd %MCWD% >nul 2>&1
EXIT /B 0

