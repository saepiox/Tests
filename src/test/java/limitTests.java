import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import tester.*;

import java.io.IOException;

public class limitTests {
    private static WebDriver driver;
    @Before
    public void setup(){



        testingVariablesPile.setAdminPass("pass");
        testingVariablesPile.setAdminlogin("tj@saepiox.com");
    }
    @Test
    public void create_agreement(){
        driver = new ChromeDriver();
       // System.out.println("attempting creation and destruction of user");
      //  Login.admin(driver);
     //   Agreement.createagreement(driver);
        //GenerateUser.auser(driver);
       Login.admin(driver);
            Positions.uploadTestPositions(driver);

        //GenerateUser.Delete_test_user(driver);
    }
    //@After
    //public void teardown() {
   //     driver.close();
    //}
}


