@echo off
rem Starts the Streamlit chat UI (requires the backend running, see start.cmd).
cd /d "%~dp0\..\ui"
pip install -r requirements.txt
streamlit run app.py
