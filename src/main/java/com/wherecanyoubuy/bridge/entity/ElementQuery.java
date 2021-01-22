package com.wherecanyoubuy.bridge.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ElementQuery {
    private String itemCssQuery;
    private List<ElementQueryField> elementQueryFields;
}
