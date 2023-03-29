package gov.cms.ab2d.e2etest;

import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class TestLauncher {

    // Used when not being run from TravisCI as part of a build to pass in values to the TestRunner. This allows us
    // to use different environments without having to change any code

    // PIPELINE CHECK - REMOVE ME!
    public static void main(String [] args) throws InterruptedException, JSONException, IOException, InvocationTargetException,
            IllegalAccessException, KeyManagementException, NoSuchAlgorithmException {
        Environment env = Environment.DEV;
        if(args.length > 0 && args[0] != null) {
            env = Environment.valueOf(args[0]);
        }
        String contractNum = null;
        if(args.length > 1 && args[1] != null) {
            contractNum = args[1];
        }

        TestRunner testRunner = new TestRunner(env);
        testRunner.runTests(contractNum);
    }
}
