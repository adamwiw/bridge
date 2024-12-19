package com.wherecanyoubuy.bridge.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequestEntity {
    protected String url;
    protected String scraperName;
}
