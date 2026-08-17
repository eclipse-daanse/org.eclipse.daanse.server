@echo off
setlocal

REM Path to Java-Binary
if defined JAVA_HOME (set "JAVA_EXEC=%JAVA_HOME%\bin\java") else (set "JAVA_EXEC=java")

REM The bundles declare osgi.ee=JavaSE-25. An older JVM starts the framework and
REM opens the HTTP port anyway, leaves most bundles unresolved, and then serves
REM nothing - so check here rather than debug that.
set "REQUIRED_JAVA=25"

REM Logback-Configuration-File
set "LOGBACK_CONFIG=.\logback.xml"

REM Check if java -version works
"%JAVA_EXEC%" -version >nul 2>&1
if errorlevel 1 (
    echo "Java is installed but not working properly (java -version failed)."
    exit /b 1
)

REM "25.0.4" -^> 25, and the old "1.8.0_402" -^> 8.
for /f tokens^=2^ delims^=^" %%v in ('"%JAVA_EXEC%" -version 2^>^&1 ^| findstr /i "version"') do (
    for /f "tokens=1,2 delims=._" %%a in ("%%v") do (
        if "%%a"=="1" (set "JAVA_MAJOR=%%b") else (set "JAVA_MAJOR=%%a")
    )
    goto :checked
)
:checked

if %JAVA_MAJOR% LSS %REQUIRED_JAVA% (
    echo "Java %REQUIRED_JAVA% is required, but %JAVA_EXEC% is Java %JAVA_MAJOR%."
    echo "Point JAVA_HOME at a Java %REQUIRED_JAVA% installation."
    exit /b 1
)

REM Check Logback configuration file exists
if not exist "%LOGBACK_CONFIG%" (
    echo "Logback configuration file "%LOGBACK_CONFIG%" not found."
    exit /b 1
)

REM Start Server
echo "Starting server..."
"%JAVA_EXEC%" -Dlogback.configurationFile=file:"%LOGBACK_CONFIG%" -jar daanse.playground.jar

endlocal
