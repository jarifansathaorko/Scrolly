@echo off
REM Local build/test entry point. JAVA_HOME is set to the Temurin 21 JDK that is
REM installed on this machine; override it in the environment before calling.
setlocal
if "%JAVA_HOME%"=="" set JAVA_HOME=C:\Users\jarif\.jdks\temurin-21.0.12.1
cd /d "%~dp0"
if "%1"=="" (
  gradlew.bat :app:compileDebugKotlin --console=plain
) else (
  gradlew.bat %* --console=plain
)
endlocal