@echo off

TITLE launch

SETLOCAL ENABLEDELAYEDEXPANSION



:checkargs

IF "%~1"=="" goto setjoin
set /A spawns = %1
%


IF "%~2"=="" goto settime
set /A spawntime = %2%




IF "%~3"=="" goto sethost

set /A host = %3%
goto spawnjoin



:setjoin
set /A spawns = 10


:settime

set /A spawntime = 3


:sethost
set /A host = 9000

:init
echo Spawns: %spawns%
echo Wait: %spawntime%

:spawnhost
echo Initializing host at:
echo %host%
start "%host%" cmd /c "mvn exec:java -Dexec.args='%host%'"
pause



:spawnjoin
echo Joining at:
echo %host%
echo Initializing node at:

for /l %%x in (1, 1, %spawns%) do (

  set /A port=%host% + %%x

  echo !port!
  start "!port!" cmd /c "mvn exec:java "-Dexec.args=!port! %host%""

  timeout %spawntime% > NUL

)



:done

echo done
