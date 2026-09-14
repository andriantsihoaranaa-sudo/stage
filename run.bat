@echo off
REM Script de lancement pour Windows de « Gestion des Naissances »
echo ====================================================
echo   Lancement de « Gestion des Naissances »
echo   Application Desktop Hors-Ligne (JavaFX + SQLite)
echo ====================================================

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    mvn javafx:run
) else (
    if exist target\gestion-naissances-1.0.0.jar (
        java -jar target\gestion-naissances-1.0.0.jar
    ) else (
        echo Erreur : Maven ou le fichier JAR compile est introuvable.
        echo Lancez 'mvn clean package' pour generer l'executable.
        pause
    )
)
