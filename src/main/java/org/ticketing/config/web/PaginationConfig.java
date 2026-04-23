package org.ticketing.config.web;

import java.util.List;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Setter
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationConfig {
    private List<Integer> allowedSizes = List.of(10, 30, 50);
    private Integer defaultSize = 10;

    public int getValidatedSize(int inputSize) {
        if (allowedSizes == null || !allowedSizes.contains(inputSize)) {
            return defaultSize;
        }
        return inputSize;
    }
}
