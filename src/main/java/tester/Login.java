package tester;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf;

public class Login {

    public static void admin(WebDriver adriver){
        WebDriverWait wait = new WebDriverWait(adriver, java.time.Duration.ofSeconds(10));
        adriver.get(testingVariablesPile.getHost());
        wait.until(presenceOfElementLocated(By.tagName("html")));
        WebElement a =adriver.findElement(By.tagName("html"));
        adriver.findElement(By.id("login")).sendKeys(testingVariablesPile.getAdminlogin());
        adriver.findElement(By.id("password")).sendKeys(testingVariablesPile.getAdminPass());
        adriver.findElement(By.id("button-submit")).click();
        wait.until(stalenessOf(a));
    }
    public static void charles(WebDriver adriver) {

        String login = "Bad Client";
        String pass = "@TerriblePass01";
        WebDriverWait wait = new WebDriverWait(adriver, java.time.Duration.ofSeconds(10));
        adriver.get(testingVariablesPile.getHost());
        wait.until(presenceOfElementLocated(By.tagName("html")));
        WebElement a =adriver.findElement(By.tagName("html"));
        adriver.findElement(By.id("login")).sendKeys("Bad Client");
        adriver.findElement(By.id("password")).sendKeys("@TerriblePass01");
        adriver.findElement(By.id("button-submit")).click();
        wait.until(stalenessOf(a));
    }
}
