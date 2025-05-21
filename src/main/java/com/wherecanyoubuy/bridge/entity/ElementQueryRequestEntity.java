package com.wherecanyoubuy.bridge.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;

import java.net.Proxy;
import java.nio.Buffer;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElementQueryRequestEntity extends QueryRequestEntity {
    private ElementQuery elementQuery;
}
