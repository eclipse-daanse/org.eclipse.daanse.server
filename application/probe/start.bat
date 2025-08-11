@echo off
setlocal

REM path to Java-Binary
set JAVA_EXEC=.\java\bin\java.exe

REM Logback-Configuration-File
set LOGBACK_CONFIG=.\logback.xml

REM Check, that Java-Binary exists
if not exist "%JAVA_EXEC%" (
    echo Java-Binary %JAVA_EXEC% not found.
    exit /b 1
)

REM Check, that Logback-Configuration esists
if not exist "%LOGBACK_CONFIG%" (
    echo Logback-Configuration %LOGBACK_CONFIG% not found.
    exit /b 1
)

REM start Server
echo Starte Server
"%JAVA_EXEC%" -Dlogback.configurationFile=file:"%LOGBACK_CONFIG%" -jar daanse.probe.jar

endlocal

