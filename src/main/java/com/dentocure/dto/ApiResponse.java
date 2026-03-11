package com.dentocure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard response envelope for all API responses.
 * Shape: { "data": {...}, "meta": {...} }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final T data;
    private final Object meta;

    private ApiResponse(T data, Object meta) {
        this.data = data;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> of(T data, Object meta) {
        return new ApiResponse<>(data, meta);
    }

    public T getData() { return data; }
    public Object getMeta() { return meta; }
}
