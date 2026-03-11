package com.dentocure.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JPA Converter: stores List<String> as a pipe-separated TEXT column.
 * Example: ["Penicillin", "Aspirin"] ↔ "Penicillin|Aspirin"
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String SEPARATOR = "|";

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(SEPARATOR, list);
    }

    @Override
    public List<String> convertToEntityAttribute(String data) {
        if (data == null || data.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(data.split("\\|")));
    }
}
