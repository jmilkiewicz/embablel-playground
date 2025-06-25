@echo off
setlocal

call .\scripts\support\check_env.bat

if errorlevel 1 (
    echo Environment check failed. Exiting...
    exit /b 1
)


set SPRING_PROFILES_ACTIVE=shell,severance

cmd /c mvn -P agent-examples-kotlin -Dmaven.test.skip=true spring-boot:run

endlocal