<h1 align="center">las2peer-tmitocar-service</h1>

This service acts as a wrapper for the [social bot manager service](https://github.com/rwth-acis/las2peer-social-bot-manager-service) to use the [tmitocar tools](https://gitlab.com/Tech4Comp/tmitocar-tools). It allows active bots to forward files from the users to the tmitocar script, which produces an analysis model. With the help of the feedback.sh script of the tmitocar tools, a feedback file is created based on the analysis model and returned to the user.  

## Preparations

### Java

las2peer uses **Java 8**.

If you use an Oracle Java version, please make sure you have **Java 8u162** or later installed, so that the Java Cryptography Extension (JCE) is enabled.
Otherwise, you have to enable it manually.
Each las2peer node performs an encryption self-test on startup.

### Build Dependencies

* Apache ant


## Quick Setup of your Service Development Environment

*If you never used las2peer before, it is recommended that you first visit the
[Step by Step - First Service](https://github.com/rwth-acis/las2peer-Template-Project/wiki/Step-By-Step:-First-Service)
tutorial for a more detailed guidance on how to use this template.*  

Follow these five steps to setup your project:  
1. If you use Eclipse, import this project or just create a new project in the same folder.  
2. Run "ant get_deps" once to pull all dependencies (You can skip this but Eclipse will complain about missing libraries until you build the first time).  
3. The service source code can be found at `i5.las2peer.services.servicePackage.TemplateService`.  
(3.5 Optional: Change [etc/ant_configuration/service.properties](etc/ant_configuration/service.properties) and [etc/ant_configuration/user.properties](etc/ant_configuration/user.properties)
according to the service you want to build. Rename your build directory structure according to the names you gave in 2.,
you have to also correct the package declaration and the 'testTemplateService' constant in your source code files.)  
4. Compile your service with `ant jar` or just `ant` (default target). This will also build the service jar.  
5. Generate documentation, run your JUnit tests and generate service and user agent with `ant all` (If this did not run check that the policy files are working correctly).  

The jar file with your service will be in "export/" and "service/" and the generated agent XML files in "etc/startup/".
You can find the JUnit reports in the folder "export/test_reports/".  

If you decide to change the dependencies of your project, please make sure to run "ant clean_all" to remove all previously
added libraries first.  


## Next Steps

Please visit the [Wiki](https://github.com/rwth-acis/las2peer-Template-Project/wiki/) of this project.
There you will find guides and tutorials, information on las2peer concepts and further interesting las2peer knowledge.  
