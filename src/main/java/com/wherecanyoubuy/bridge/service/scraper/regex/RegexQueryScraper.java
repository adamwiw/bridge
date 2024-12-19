package com.wherecanyoubuy.bridge.service.scraper.regex;

import com.wherecanyoubuy.bridge.entity.QueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.RegexQueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.SerializableSimpleEntry;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import com.wherecanyoubuy.bridge.service.scraper.ScraperServiceInterface;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class RegexQueryScraper implements ScraperServiceInterface {
    public List<Map<String, String>> scrape(ScraperInterface scraperInterface, QueryRequestEntity queryRequestEntity) {
        Pattern pattern = Pattern.compile(((RegexQueryRequestEntity) queryRequestEntity)
                .getRegexQuery()
                .getQuery());
        Matcher matcher = pattern.matcher(scraperInterface
                .findElement()
                .getText());
        if (matcher.find()) {
            return Collections.singletonList(((RegexQueryRequestEntity) queryRequestEntity)
                    .getRegexQuery()
                    .getRegexQueryFields()
                    .stream()
                    .map(regexQueryField -> new SerializableSimpleEntry<>(regexQueryField.getName(), matcher.group(Integer.parseInt(regexQueryField.getName()))))
                    .collect(Collectors.toMap(SerializableSimpleEntry::getKey, SerializableSimpleEntry::getValue)));
        }
        return Collections.emptyList();
    }
}
