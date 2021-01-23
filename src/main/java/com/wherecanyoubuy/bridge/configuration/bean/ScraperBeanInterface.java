package com.wherecanyoubuy.bridge.configuration.bean;

import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import org.springframework.beans.factory.DisposableBean;

public interface ScraperBeanInterface extends DisposableBean {
    ScraperInterface getScraper();
}
