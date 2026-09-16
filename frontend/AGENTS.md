# Project overview
This is frontend part of training simulator for Russian emergency service operator. It has levels with different incidents, each incident represents test case (scenario) for operator. The main functionality is to communicate with the victim (who is impersonated by the AI), ask him questions, and then fill out an incident report based on the answers received. The user can complete the dialogue and then close the incident card and get a detailed review of the results. When the user closes the incident card, the solution context - the data they filled in - should be sent to the backend.

# Stask
* Astro.js
* webawesome components

# Code style
* Do not move one tag's < > to different lines, all tag attributes must be written on one line. Every new tag (including closing one's) should be on new line.
* Try to keep code minimal, but safe and reliable
