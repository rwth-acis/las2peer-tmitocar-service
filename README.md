<h1 align="center">las2peer-tmitocar-service</h1>

This service acts as a wrapper for the [social bot manager service](https://github.com/rwth-acis/las2peer-social-bot-manager-service) to use the [tmitocar tools](https://gitlab.com/Tech4Comp/tmitocar-tools). It allows active bots to forward files from the users to the tmitocar script, which produces an analysis model. With the help of the feedback.sh script of the tmitocar tools, a feedback file is created based on the analysis model and returned to the user.  

## Preparations

### Java

las2peer uses **Java 14**.

### Build Dependencies

* gradle 6.8


