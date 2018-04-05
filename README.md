Azure offers log analytics that are equivalent to ELK or Splunk. Log Analytics has several APIs and methods through which logs can be shared from the apps running on Azure or on-premise. Available methods are OS Agents (Windows/Linux), Azure VMs, Azure PaaS (only system metrics), and CustomLogs (OS-based only).

In the case of Spring Boot, we use Azure API apps running on an App Service Plan to deploy them. This typically means there is no OS associated with the deployment (at least, explicitly). 

Though the search and analytics ability of Azure Log Analytics catches up to ELK or Splunk, connectors-wise, it still lacks versatility as it doesn't have any Log API connectors (i.e. logback or Log4j) and misses Java APIs. 

A sample project I have created solves part of this problem by sharing custom logs to Azure Log Analytics asynchronously from Spring Boot applications. 

Required connection parameters from Log Analytics are:

1. Workspace-Id

2. Shared ley

https://dzone.com/storage/temp/7252058-azupsdsstlog1-microsoft-azure.jpg

Both of these can be retrieved from Log Analytics > Advanced Settings > Connected Resources.

Image title

That's good enough! We do not need specific URIs or even the app names. 

Now, let's move on the Spring Boot application. It's a simple Spring Boot REST application that will asynchronously write log data into Log Analytics via its REST API in JSON format. Yes, Azure provides an out of the box REST API, but the tricky part is its authorization and plenty other conventions that need to be taken care of to use it efficiently. 

Application.java is simple; @EnableAsync is added to enable asynchronous processing:


RestController.java is the API controller with a POST method that invokes a logger component to write logs into Log Analytics. It returns a hardcoded text message in response. For the request, the sample JSON message {"id":1,"name":"Azure"} can be used.


AzureLoggerComponent.java does the heavy lifting here. It takes constructing the REST request based on Azure's recommendation. The logger component constructs the log message with a log-type,which is a mandatory field typically used to differentiate logs from various sources in Log Analytics. In my case, I follow a microservices architecture with Spring Boot with many independent applications. I mandate the use of spring.application.name and spring.application.version in all these apps, so I use a custom format for the log-type:


This is very helpful when using the Log Analytics search functionality.

Finally, application.yml has the required parameters:


Upon successfully invoking the rest endpoint, logs are sent in the below sample format with a co-relation ID and a custom message. 


Once the logger components invoke the REST API, it typically takes from two to five minutes for the logs to appear in Log Analytics. After which the search query search demoapp_v0_al can be used to shortlist logs specific to this particular application. Advanced search queries can be found here. The result of the above query is:

Image title

The "export" option can be used to export these logs in Excel or CSV format. 

Source code can be found here.

PS: I use lombok for simplified Java models. It is really useful! Also, this can be converted to a logback appender to seamlessly send logs to Azure Log Analytics. 
