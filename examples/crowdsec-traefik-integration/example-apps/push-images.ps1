$Registry = "localhost:9002"
$Apps = @("api", "browser")
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

foreach ($App in $Apps) {
    Write-Host "Processing $App..." -ForegroundColor Cyan
    $AppPath = Join-Path $ScriptDir $App
    
    if (-not (Test-Path $AppPath)) {
        Write-Error "App path $AppPath not found!"
        continue
    }

    # Get version from package.json
    $PackageJson = Get-Content (Join-Path $AppPath "package.json") | ConvertFrom-Json
    $Version = $PackageJson.version
    $ImageName = "$App-app"
    
    $FullImageNameVersion = "$($Registry)/$($ImageName):$($Version)"
    $FullImageNameLatest = "$($Registry)/$($ImageName):latest"

    Write-Host "Building $FullImageNameVersion..." -ForegroundColor Green
    docker build -t $FullImageNameVersion -t $FullImageNameLatest $AppPath

    Write-Host "Pushing $FullImageNameVersion..." -ForegroundColor Green
    docker push $FullImageNameVersion

    Write-Host "Pushing $FullImageNameLatest..." -ForegroundColor Green
    docker push $FullImageNameLatest
}
