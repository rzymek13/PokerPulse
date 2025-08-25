package prtech.com.pokerpulse.endToEnd.pages;

import com.microsoft.playwright.Page;

public class RegisterPage {
    private final Page page;
    private final String registerPage = "home-registerPage";
    private final String usernameInput = "register-username";
    private final String passwordInput = "register-password";
    private final String submitButton = "register-submitButton";

    public void openApp() {
        page.navigate("http://localhost:8081/");
    }

    public RegisterPage(Page page) {
        this.page = page;
    }
    public void navigateToRegisterPage() {
        page.getByTestId(registerPage).click();
        
    }
    public void enterUsername(String username) {
        page.getByTestId(usernameInput).fill(username);
    }
    public void enterPassword(String password) {
        page.getByTestId(passwordInput).fill(password);
    }
    public void clickSubmitButton() {
        page.getByTestId(submitButton).click();
    }


    

}
