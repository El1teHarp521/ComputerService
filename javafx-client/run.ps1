# Запуск JavaFX-приложения (Windows PowerShell). База данных PostgreSQL должна быть уже запущена.
# Запуск:  powershell -ExecutionPolicy Bypass -File .\run.ps1
$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "Maven не найден. Нужен Maven 3.9+ и Java 21." -ForegroundColor Red
    exit 1
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "Java не найдена. Нужна Java 21." -ForegroundColor Red
    exit 1
}

mvn -q javafx:run
