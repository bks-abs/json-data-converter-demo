package org.bks.po;

public enum FieldType {
    STRING,
    BOOLEAN,
    INTEGER,
    DOUBLE,
    OBJECT,
    ARRAY;

    public static boolean isBasicType(FieldType type) {
        return type == STRING || type == BOOLEAN || type == INTEGER || type == DOUBLE;
    }
}
