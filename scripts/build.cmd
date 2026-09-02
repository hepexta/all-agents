@echo off
rem Full build: compiles, runs all tests (mock LLM profile) and enforces 100%% line coverage.
cd /d "%~dp0\.."
call mvnw.cmd clean verify
