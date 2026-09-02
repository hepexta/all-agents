generate codebase structure for agentic project with 100 of different agents in plan. 
the first agent is a 'master agent' who should orchestrate all the other agents, stop/start them and have all the info about agents, skills and tools available. 
have first slave agent designed for pdf document data extracting
add instruction for next slave agent integration
have hexagonal architecture 
have A2A protocol in communication
have REST for UI/backend integration
make the solution ready for future async mode with kafka integration
generate separate module in the same project for simple UI based on python with simple preseted of master agent options and chat (new chat, option to load previous chat).
Have chat memory with spring AI with in-memory disk based database
implement guardrails for agents
add tool for current date with spring UI
add toolsearch tool from spring AI
generate sandbox module for python based prompt/skills testing
generate BDD (given, when, then) tests for master agent and first slave agent but with mock LLM, design it in a way to switch from mock to IT test profile, mock by default. test coverage 100%
no secrets in yaml properties
add scripts for startup/testrun