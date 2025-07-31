A small project to hold shared code for testing. 

TestContext will be useful for all lambda projects and if copied will likely drift in implementation over time. 


Tp use it for other lambda projects add this to the project's build.gradle
`testImplementation project(':lambda-test-utils')`