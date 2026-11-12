package com.wherecanyoubuy.bridge.scraper.cdp;

import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.stream.Collectors;

public class CdpElement implements ScrapedElementInteface {
    private final Element element;

    public CdpElement(Element element) {
        this.element = element;
    }

    @Override
    public String getAttribute(String attributeName) {
        if (attributeName == null || element == null) {
            return null;
        }
        return switch (attributeName.toLowerCase()) {
            case "innertext", "text" -> getText();
            case "innerhtml" -> element.html();
            case "outerhtml" -> element.outerHtml();
            default -> element.hasAttr(attributeName) ? element.attr(attributeName) : null;
        };
    }

    @Override
    public String getText() {
        return element == null ? "" : element.text();
    }

    @Override
    public List<ScrapedElementInteface> findElements(String cssQuery) {
        if (element == null) {
            return List.of();
        }
        return element.select(cssQuery).stream()
                .map(CdpElement::new)
                .collect(Collectors.toList());
    }
}
