<#
Script de ayuda para ejecutar la aplicación desde PowerShell.
Este script asume que Maven está instalado y disponible en PATH.

Uso:
  - Abrir PowerShell y ejecutar desde la carpeta backend:
      .\run.ps1

El script:
  1) Establece variables de entorno DB si no existen (valores por defecto incluidos)
  2) Empaqueta el WAR (mvn package)
  3) Arranca Jetty en modo desarrollo (mvn jetty:run)
#>

param(
    [int]$Port = 8080
)

Write-Host "== Ejecutando script de arranque para coovalluna (backend) =="

# Establecer variables de entorno temporales para la sesión si no están definidas
if (-not $env:DB_URL) {
    $env:DB_URL = 'jdbc:postgresql://localhost:5432/coovalluna'
    Write-Host "DB_URL no definida. Usando valor por defecto: $env:DB_URL"
} else {
    Write-Host "DB_URL encontrada: $env:DB_URL"
}
if (-not $env:DB_USER) {
    $env:DB_USER = 'postgres'
    Write-Host "DB_USER no definida. Usando valor por defecto: $env:DB_USER"
} else {
    Write-Host "DB_USER encontrada: $env:DB_USER"
}
if (-not $env:DB_PASSWORD) {
    $env:DB_PASSWORD = '1025'
    Write-Host "DB_PASSWORD no definida. Usando valor por defecto."
} else {
    Write-Host "DB_PASSWORD encontrada (oculta)."
}

Write-Host "Compilando y empaquetando la aplicación (esto descargará dependencias la primera vez)..."
mvn -DskipTests package
if ($LASTEXITCODE -ne 0) {
    Write-Error "mvn package falló. Revisa la salida anterior."
    exit $LASTEXITCODE
}

Write-Host "Iniciando Jetty en puerto $Port... (Ctrl+C para parar)"
mvn -Djetty.port=$Port jetty:run

