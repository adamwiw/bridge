package com.wherecanyoubuy.bridge.scraper.selenium;

import com.wherecanyoubuy.bridge.scraper.AbstractScraper;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import jodd.jerry.Jerry;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.MethodNotAllowedException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SeleniumJerryScraper extends AbstractScraper {
    private ChromeDriver driver;

    public SeleniumJerryScraper() {
        super(LoggerFactory.getLogger(SeleniumJerryScraper.class));
    }

    @Override
    public void startScraper(ChromeOptions chromeOptions) {
        super.startScraper(chromeOptions);
        // Set the system property for Chrome driver
        System.setProperty("webdriver.chrome.driver", "src/main/resources/driver/chromedriver.exe");
        driver = new ChromeDriver();

        // Create driver object for CHROME browser
        driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
        driver.manage().window().maximize();
    }

    @Override
    public void getUrl(String url) throws IOException {
        super.getUrl(url);
        driver.get(url);
    }

    @Override
    public List<ScrapedElementInteface> findElements(String cssQuery) {
        return driver
                .findElements(By.cssSelector(cssQuery))
                .stream()
                .map(webElement -> JerryElement
                        .builder()
                        .jerry(Jerry.of(
                                webElement.getAttribute("outerHTML")))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public ScrapedElementInteface findElement() {
        throw new RuntimeException();
    }

    @Override
    public String getPageSource() {
        return null;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public int getStatusCode() {
        return -1;
    }

    @Override
    public void quit() {
        super.quit();
        driver.quit();
    }
}
