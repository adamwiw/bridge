package com.wherecanyoubuy.bridge.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class RegexQuery {
    private String query;
    private List<RegexQueryField> regexQueryFields;
}
