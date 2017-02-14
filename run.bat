@echo off
SETLOCAL ENABLEDELAYEDEXPANSION
set /A port=9000
start cmd /c "mvn exec:java -Dexec.args='8080'"
pause
:check
IF NOT "%port%"=="9045" (Goto loop)
goto end
:loop
echo %port%
start cmd /c "mvn exec:java "-Dexec.args=%port% 8080""
set /A port = %port%+1
timeout 1 > NUL
goto check
:end
echo done
pause
)
