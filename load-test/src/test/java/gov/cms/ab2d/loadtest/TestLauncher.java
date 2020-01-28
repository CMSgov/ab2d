package gov.cms.ab2d.loadtest;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.java.sampler.JavaSampler;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class TestLauncher {

    public static void main(String [] args) throws IOException {
        /*TestRunner testRunner = new TestRunner();
        Arguments arguments = new Arguments();
        arguments.addArgument("contracts", "S0003, S0004,S0006");
        arguments.addArgument("api-url", "http://localhost:8080/api/v1/fhir/");
        arguments.addArgument("okta-url", "https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token");
        JavaSamplerContext javaSamplerContext = new JavaSamplerContext(arguments);
        testRunner.setupTest(javaSamplerContext);
        testRunner.runTest(javaSamplerContext);*/

        StandardJMeterEngine jmeter = new StandardJMeterEngine();

        //JMeter initialization (properties, log levels, locale, etc)
        JMeterUtils.loadJMeterProperties("/Users/andy.daykin/code/jmeter/bin/jmeter.properties");
        JMeterUtils.setJMeterHome("/Users/andy.daykin/code/jmeter");
        //JMeterUtils.setLocale(Locale.ENGLISH);

        //JMeterUtils.initLocale();

        // JMeter Test Plan, basic all u JOrphan HashTree
        HashTree testPlanTree = new HashTree();

        // Java Sampler
        JavaSampler javaSampler = new JavaSampler();
        javaSampler.setClassname("gov.cms.ab2d.loadtest.TestRunner");
        //javaSampler.setClassname("org.apache.jmeter.protocol.java.test.SleepTest");
        javaSampler.setName("Test Runner");
        javaSampler.setComment("Test Runner Comment");

        // Loop Controller
        LoopController loopController = new LoopController();
        loopController.setLoops(1);
        loopController.addTestElement(javaSampler);
        loopController.setFirst(true);
        loopController.initialize();

        // Thread Group
        org.apache.jmeter.threads.ThreadGroup threadGroup = new org.apache.jmeter.threads.ThreadGroup();
        threadGroup.setNumThreads(1);
        threadGroup.setRampUp(1);
        threadGroup.setSamplerController(loopController);

        // Test Plan
        TestPlan testPlan = new TestPlan("Create JMeter Script From Java Code");

        // Construct Test Plan from previously initialized elements
        testPlanTree.add("testPlan", testPlan);
        testPlanTree.add("loopController", loopController);
        testPlanTree.add("threadGroup", threadGroup);
        testPlanTree.add("javaSampler", javaSampler);

        SaveService.saveTree(testPlanTree, new FileOutputStream("test.jmx"));

        //add Summarizer output to get test progress in stdout like:
        // summary =      2 in   1.3s =    1.5/s Avg:   631 Min:   290 Max:   973 Err:     0 (0.00%)
        Summariser summer = null;
        String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
        if (summariserName.length() > 0) {
            summer = new Summariser(summariserName);
        }


        // Store execution results into a .jtl file
        String logFile = "test.jtl";
        ResultCollector logger = new ResultCollector(summer);
        logger.setFilename(logFile);
        testPlanTree.add(testPlanTree.getArray()[0], logger);

        // Run Test Plan
        jmeter.configure(testPlanTree);
        jmeter.run();

        System.out.println("Test completed. See test.jtl file for results");
        System.out.println("JMeter .jmx script is available at test.jmx");
        System.exit(0);
    }
}
