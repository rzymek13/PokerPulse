package prtech.com.pokerpulse.endToEnd.base;

import com.microsoft.playwright.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import prtech.com.pokerpulse.endToEnd.pages.RegisterPage;
import prtech.com.pokerpulse.endToEnd.utilities.ConfigLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BaseTest {

    protected ThreadLocal<Playwright> playwright = ThreadLocal.withInitial(Playwright::create);
    protected ThreadLocal<Browser> browser = new ThreadLocal<>();
    protected ThreadLocal<Page> page = new ThreadLocal<>();
    private ThreadLocal<BrowserContext> context = new ThreadLocal<>();

    protected RegisterPage registerPage;

    

    @BeforeMethod
    public void setUp() {
        ConfigLoader.getAllProperties();
        String browserName = ConfigLoader.getProperty("browserName");
        if (browserName == null || browserName.isBlank()) browserName = "chrome";
        boolean isHeadlessModeEnabled = Boolean.parseBoolean(ConfigLoader.getProperty("headless"));
        
        System.out.println("Browser name: " + browserName);
        Playwright pw = playwright.get();
        if ("edge".equalsIgnoreCase(browserName)) {
            browser.set(pw.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(isHeadlessModeEnabled)
                    .setChannel("msedge")));
        } else if ("chrome".equalsIgnoreCase(browserName)) {
            browser.set(pw.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(isHeadlessModeEnabled)));
        } else if ("firefox".equalsIgnoreCase(browserName)) {
            browser.set(pw.firefox().launch(new BrowserType.LaunchOptions()
                    .setHeadless(isHeadlessModeEnabled)));
        } else {
            // default fallback
            browser.set(pw.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(isHeadlessModeEnabled)));
        }
        context.set(browser.get().newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setRecordVideoDir(Paths.get("src/test/resources/videos"))
                .setRecordVideoSize(1920,1080)));

        context.get().tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page.set(context.get().newPage());

        registerPage = new RegisterPage(page.get());


        
    }
    @AfterMethod()
    public void clearCookiesAndCache() {context.get().clearCookies();}

    @AfterMethod
    public void tearDown(ITestResult result) throws IOException {

        if (result.getStatus() == ITestResult.FAILURE) {
            String formattedDateTime = formatName();

            String resultName = result.getName() + "_" + "_" + formattedDateTime;
            Path screenshotPath = Paths.get("./target/screenshots/" + resultName + ".png");

            page.get().screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
            page.get().close();
            page.get().video().saveAs(Paths.get("./target/videos/" + resultName + ".webm"));
            attachScreenshotToAllureReport(resultName, screenshotPath);
            context.get().tracing().stop(new Tracing.StopOptions()
                    .setPath(Paths.get("trace.zip")));


        }
        context.get().close();
        browser.get().close();
    }

    private static String formatName() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        return formatter.format(currentDateTime);
    }

    @Attachment(type = "image/png")
    private void attachScreenshotToAllureReport(String resultName, Path screenshotPath) throws IOException {
        Allure.addAttachment(resultName, new ByteArrayInputStream(Files.readAllBytes(screenshotPath)));
    }
}
