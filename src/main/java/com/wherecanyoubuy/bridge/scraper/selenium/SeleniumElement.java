package com.wherecanyoubuy.bridge.scraper.selenium;

import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import lombok.Builder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

@Deprecated
@Builder
public class SeleniumElement implements ScrapedElementInteface {
    private WebElement webElement;

    @Override
    public String getAttribute(String attributeName) {
        return webElement.getAttribute(attributeName);
    }

    @Override
    public String getText() {
        return webElement.getText();
    }

    @Override
    public List<ScrapedElementInteface> findElements(String selector) {
        return webElement.findElements(By.cssSelector(selector))
                .stream()
                .map(webElement1 ->
                        SeleniumElement
                                .builder()
                                .webElement(webElement1)
                                .build())
                .collect(Collectors.toList());
    }
}