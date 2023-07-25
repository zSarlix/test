package com.applitools.example;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.EyesRunner;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.selenium.BrowserType;
import com.applitools.eyes.selenium.ClassicRunner;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.visualgrid.model.DeviceName;
import com.applitools.eyes.visualgrid.model.ScreenOrientation;
import com.applitools.eyes.visualgrid.services.RunnerOptions;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.time.Duration;


public class AcmeBankTests {
    // This class contains everything needed to run a full visual test against the ACME bank site.
    // It runs the test once locally.
    // If you use the Ultrafast Grid, then it performs cross-browser testing against multiple unique browsers.
    // It runs the test from a main function, not through a test framework.

    // Test constants
    private final static boolean USE_ULTRAFAST_GRID = true;
    private final static boolean USE_EXECUTION_CLOUD = false;
    private final static String RUNNER_NAME = (USE_ULTRAFAST_GRID) ? "Ultrafast Grid" : "Classic runner";
    private final static BatchInfo BATCH = new BatchInfo("Example: Selenium Java Basic with the " + RUNNER_NAME);

    public static void main(String [] args) {

        EyesRunner runner = null;
        Eyes eyes = null;
        WebDriver driver = null;

        try {
            // The following steps set up Applitools for testing.

            if (USE_ULTRAFAST_GRID) {
                // Create the runner for the Ultrafast Grid.
                // Concurrency refers to the number of visual checkpoints Applitools will perform in parallel.
                // Warning: If you have a free account, then concurrency will be limited to 1.
                runner = new VisualGridRunner(new RunnerOptions().testConcurrency(5));
            }
            else {
                // Create the Classic runner.
                runner = new ClassicRunner();
            }

            // Create the Applitools Eyes object connected to the runner and set its configuration.
            eyes = new Eyes(runner);

            // Create a configuration for Applitools Eyes.
            Configuration config = eyes.getConfiguration();

            // Set the Applitools API key so test results are uploaded to your account.
            // If you don't explicitly set the API key with this call,
            // then the SDK will automatically read the `APPLITOOLS_API_KEY` environment variable to fetch it.
            config.setApiKey(System.getenv("APPLITOOLS_API_KEY"));

            // Read the headless mode setting from an environment variable.
            // Use headless mode for Continuous Integration (CI) execution.
            // Use headed mode for local development.
            boolean headless = Boolean.parseBoolean(System.getenv().getOrDefault("HEADLESS", "false"));

            // Create a new batch for tests.
            // A batch is the collection of visual tests.
            // Batches are displayed in the Eyes Test Manager, so use meaningful names.
            config.setBatch(BATCH);

            // If running tests on the Ultrafast Grid, configure browsers.
            if (USE_ULTRAFAST_GRID) {

                // Add 3 desktop browsers with different viewports for cross-browser testing in the Ultrafast Grid.
                // Other browsers are also available, like Edge and IE.
                config.addBrowser(800, 600, BrowserType.CHROME);
                config.addBrowser(1600, 1200, BrowserType.FIREFOX);
                config.addBrowser(1024, 768, BrowserType.SAFARI);

                // Add 2 mobile emulation devices with different orientations for cross-browser testing in the Ultrafast Grid.
                // Other mobile devices are available, including iOS.
                config.addDeviceEmulation(DeviceName.Pixel_2, ScreenOrientation.PORTRAIT);
                config.addDeviceEmulation(DeviceName.Nexus_10, ScreenOrientation.LANDSCAPE);
            }

            // Set the configuration for Eyes
            eyes.setConfiguration(config);

            // Create ChromeDriver options
            ChromeOptions options = new ChromeOptions().setHeadless(headless);

            if (USE_EXECUTION_CLOUD) {
                // Open the browser remotely in the Execution Cloud.
                driver = new RemoteWebDriver(new URL(Eyes.getExecutionCloudURL()), options);
            }
            else {
                // Open the browser with a local ChromeDriver instance.
                driver = new ChromeDriver(options);
            }

            // Set an implicit wait of 10 seconds.
            // For larger projects, use explicit waits for better control.
            // https://www.selenium.dev/documentation/webdriver/waits/
            // The following call works for Selenium 4:
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            // If you are using Selenium 3, use the following call instead:
            // driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);


            // The following steps are a test covering login for the Applitools demo site, which is a dummy banking app.
            // The interactions use typical Selenium WebDriver calls,
            // but the verifications use one-line snapshot calls with Applitools Eyes.
            // If the page ever changes, then Applitools will detect the changes and highlight them in the Eyes Test Manager.
            // Traditional assertions that scrape the page for text values are not needed here.

            // Open Eyes to start visual testing.
            // It is a recommended practice to set all four inputs:
            eyes.open(
                
                // WebDriver object to "watch".
                driver,
                
                // The name of the application under test.
                // All tests for the same app should share the same app name.
                // Set this name wisely: Applitools features rely on a shared app name across tests.
                "ACME Bank Web App",
                
                // The name of the test case for the given application.
                // Additional unique characteristics of the test may also be specified as part of the test name,
                // such as localization information ("Home Page - EN") or different user permissions ("Login by admin"). 
                "Log into bank account",
                
                // The viewport size for the local browser.
                // Eyes will resize the web browser to match the requested viewport size.
                // This parameter is optional but encouraged in order to produce consistent results.
                new RectangleSize(1200, 600));

            // Load the login page.
            driver.get("https://demo.applitools.com");

            // Verify the full login page loaded correctly.
            eyes.check(Target.window().fully().withName("Login page"));

            // Perform login.
            driver.findElement(By.id("username")).sendKeys("applibot");
            driver.findElement(By.id("password")).sendKeys("I<3VisualTests");
            driver.findElement(By.id("log-in")).click();

            // Verify the full main page loaded correctly.
            // This snapshot uses LAYOUT match level to avoid differences in closing time text.
            eyes.check(Target.window().fully().withName("Main page").layout());

            // Close Eyes to tell the server it should display the results.
            eyes.closeAsync();
        }
        catch (Exception e) {
            // Dump any errors and abort any tests.
            e.printStackTrace();
            if (eyes != null)
                eyes.abortAsync();
        }

        try {
            // No matter what, perform cleanup.
            if (driver != null)
                driver.quit();

            // Close the batch and report visual differences to the console.
            // Note that it forces execution to wait synchronously for all visual checkpoints to complete.
            if (runner != null) {
                TestResultsSummary allTestResults = runner.getAllTestResults();
                System.out.println(allTestResults);
            }
        }
        catch (Exception e) {
            // Dump any cleanup errors.
            e.printStackTrace();
        }

        // Always force execution to end.
        System.exit(0);
    }
}
