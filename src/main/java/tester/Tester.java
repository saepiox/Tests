package tester;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf;

public class Tester {
    private static String targethost = "localhost:5000/";

    public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
        testingVariablesPile.setAdminPass("pass");
        testingVariablesPile.setAdminlogin("tj@saepiox.com");
        testingVariablesPile.setHost(targethost);

        try {
    // Login.admin(driver);

            // Dashboard.Test(driver);
            //  logouttest.logout(driver);

        // Agreement.createagreement(driver);
            // System.out.println(Agreement.testAgreementCreation(driver));
            //GenerateUser.auser(driver);
         //   Login.admin(driver);
          // Login.admin(driver);
   //GenerateUser.Delete_test_user(driver);
      //Login.charles(driver);
  //  Agreement.createAndDestroyAgreement(driver);
        } finally {
            //  driver.quit();
        }
    }
}

