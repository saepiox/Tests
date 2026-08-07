import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import tester.DriverFactory;
import tester.testingVariablesPile;

import java.time.Duration;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.support.ui.ExpectedConditions.attributeContains;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

/**
 * End-to-end test for the new Tryg integration, run against the local synthetic
 * SaepioX stand-in (testenv/server.py). Uses robust id/css selectors.
 *
 * Prereq: the stand-in must be running on the configured host (default
 * http://127.0.0.1:5000/). testenv/run-tests.sh starts it automatically.
 */
public class TrygIntegrationTest {
    private WebDriver driver;
    private WebDriverWait wait;

    @Before
    public void setUp() {
        driver = DriverFactory.chrome();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void login(String user, String pass) {
        driver.get(testingVariablesPile.getHost());
        wait.until(presenceOfElementLocated(By.id("login")));
        driver.findElement(By.id("login")).sendKeys(user);
        driver.findElement(By.id("password")).sendKeys(pass);
        driver.findElement(By.id("button-submit")).click();
        // shell is up once the logout control is present
        wait.until(presenceOfElementLocated(By.id("logout")));
    }

    /** The Tryg test user can log in and pull the synthetic Tryg policy data. */
    @Test
    public void trygTesterCanLoadSyntheticPolicies() {
        login(testingVariablesPile.getTrygLogin(), testingVariablesPile.getTrygPass());

        driver.get(testingVariablesPile.getHost() + "#!tryg");
        wait.until(presenceOfElementLocated(By.id("tryg-connect")));
        driver.findElement(By.id("tryg-connect")).click();

        // status pill flips to "ok" once the integration data has loaded
        wait.until(attributeContains(By.id("tryg-status"), "class", "ok"));

        List<WebElement> rows = driver.findElements(By.cssSelector("#tryg-rows tr"));
        assertEquals("expected 3 synthetic Tryg policies", 3, rows.size());

        WebElement firstId = driver.findElement(
                By.cssSelector("#tryg-rows tr:first-child td:first-child"));
        assertEquals("TRYG-1001", firstId.getText().trim());

        String status = driver.findElement(By.id("tryg-status")).getText();
        assertTrue("status should report a loaded count, was: " + status,
                status.toLowerCase().contains("loaded"));
    }

    /** Sanity: admin login works and logout returns to the login screen. */
    @Test
    public void adminCanLogInAndOut() {
        login(testingVariablesPile.getAdminlogin(), testingVariablesPile.getAdminPass());
        assertTrue(driver.findElement(By.id("logout")).isDisplayed());

        driver.findElement(By.id("logout")).click();
        wait.until(presenceOfElementLocated(By.id("button-submit")));
        assertTrue(driver.findElement(By.id("login")).isDisplayed());
    }
}
