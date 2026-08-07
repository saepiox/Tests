package tester;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Builds a Chrome/Chromium WebDriver for the local test environment.
 *
 * Machine-specific paths are NOT hardcoded here — they are supplied at runtime
 * so the harness stays portable:
 *
 *   -Dwebdriver.chrome.driver=/path/to/chromedriver   (standard Selenium property)
 *   -Dchrome.binary=/path/to/chrome                   (or env CHROME_BINARY)
 *   -Dchrome.headless=false                           (default: true)
 *
 * testenv/run-tests.sh wires these up for this machine. If webdriver.chrome.driver
 * is not set, Selenium Manager attempts to resolve a driver automatically.
 */
public final class DriverFactory {
    private DriverFactory() {}

    public static ChromeDriver chrome() {
        ChromeOptions options = new ChromeOptions();

        String binary = System.getProperty("chrome.binary");
        if (binary == null || binary.isEmpty()) {
            binary = System.getenv("CHROME_BINARY");
        }
        if (binary != null && !binary.isEmpty()) {
            options.setBinary(binary);
        }

        boolean headless = !"false".equalsIgnoreCase(System.getProperty("chrome.headless", "true"));
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1280,1024");

        return new ChromeDriver(options);
    }
}
