package prtech.com.pokerpulse.endToEnd.tests;
import prtech.com.pokerpulse.endToEnd.base.BaseTest;
import com.microsoft.playwright.Page;
import io.qameta.allure.testng.Tag;
import org.testng.Assert;
import org.testng.annotations.Test;


public class RegisterTest extends BaseTest {

    @Test()
    @Tag("second")
    public void registerTest() throws InterruptedException {
        registerPage.openApp();
        registerPage.navigateToRegisterPage();

        registerPage.enterUsername("testUser");
        registerPage.enterPassword("testPassword");
        registerPage.clickSubmitButton();
           
    }
}
