package com.wherecanyoubuy.bridge.service.scraper;

import com.wherecanyoubuy.bridge.entity.QueryRequestEntity;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;

import java.util.List;
import java.util.Map;

public interface ScraperServiceInterface {
    List<Map<String, String>> scrape(
            ScraperInterface scraperInterface,
            QueryRequestEntity queryRequestEntity
    );
}
