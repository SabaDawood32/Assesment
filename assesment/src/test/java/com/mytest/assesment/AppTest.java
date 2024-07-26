package com.mytest.assesment;

import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.time.Duration;


public class AppTest {

    private static WebDriver driver;
    private static FluentWait<WebDriver> wait;
    private static String previousTransactionReference = "";

    @BeforeClass
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(20))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testCardPayments() throws Exception {
        driver.get("https://demo.dev.tap.company/v2/sdk/card");

        selectDropdownOption(By.id("currency"), "BHD");
        selectDropdownOption(By.id("scope"), "AuthenticatedToken");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));

        // Perform the first transaction and store its reference
        performTransaction("5123450000000008", "01/39", "100");
      
        String newTransactionReference = getTransactionReference();
        String newLast8Digits = extractLast8Digits(newTransactionReference);

        // Validate the change in last 8 digits
        validateTransactionReferenceChange(previousTransactionReference, newLast8Digits);
    }
    @Test
    public void testCardPayments1() throws Exception {
        driver.get("https://demo.dev.tap.company/v2/sdk/card");

        selectDropdownOption(By.id("currency"), "BHD");
        selectDropdownOption(By.id("scope"), "AuthenticatedToken");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));

        // Perform the second transaction and verify the change
        performTransaction("4508750015741019", "01/39", "100");
        String newTransactionReference = getTransactionReference();
        String newLast8Digits = extractLast8Digits(newTransactionReference);

        // Validate the change in last 8 digits
        validateTransactionReferenceChange(previousTransactionReference, newLast8Digits);
    }

    private void selectDropdownOption(By dropdownLocator, String value) {
        WebElement dropdown = driver.findElement(dropdownLocator);
        dropdown.click();
        WebElement option = driver.findElement(By.xpath("//option[@value='" + value + "']"));
        option.click();
    }

    private void performTransaction(String cardNumber, String expiry, String cvv) throws Exception {
        fillCardDetailsAndSubmit(cardNumber, expiry, cvv);
    }

    private void fillCardDetailsAndSubmit(String cardNumber, String expiry, String cvv) throws Exception {
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("tap-card-iframe")));

        WebElement cardNumberField = driver.findElement(By.id("card_input_mini"));
        cardNumberField.sendKeys(cardNumber);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        WebElement expiryInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("date_input")));
        scrollToElement(expiryInput);
        expiryInput.sendKeys(expiry);

        WebElement cvvInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cvv_input")));
        scrollToElement(cvvInput);
        cvvInput.sendKeys(cvv);

        driver.switchTo().defaultContent();

        WebElement generateTokenButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"root\"]/section/article/div[3]/button[1]")));
        scrollToElement(generateTokenButton);
        generateTokenButton.click();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(60));

        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("tap-card-iframe")));
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("tap-card-iframe-authentication")));
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("challengeFrame")));
        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("acssubmit")));
        scrollToElement(submitButton);
        submitButton.sendKeys(Keys.ENTER);
        driver.switchTo().defaultContent();

        WebElement copyButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@data-label=\"copy button\"]")));
        scrollToElement(copyButton);
        copyButton.click();
    }

    private String getTransactionReference() throws Exception {
        String copiedJson = getClipboardContents();
        JSONObject jsonObject = new JSONObject(copiedJson);
        return jsonObject.getString("transactionReference");
    }

    private String extractLast8Digits(String transactionReference) {
        if (transactionReference.length() < 8) {
            throw new IllegalArgumentException("Transaction reference must be at least 8 characters long.");
        }
        return transactionReference.substring(transactionReference.length() - 8);
    }

    private void validateTransactionReferenceChange(String oldLast8Digits, String newLast8Digits) {
        if (!oldLast8Digits.equals(newLast8Digits)) {
            System.out.println("Last 8 digits have changed: " + newLast8Digits);
        } else {
            System.err.println("Last 8 digits have not changed: " + newLast8Digits);
        }
    }

    private void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    private static String getClipboardContents() throws Exception {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return (String) contents.getTransferData(DataFlavor.stringFlavor);
        }
        return null;
    }
}