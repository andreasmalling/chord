@echo off

TITLE main

SETLOCAL ENABLEDELAYEDEXPANSION



:checkargs

IF "%~1"=="" goto setjoin
set /A spawns = %1
%


IF "%~2"=="" goto settime
set /A spawntime = %2%




IF "%~3"=="" goto sethost

set /A host = %3%


goto init



:setjoin
set /A spawns = 3



:settime

set /A spawntime = 1



:sethost
set /A host = 9000

:init
echo Spawns: %spawns%
echo Wait: %spawntime%
echo - - - - -
echo Initial node available at:
echo %host%
start "%host%" cmd /c "mvn exec:java '-Dexec.args=%host%'"

pause



echo Joining node available at:
:joins

for /l %%x in (1, 1, %spawns%) do (

  set /A port=%host% + %%x

  echo !port!
  start "!port!" cmd /c "mvn exec:java '-Dexec.args=!port! %host%'"

  timeout %spawntime% > NUL

)



:done

echo done
