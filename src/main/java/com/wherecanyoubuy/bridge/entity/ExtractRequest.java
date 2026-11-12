package com.wherecanyoubuy.bridge.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractRequest {
    private String id;
    private ElementQuery elementQuery;
    private RegexQuery regexQuery;
}
