# Save current location and environment
Push-Location
$originalProfiles = $env:SPRING_PROFILES_ACTIVE

try {
    # Run environment check
    Write-Host "Running environment check..." -ForegroundColor Cyan
    & .\scripts\support\check_env.ps1
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Environment check failed. Exiting..." -ForegroundColor Red
        exit 1
    }

    
    # Set Spring profiles
    $env:SPRING_PROFILES_ACTIVE = "shell,severance"
    
    # Run Maven in subprocess
    cmd /c "mvn -P agent-examples-kotlin -Dmaven.test.skip=true spring-boot:run"
}
finally {
    # Restore original environment and location
    $env:SPRING_PROFILES_ACTIVE = $originalProfiles
    Pop-Location
}