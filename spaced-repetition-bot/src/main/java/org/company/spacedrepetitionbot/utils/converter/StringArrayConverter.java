package org.company.spacedrepetitionbot.utils.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringArrayConverter implements AttributeConverter<String[], String> {
    private static final String SPLIT_CHAR = ";";

    @Override
    public String convertToDatabaseColumn(String[] strings) {
        return strings != null ? String.join(SPLIT_CHAR, strings) : null;
    }

    @Override
    public String[] convertToEntityAttribute(String s) {
        return s != null ? s.split(SPLIT_CHAR) : new String[0];
    }
}
