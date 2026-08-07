import java.io.File;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import tester.Login;
import tester.testingVariablesPile;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf;

public class BrowserVerification {
private static String host;
@Before
   public void kickoff() {
    host= testingVariablesPile.getHost();
}
@Test
public  void FireFox(){
    System.out.println("checking if all screens load in firefox, saving screenshots");
             WebDriver driver = new FirefoxDriver();

    // Puts an Implicit wait, Will wait for 10 seconds before throwing exception
           driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

    // Launch website
           driver.navigate().to(host+"");

    // Maximize the browser
              driver.manage().window().maximize();


             File screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox1.jpg"));
            driver.findElement(By.id("login")).sendKeys("tm@saepiox.com");
            driver.findElement(By.id("password")).sendKeys("pass");
            driver.findElement(By.id("button-submit")).click();

           screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox2.jpg"));
            driver.navigate().to(host+"#!position-change");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox3.jpg"));
            driver.navigate().to(host+"#!position-change-view-new");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox4.jpg"));
            driver.navigate().to(host+"#!trade");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox5.jpg"));

            driver.navigate().to(host+"#!trade-history");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox6.jpg"));
            driver.navigate().to(host+"#!positions-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox7.jpg"));
            driver.navigate().to(host+"#!csa");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox8.jpg"));
            driver.navigate().to(host+"#!portfolio-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox9.jpg"));
            driver.navigate().to(host+"#!user-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox10.jpg"));
            driver.navigate().to(host+"#!calculations-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox11.jpg"));
            driver.navigate().to(host+"#!calculations-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox12.jpg"));
            driver.navigate().to(host+"#!localized-text");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox13.jpg"));
            driver.navigate().to(host+"#!agreement");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\FireFox14.jpg"));

        } catch (IOException e) {
            e.printStackTrace();
        }

 finally {


            //Close the Browser.
            driver.close();
        }}
        @Test
    public  void Chrome(){
        WebDriver driver = new ChromeDriver();
        System.out.println("Checking if all screens function in chrome, saving screenshots");
        // Puts an Implicit wait, Will wait for 10 seconds before throwing exception
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        // Launch website
        driver.navigate().to(host+"");

        // Maximize the browser
        driver.manage().window().maximize();


        File screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome1.jpg"));
            driver.findElement(By.id("login")).sendKeys("tm@saepiox.com");
            driver.findElement(By.id("password")).sendKeys("pass");
            driver.findElement(By.id("button-submit")).click();

            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome2.jpg"));
            driver.navigate().to(host+"#!position-change");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome3.jpg"));
            driver.navigate().to(host+"#!position-change-view-new");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome4.jpg"));
            driver.navigate().to(host+"#!trade");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome5.jpg"));

            driver.navigate().to(host+"#!trade-history");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome6.jpg"));
            driver.navigate().to(host+"#!positions-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome7.jpg"));
            driver.navigate().to(host+"#!csa");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome8.jpg"));
            driver.navigate().to(host+"#!portfolio-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome9.jpg"));
            driver.navigate().to(host+"#!user-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome10.jpg"));
            driver.navigate().to(host+"#!calculations-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome11.jpg"));
            driver.navigate().to(host+"#!calculations-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome12.jpg"));
            driver.navigate().to(host+"#!localized-text");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome13.jpg"));
            driver.navigate().to(host+"#!agreement");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\Chrome14.jpg"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        finally {


            //Close the Browser.
            driver.close();
        }}
/*        @Test
   public  void edge(){
        WebDriver driver = new EdgeDriver();

        // Puts an Implicit wait, Will wait for 10 seconds before throwing exception
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        // Launch website
        driver.navigate().to(host+"");

        // Maximize the browser
        driver.manage().window().maximize();


        File screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge1.jpg"));
            driver.findElement(By.id("login")).sendKeys("tm@saepiox.com");
            driver.findElement(By.id("password")).sendKeys("pass");
            driver.findElement(By.id("button-submit")).click();

            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge2.jpg"));
            driver.navigate().to(host+"#!position-change");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge3.jpg"));
            driver.navigate().to(host+"#!position-change-view-new");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge4.jpg"));
            driver.navigate().to(host+"#!trade");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge5.jpg"));

            driver.navigate().to(host+"#!trade-history");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge6.jpg"));
            driver.navigate().to(host+"#!positions-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge7.jpg"));
            driver.navigate().to(host+"#!csa");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge8.jpg"));
            driver.navigate().to(host+"#!portfolio-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge9.jpg"));
            driver.navigate().to(host+"#!user-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge10.jpg"));
            driver.navigate().to(host+"#!calculations-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge11.jpg"));
            driver.navigate().to(host+"#!calculations-admin");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge12.jpg"));
            driver.navigate().to(host+"#!localized-text");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge13.jpg"));
            driver.navigate().to(host+"#!agreement");
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\edge14.jpg"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        finally {


            //Close the Browser.
            driver.close();
        }}
  */  }

