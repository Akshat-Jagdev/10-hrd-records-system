package com.hrd.records.common;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable page envelope for list endpoints (independent of Spring's Page JSON shape). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
