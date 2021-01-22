package com.wherecanyoubuy.bridge.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BridgeRequestEntity {
    private String url;
    private ElementQuery elementQuery;
    private String scraperName;
}
