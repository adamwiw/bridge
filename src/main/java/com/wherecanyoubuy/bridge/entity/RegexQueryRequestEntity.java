package com.wherecanyoubuy.bridge.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegexQueryRequestEntity extends QueryRequestEntity {
    private RegexQuery regexQuery;
}
