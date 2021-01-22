package com.wherecanyoubuy.bridge.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ElementQueryField {
    private String name;
    private String cssQuery;
    private boolean isAttribute;
    private String attributeName;
    private String queryResult;
}
