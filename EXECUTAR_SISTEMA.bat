@echo off
chcp 65001 >nul
title Power Plus - Sistema de Agendamento e Painel Operacional
echo ======================================================================
echo   POWER PLUS - SISTEMA DE AGENDAMENTO E ATENDIMENTO (FATEC RP)
echo ======================================================================
echo.

if exist "%USERPROFILE%\tools\jdk21\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\tools\jdk21"
    set "PATH=%USERPROFILE%\tools\jdk21\bin;%PATH%"
)

where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERRO] Java 21 nao encontrado no PATH.
    echo Instale o JDK 21 ^(Eclipse Temurin^) em https://adoptium.net/ e tente novamente.
    pause
    exit /b 1
)

echo Iniciando servidor em http://localhost:8080 ...
echo   - Site do Cliente:        http://localhost:8080/
echo   - Painel do Proprietario: http://localhost:8080/painel/login
echo   - Login: carlos.powerplus ^| Senha: PowerPlus@2026
echo.
start "" cmd /c "timeout /t 5 /nobreak >nul & start http://localhost:8080/ & start http://localhost:8080/painel/login"

if exist "target\power-plus-mvp-1.0.0.jar" (
    java -jar "target\power-plus-mvp-1.0.0.jar"
) else if exist "power-plus-mvp-1.0.0.jar" (
    java -jar "power-plus-mvp-1.0.0.jar"
) else (
    call mvnw.cmd spring-boot:run
)
pause
