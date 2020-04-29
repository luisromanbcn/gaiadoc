# gaiadoc

Version 0.0.1-alpha

Gaiadoc is a Service that allows you to deploy Docker Containers in multiple Docker Host, this is mainly created for people who needs to deploy containers used for testing: Android Emulators, browsers, etc...

How to use: gradle bootRun

This service is using mainly the port 8081, you can change it in application.properties

POST /client/v1/addClientBasic

Body example:

{
    "host": "tcp://192.168.1.1:2375"
}

POST /container/v1/createContainer

Body example:

{
	"image": "container/android-x86-10.0:latest",
	"portBindings": [{
		"privatePort": 6080,
		"publicRandomRangePort": "20000-25000"
	},
	{
		"privatePort": 5554,
		"publicRandomRangePort": "30000-34999"
	},
	{
		"privatePort": 5555,
		"publicPort": 9999
	}
	],
	"privileged": true,
	"envVariables": [
		"DEVICE=Samsung Galaxy S6"
	]
}

DELETE /container/v1/deleteContainer/{containerId} (you can get it from createContainer response body or containerList API call)



DELETE /client/v1/deleteClient{hostId} (you can get it from addClientBasic API from info.id or clientList API)


GET /container/v1/containerList


GET /client/v1/getClientList
