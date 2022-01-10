<h1 align="center">las2peer-tmitocar-service</h1>

This service acts as a wrapper for the [social bot manager service](https://github.com/rwth-acis/las2peer-social-bot-manager-service) to use the [tmitocar tools](https://gitlab.com/Tech4Comp/tmitocar-tools). It allows active bots to forward files from the users to the tmitocar script, which produces an analysis model. With the help of the feedback.sh script of the tmitocar tools, a feedback file is created based on the analysis model and returned to the user.  

## Preparations

### Java

las2peer uses **Java 14**.

### Build Dependencies

* gradle 6.8

### Important Repositories: 

- Social-Bot-Manager-Service: https://github.com/rwth-acis/las2peer-Social-Bot-Manager-Service
- Tmitocar tools: https://github.com/rwth-acis/las2peer-tmitocar-service

How to run using Docker
-------------------

First build the image:
```bash
docker build -t tmitocar-service . 
```

Then you can run the image like this:

```bash
docker run -e PUBLIC_KEY=*PublicKeyForTmitocarServerAuth* -e PRIVATE_KEY=*PrivateKeyForTmitocarServerAuth* -e LRS_URL=*https://your-lrs-address
* -p 8080:8080 -p 9011:9011 tmitocar-service
```

Replace *PublicKeyForTmitocarServerAuth* and *PrivateKeyForTmitocarServerAuth* with the appropriate keys handed by the admin of the server on which the tmitocar script is hosted. 

The REST-API will be available via *http://localhost:8080/tmitocar* and the las2peer node is available via port 9011.

## Bot Functions

| Path | Function name | Description | Parameters | Returns |
|-----|-----|-------------|---------|---------------|
| /analyzeText | compareTextFromBot | Compares user texts of assignments 1 to 12 to experts text using tmitocar tools. Returns feedback pdf file based on analysis. If LRS_AUTH_TOKEN_LEIPZIG is given, additionally stores information in LRS. |  submissionSucceeded: Returned text in case of success <br /> submissionFailed: Returned text in case of error <br />  lrs: Bool whether LRS should be used <br /> Note: Function only works if triggering message contains a file (Use "ifFile" field of "Incoming Message" element)| Feedback file on success, otherwise error message |
| /sendJson | sendJson | Sends json graph file based on single text analysis. To be used immediately after "/analyzeText" was called. | submissionSucceeded: Returned text in case of success <br /> submissionFailed: Returned text in case of error <br /> Note: Function only works if immediately triggered after call of "/analyzeText" | Json file of text model based on analysis. |
| /getCredits | getCredits | Returns the current status of handed in assignments for tasks 1-12 based on data found in LRS. Also gives the current percentage of reached bonus points. | / | Message of the form: <br /> Schreibaufgabe 01: X ... Schreibaufgabe 12: Y <br />Das heißt, du hast bisher Z Leistunsprozente gesammelt. |
| /analyzeTextTUDresden | compareTextFromBotDresden | Compares user text of given task to expert text (Medienkompetenz) using tmitocar tools. Returns feedback pdf file based on analysis. If LRS_AUTH_TOKEN_DRESDEN is given, additionally stores information in LRS. | - submissionSucceeded: Returned text in case of success <br />- submissionFailed: Returned text in case of error <br /> - lrs: Bool whether LRS should be used <br /> Note: Function only works if triggering message contains a file (Use "ifFile" field of "Incoming Message" element) | Feedback file on success, otherwise error message |
| /analyzeSingleText | analyzeSingleText | Analyses single text from user and create feedback file based on analysis results. If LRS_AUTH_TOKEN_DRESDEN is given, additionally stores information in LRS.| submissionSucceeded: Returned text in case of success <br /> submissionFailed: Returned text in case of error <br />  lrs: Bool whether LRS should be used <br /> Note: Function only works if triggering message contains a file (Use "ifFile" field of "Incoming Message" element) | Feedback file on success, otherwise error message|
| /storeCompareText | storeCompareText | Used when wanting to compare two texts from the user. Will create feedback file based on analysis of comparison from two user texts. Is the first of two function calls needed for the comparison. Is used to first store the text, while waiting for the second text. The second function is "compareUserTexts" | submissionSucceeded: Returned text in case of success <br /> submissionFailed: Returned text in case of error <br />  Note: Function only works if triggering message contains a file (Use "ifFile" field of "Incoming Message" element) | Message of storing success/failure |
| /compareUserTexts | compareUserTexts | Second function call when wanting to compare two user texts. Needs to be called after "storeCompareText". WIll use tmitocar script to conduct analysis and create feedback file which is then returned.  If LRS_AUTH_TOKEN_DRESDEN is given, additionally stores information in LRS. | submissionSucceeded: Returned text in case of success <br /> submissionFailed: Returned text in case of error <br />  lrs: Bool whether LRS should be used <br /> Note: Function only works if triggering message contains a file (Use "ifFile" field of "Incoming Message" element) |  Feedback file on success, otherwise error message|
