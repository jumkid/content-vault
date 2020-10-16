package com.jumkid.vault.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jumkid.vault.exception.InvalidFieldException;
import lombok.Getter;

@Getter
public enum MediaFilePropType {

    INTEGER("integer"), STRING("string"), BOOLEAN("boolean"), DATETIME("datetime"), FLOAT("float");

    MediaFilePropType(String value) { this.value = value; }

    @JsonValue
    private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static MediaFilePropType fromValue(String value) {
        for (MediaFilePropType e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException(value);
    }

}
