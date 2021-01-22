package com.wherecanyoubuy.bridge.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ElementQuery {
    private String itemCssSelector;
    private List<ElementQueryField> elementQueryFields;
}
