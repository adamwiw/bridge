package com.wherecanyoubuy.bridge.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegexQuery {
    private String query;
    private List<RegexQueryField> regexQueryFields;
}
