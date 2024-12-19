package com.wherecanyoubuy.bridge.entity;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class ElementQueryField extends RegexQueryField {
    private String cssQuery;
    private String attributeName;
}
