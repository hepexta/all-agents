@echo off
rem Builds and starts the backend (REST + A2A endpoints on :8080).
rem API credentials are read from .\.env (never committed).
cd /d "%~dp0\.."
if exist .env (
  for /f "usebackq tokens=1,* delims== " %%a in (".env") do (
    if "%%a"=="export" (
      for /f "tokens=1,* delims==" %%x in ("%%b") do set "%%x=%%y"
    ) else (
      set "%%a=%%b"
    )
  )
)
call mvnw.cmd -pl app -am package -DskipTests
java -jar app\target\all-agents-app-0.0.1-SNAPSHOT.jar
