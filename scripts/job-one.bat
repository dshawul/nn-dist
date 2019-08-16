REM hack for parallel GPU runs
@ECHO OFF
SET CUDA_VISIBLE_DEVICES=%1
CALL %2 %3
DEL %1.pid
