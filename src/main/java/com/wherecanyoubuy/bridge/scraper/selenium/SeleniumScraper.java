package com.wherecanyoubuy.bridge.scraper.selenium;

import com.wherecanyoubuy.bridge.scraper.AbstractScraper;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import lombok.Synchronized;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.openqa.selenium.By.ByTagName;

@Deprecated
public class SeleniumScraper extends AbstractScraper {
    private ChromeDriver driver;

    public SeleniumScraper() {
        super(org.slf4j.LoggerFactory.getLogger(SeleniumScraper.class));
    }

    @Override
    @Synchronized
    public void startScraper() {
        super.startScraper();
        // Set the system property for Chrome driver
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        ChromeOptions chromeOptions = new ChromeOptions();
        //chromeOptions.setHeadless(true);
        driver = new ChromeDriver(chromeOptions);
        // Create driver object for CHROME browser
        driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
        driver.manage().window().maximize();
    }

    @Override
    @Synchronized
    public void getUrl(String url) throws IOException {
        super.getUrl(url);
        driver.get(url);
    }

    @Override
    @Synchronized
    public List<ScrapedElementInteface> findElements(String cssQuery) {
        return driver
                .findElementsByCssSelector(cssQuery)
                .stream()
                .map(webElement ->
                        SeleniumElement
                                .builder()
                                .webElement(webElement)
                                .build())
                .collect(Collectors.toList());
    }

    @Override
    @Synchronized
    public ScrapedElementInteface findElement() {
        return SeleniumElement
                .builder()
                .webElement(driver.findElement(new ByTagName("Body")))
                .build();
    }

    @Override
    @Synchronized
    public void quit() {
        super.quit();
        driver.quit();
    }
}
