@echo off
chcp 65001 >nul
title Power Plus - Sistema de Agendamento

if exist "%USERPROFILE%\tools\jdk21\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\tools\jdk21"
    set "PATH=%USERPROFILE%\tools\jdk21\bin;%PATH%"
)

where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    for /d %%D in ("%USERPROFILE%\.jdks\jdk-21*") do (
        if exist "%%D\bin\java.exe" (
            set "JAVA_HOME=%%D"
            set "PATH=%%D\bin;%PATH%"
        )
    )
)

where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Java 21 nao encontrado no PATH.
    pause
    exit /b 1
)

echo Iniciando aplicacao em http://localhost:8080 ...
start "" cmd /c "timeout /t 5 /nobreak >nul & start http://localhost:8080/"

if exist "target\power-plus-mvp-1.0.0.jar" (
    java -jar "target\power-plus-mvp-1.0.0.jar"
) else if exist "power-plus-mvp-1.0.0.jar" (
    java -jar "power-plus-mvp-1.0.0.jar"
) else (
    call mvnw.cmd spring-boot:run
)
pause
