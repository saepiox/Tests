package tester;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf;

public class GenerateUser {
       private static WebElement a;


    public static void auser(WebDriver adriver) {
        WebElement a = adriver.findElement(By.tagName("iframe"));
        WebDriverWait wait = new WebDriverWait(adriver, java.time.Duration.ofSeconds(10));
        adriver.navigate().to(testingVariablesPile.getHost() + "#!user-admin");
        wait.until(presenceOfElementLocated(By.id("add")));
        adriver.findElement(By.id("add")).click();
        adriver.findElement(By.id("email")).sendKeys("aMail@aMailService.com");
        adriver.findElement(By.id("userName")).sendKeys("Bad Client");
        adriver.findElement(By.id("name")).sendKeys("Charles Ponzi");
        adriver.findElement(By.id("password")).sendKeys("aTerriblePassword" + Keys.TAB + Keys.ARROW_DOWN+ Keys.ENTER + Keys.TAB + "charles" + Keys.ENTER);
        adriver.findElement(By.id("update")).click();
        adriver.findElement(By.id("logout")).click();
        wait.until(stalenessOf(a));
        a = adriver.findElement(By.tagName("html"));
        adriver.navigate().to(testingVariablesPile.getHost() + "#!login");
        adriver.findElement(By.id("login")).sendKeys("Bad Client");
        adriver.findElement(By.id("password")).sendKeys("aTerriblePassword");
        adriver.findElement(By.id("button-submit")).click();
        wait.until(stalenessOf(a));
        adriver.navigate().to(testingVariablesPile.getHost() + "#!dashboard");
        wait.until(presenceOfElementLocated(By.xpath("/html/body/div[2]/div[3]/div/div/div[3]/div/div/div/div[3]/div/input")));
        a = adriver.findElement(By.xpath("/html/body/div[2]/div[3]/div/div/div[3]/div/div/div/div[3]/div/input"));
        a.sendKeys("@TerriblePass01" + Keys.TAB + "@TerriblePass01" + Keys.ENTER);
        wait.until(stalenessOf(a));
        a = adriver.findElement(By.tagName("html"));
        adriver.findElement(By.id("logout")).click();
        wait.until(stalenessOf(a));
        System.out.println("Charles Ponzi added to users");
    }
    public static void Delete_test_user(WebDriver adriver){
        int i = 1;
        WebDriverWait wait = new WebDriverWait(adriver, java.time.Duration.ofSeconds(10));
        adriver.navigate().to(testingVariablesPile.getHost() + "#!user-admin");
        wait.until(presenceOfElementLocated(By.id("add")));
        WebElement a = adriver.findElement(By.tagName("html"));
        while(i !=0 ){
            if (adriver.findElements(By.xpath("/html/body/div[1]/div/div[2]/div[2]/div/div/div/div/div[2]/div/div[1]/div/div[3]/table/tbody/tr["+i+"]/td[3]")).size()>0){
                if (adriver.findElement(By.xpath("/html/body/div[1]/div/div[2]/div[2]/div/div/div/div/div[2]/div/div[1]/div/div[3]/table/tbody/tr["+i+"]/td[3]")).getText().equalsIgnoreCase("charles ponzi")){
                    adriver.findElement(By.xpath("/html/body/div[1]/div/div[2]/div[2]/div/div/div/div/div[2]/div/div[1]/div/div[3]/table/tbody/tr["+i+"]/td[3]")).click();
                    adriver.findElement(By.id("delete")).click();
                    System.out.println("User Charles Ponzi Righeously Deleted");
                i=0;
                }
                else {
                    i++;
                }

        }
            else{
                i=0;
                System.out.println("Failed to find Charles Ponzi");
            }}

            adriver.findElement(By.id("logout")).click();
            wait.until(stalenessOf(a));
        }

        }

