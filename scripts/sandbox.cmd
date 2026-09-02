@echo off
rem Runs the prompt/skill testing sandbox (requires the backend running, see start.cmd).
cd /d "%~dp0\..\sandbox"
pip install -r requirements.txt
python runner.py
pytest tests -v
