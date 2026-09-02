@echo off
rem Integration tests against a real LLM (profile "it", @it-tagged scenarios).
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
call mvnw.cmd -pl app -am test -Pit
