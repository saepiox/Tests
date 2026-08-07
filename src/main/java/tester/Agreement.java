package tester;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ByIdOrName;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.security.Key;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.support.ui.ExpectedConditions.*;

public class Agreement {


    public static void createagreement(WebDriver adriver){

            adriver.navigate().to(testingVariablesPile.getHost()+"#!agreement");
            WebDriverWait wait = new WebDriverWait(adriver, java.time.Duration.ofSeconds(10));
            WebElement name = wait.until(presenceOfElementLocated(By.id("name")));

            if (name.isEnabled()) {
                System.out.println("name field improperly accessible");
            } else {
                System.out.println("Name field locked, as intended");
            }


            adriver.findElement(By.id("add")).click();
            name.sendKeys("charles" + Keys.TAB + "ASSET_MANAGER" + Keys.ENTER + Keys.ENTER + Keys.TAB + "DKK" + Keys.ENTER +  Keys.TAB + "3"+Keys.TAB+"5");
            WebElement a = adriver.findElement(By.id("update"));
            a.click();

            adriver.findElement(By.id("search")).sendKeys("charles"+Keys.ENTER);
            wait.until(presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div[2]/div[2]/div/div/div/div/div[2]/div/div[1]/div/div[3]/table/tbody/tr[1]/td[1]")));
            WebElement acell = adriver.findElement(By.xpath("/html/body/div[1]/div/div[2]/div[2]/div/div/div/div/div[2]/div/div[1]/div/div[3]/table/tbody/tr[1]/td[1]"));
            if(acell.getText().equalsIgnoreCase("charles")){
                System.out.println("agreement succesfully created");
            }
            else{
                System.out.println("agrement creation failure");
            }

            adriver.findElement(By.id("logout")).click();

            wait.until(stalenessOf(a));
        }






    public static void createAndDestroyAgreement(WebDriver adriver){
            adriver.navigate().to(testingVariablesPile.getHost()+"#!agreement");
            WebDriverWait wait = new WebDriverWait(adriver, java.time.Duration.ofSeconds(10));
            WebElement name = wait.until(presenceOfElementLocated(By.id("name")));

            if (name.isEnabled()) {
                System.out.println("name field improperly accessible");
            } else {
                System.out.println("Name field locked, as intended");
            }


            adriver.findElement(By.id("add")).click();
            name.sendKeys("charles" + Keys.TAB + "ASSET_MANAGER" + Keys.ENTER + Keys.ENTER + Keys.TAB + "DKK" + Keys.ENTER +  Keys.TAB + "3"+Keys.TAB+"5");
            WebElement a = adriver.findElement(By.id("update"));
            a.click();

            adriver.findElement(By.id("search")).sendKeys("charles"+Keys.ENTER);
            wait.until(presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div[2]/div[2]/div/div/div/div/div[2]/div/div[1]/div/div[3]/table/tbody/tr[1]/td[1]")));
            WebElement acell = adriver.findElement(By.xpath("/html/body/div[1]/div/div[2]/div[2]/div/div/div/div/div[2]/div/div[1]/div/div[3]/table/tbody/tr[1]/td[1]"));
            if(acell.getText().equalsIgnoreCase("charles")){
                System.out.println("agreement succesfully created");
                adriver.findElement(By.id("delete")).click();
                System.out.println("and deleted");
            }
            else{
                System.out.println("agrement creation failure");
            }

            adriver.findElement(By.id("logout")).click();

            wait.until(stalenessOf(a));
        }

    }
