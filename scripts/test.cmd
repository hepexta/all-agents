@echo off
rem Runs unit + BDD tests with the mock LLM profile (default).
cd /d "%~dp0\.."
call mvnw.cmd -pl app -am test
