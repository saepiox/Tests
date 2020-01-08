package tester;



import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.commons.lang3.EnumUtils;

import java.io.File;
import java.io.IOException;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf;

public class UserSettings{
    static Integer i = 1;
    public static void Navigate_to_user_settings(WebDriver adriver) {
        WebDriverWait wait = new WebDriverWait(adriver, 10);
        adriver.navigate().to(testingVariablesPile.getHost() + "#!user-settings");
        wait.until(presenceOfElementLocated(By.id("dateStylePicker")));
    }

    public static void take_screenshot_user_settings(WebDriver adriver){
        File screenshot = ((TakesScreenshot)adriver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(screenshot, new File("C:\\screenshots\\"+ testingVariablesPile.getWebbrowser() +"_user_settings.jpg"));
        }
        catch (IOException e){

        }}
public static void dateCheck(WebDriver adriver){
        WebDriverWait wait = new WebDriverWait(adriver, 10);
        adriver.get(testingVariablesPile.getHost()+"#!user-settings");
        wait.until(presenceOfElementLocated(By.id("dateStylePicker")));
        WebElement a= adriver.findElement(By.xpath("//*[@id=\"dateStylePicker\"]/span[2]/label"));
        a.click();
        adriver.navigate().to(testingVariablesPile.getHost()+"#!positions-admin");
        wait.until(presenceOfElementLocated(By.xpath("//*[@id=\"dateTo\"]/input")));
    while (adriver.findElements(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+i+"]/td[1]")).size() > 0){
        if(adriver.findElement(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+i+"]/td[1]")).getText().length()>6){

            i++;
        }
      else{
          break;
        }
    }
        for(int j=1; j<i; j++){

          if(!EnumUtils.isValidEnumIgnoreCase(testingVariablesPile.months3Letter.class,adriver.findElement(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+j+"]/td[1]")).getText().replaceAll(",.*", ""))){
              System.out.println("Test Failure, position admin date formate not correct");

          }


        }

    adriver.get(testingVariablesPile.getHost()+"#!user-settings");
    wait.until(presenceOfElementLocated(By.id("dateStylePicker")));
    a= adriver.findElement(By.xpath("//*[@id=\"dateStylePicker\"]/span[1]/label"));
    a.click();
    adriver.navigate().to(testingVariablesPile.getHost()+"#!positions-admin");
    wait.until(presenceOfElementLocated(By.xpath("//*[@id=\"dateTo\"]/input")));
    while (adriver.findElements(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+i+"]/td[1]")).size() > 0){
        if(adriver.findElement(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+i+"]/td[1]")).getText().length()>6){

            i++;
        }
        else{
            break;
        }
    }
    for(int j=1; j<i; j++){

        if(!EnumUtils.isValidEnumIgnoreCase(testingVariablesPile.months.class,adriver.findElement(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+j+"]/td[1]")).getText().replaceAll(",.*", ""))){
            System.out.println("Test Failure, position admin date formate not correct");

        }


    }
    adriver.get(testingVariablesPile.getHost()+"#!user-settings");
    wait.until(presenceOfElementLocated(By.id("dateStylePicker")));
    a= adriver.findElement(By.xpath("//*[@id=\"dateStylePicker\"]/span[3]/label"));
    a.click();
    adriver.navigate().to(testingVariablesPile.getHost()+"#!positions-admin");
    wait.until(presenceOfElementLocated(By.xpath("//*[@id=\"dateTo\"]/input")));
    while (adriver.findElements(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+i+"]/td[1]")).size() > 0){
        if(adriver.findElement(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+i+"]/td[1]")).getText().length()>6){

            i++;
        }
        else{
            break;
        }
    }
    for(int j=1; j<i; j++){

        if(!(adriver.findElement(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+j+"]/td[1]")).getText().charAt(4)=='-')){
            System.out.println("Test Failure, position admin date formate not correct");

        }


    }
    adriver.get(testingVariablesPile.getHost()+"#!user-settings");
    wait.until(presenceOfElementLocated(By.id("dateStylePicker")));
    a= adriver.findElement(By.xpath("//*[@id=\"dateStylePicker\"]/span[4]/label"));
    a.click();
    adriver.navigate().to(testingVariablesPile.getHost()+"#!positions-admin");
    wait.until(presenceOfElementLocated(By.xpath("//*[@id=\"dateTo\"]/input")));
    while (adriver.findElements(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+i+"]/td[1]")).size() > 0){
        if(adriver.findElement(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+i+"]/td[1]")).getText().length()>6){

            i++;
        }
        else{
            break;
        }
    }
    for(int j=1; j<i; j++){

        if(!(adriver.findElement(By.xpath("//*[@id=\"content\"]/div/div/div/div[2]/div/div[3]/div[3]/table/tbody/tr["+j+"]/td[1]")).getText().charAt(2)=='-')){
            System.out.println("Test Failure, position admin date formate not correct");

        }


    }
}}