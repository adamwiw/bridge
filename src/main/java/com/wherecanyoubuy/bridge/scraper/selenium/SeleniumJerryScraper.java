package com.wherecanyoubuy.bridge.scraper.selenium;

import com.wherecanyoubuy.bridge.scraper.AbstractScraper;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import jodd.jerry.Jerry;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SeleniumJerryScraper extends AbstractScraper {
    private ChromeDriver driver;

    public SeleniumJerryScraper() {
        super(LoggerFactory.getLogger(SeleniumJerryScraper.class));
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
        List<ScrapedElementInteface> list = driver
                .findElementsByCssSelector(cssQuery)
                .stream()
                .map(webElement -> JerryElement
                        .builder()
                        .jerry(Jerry.of(
                                webElement.getAttribute("outerHTML")))
                        .build())
                .collect(Collectors.toList());
        isBusy = false;
        return list;
    }

    @Override
    public boolean isBusy() {
        return isBusy;
    }

    @Override
    public void quit() {
        super.quit();
        driver.quit();
    }
}
