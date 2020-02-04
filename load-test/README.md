Running JMeter

1. Download JMeter from https://jmeter.apache.org/download_jmeter.cgi, this code has been tested
with 5.2.1

2. Extract contents to a directory of your choice

3. Setup the environment variable AB2D_JMETER_HOME to the directory you extracted JMeter to 
in step 2. The maven build step will move a JAR to a subdirectory there.

4. Change the heap size of JMeter by modifying AB2D_JMETER_HOME/bin/jmeter and find the line
that starts with : "${HEAP:
Change the heap size to 4GB 

5. Add AB2D_JMETER_HOME/bin/ to your path

6. Launch JMeter by running the command `jmeter`

7. Click the open button and load the load-test.jmx file from the resources directory in this module

8. Click the play button to run the tests