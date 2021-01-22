package com.wherecanyoubuy.bridge.scraper.selenium;

import com.wherecanyoubuy.bridge.scraper.AbstractScraper;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import com.wherecanyoubuy.bridge.scraper.jsoup.JsoupElement;
import org.jsoup.Jsoup;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SeleniumJsoupScraper extends AbstractScraper {
    private ChromeDriver driver;

    public SeleniumJsoupScraper() {
        super(org.slf4j.LoggerFactory.getLogger(SeleniumJsoupScraper.class));
        // Set the system property for Chrome driver
        System.setProperty("webdriver.chrome.driver", "src/main/resources/driver/chromedriver.exe");
        driver = new ChromeDriver();

        // Create driver object for CHROME browser
        driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
        driver.manage().window().maximize();
    }

    @Override
    public void getUrl(String url) {
        String message = "GET: " + url;
        log.info(message);
        driver.get(url);
    }

    @Override
    public List<ScrapedElementInteface> findElements(String selector) {
        return driver
                .findElementsByCssSelector(selector)
                .stream()
                .map(webElement -> JsoupElement
                        .builder()
                        .element(Jsoup.parseBodyFragment(
                                webElement.getAttribute("outerHTML")))
                        .build())
                .collect(Collectors.toList());
    }

    public void quit() {
        log.info("Shutting down");
        driver.quit();
    }
}
