package gov.cms.ab2d.e2etest;

import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class TestLauncher {

    // Used when not being run from TravisCI as part of a build to pass in values to the TestRunner. This allows us
    // to use different environments without having to change any code
    public static void main(String [] args) throws InterruptedException, JSONException, IOException, InvocationTargetException, IllegalAccessException {
        Environment env = Environment.DEV;
        if(args.length > 0 && args[0] != null) {
            env = Environment.valueOf(args[0]);
        }

        TestRunner testRunner = new TestRunner(env);

        testRunner.runTests();
    }
}
