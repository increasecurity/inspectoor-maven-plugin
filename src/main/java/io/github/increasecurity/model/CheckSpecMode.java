package io.github.increasecurity.model;

public enum CheckSpecMode {
    NONE, WARN, FAIL;

    public static CheckSpecMode fromString(String value) {
        return value == null ? NONE : CheckSpecMode.valueOf(value.trim().toUpperCase());
    }
}
