<h1 align="center">las2peer-tmitocar-service</h1>

This service acts as a wrapper for the [social bot manager service](https://github.com/rwth-acis/las2peer-social-bot-manager-service) to use the [tmitocar tools](https://gitlab.com/Tech4Comp/tmitocar-tools). It allows active bots to forward files from the users to the tmitocar script, which produces an analysis model. With the help of the feedback.sh script of the tmitocar tools, a feedback file is created based on the analysis model and returned to the user.  

## Preparations

### Java

las2peer uses **Java 14**.

### Build Dependencies

* gradle 6.8


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
|----------|---------|-------------|---------|-------------|
| /analyzeText | compareTextFromBot | Compares user text to expert text using tmitocar tools. Returns feedback pdf file based on analysis. If LRS_AUTH_TOKEN_LEIPZIG is given, additionally stores information in LRS. | - submissionSucceeded: Returned text in case of success <br />- submissionFailed: Returned text in case of error <br /> - lrs: Bool whether LRS should be used <br /> Note: Function only works if triggering message contains a file (Use "ifFile" field of "Incoming Message" element)| Feedback file on success, otherwise error message |
