@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title Fisio

rem Executa o sistema a partir desta pasta (%~dp0).
rem Estrutura esperada:
rem   fisio-system-sql2o.jar
rem   jdk-17...\bin\java.exe   (ou JAVA_HOME/PATH)
rem   data\                    (criada aqui se faltar)
rem
rem Para outra porta, descomente a linha abaixo:
rem set "PORT=8081"

cd /d "%~dp0"

if not exist "fisio-system-sql2o.jar" (
    echo [ERRO] fisio-system-sql2o.jar nao encontrado em "%~dp0".
    pause
    exit /b 1
)

if not exist "data" mkdir "data"

set "JAVA_BIN="
for /d %%D in ("%~dp0jdk*") do (
    if exist "%%~fD\bin\java.exe" set "JAVA_BIN=%%~fD\bin\java.exe"
)
if not defined JAVA_BIN if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_BIN (
    where java >nul 2>&1
    if not errorlevel 1 set "JAVA_BIN=java"
)
if not defined JAVA_BIN (
    echo [ERRO] Java nao encontrado. Extraia o JDK nesta pasta ^(jdk-17^) ou instale o Java 17.
    pause
    exit /b 1
)

if not defined JAVA_OPTS set "JAVA_OPTS=-Dfile.encoding=UTF-8"

echo Iniciando Fisio com: "%JAVA_BIN%"
"%JAVA_BIN%" %JAVA_OPTS% -jar "fisio-system-sql2o.jar"
pause
