package com.wherecanyoubuy.bridge.handler;

import com.wherecanyoubuy.bridge.entity.BridgeRequestEntity;
import com.wherecanyoubuy.bridge.entity.SerializableSimpleEntry;
import com.wherecanyoubuy.bridge.service.ScraperService;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Builder
//@RestController
public class BridgeController {
    /*@Autowired
    private ScraperService scraperService;

    @PostMapping(value = "/search", consumes = "application/json", produces = "application/json")
    public List<List<SerializableSimpleEntry<String, String>>> search(
            @RequestBody BridgeRequestEntity bridgeRequestEntity) {
        return scraperService.search(bridgeRequestEntity);
    }*/
}
