package io.github.increasecurity.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ConfigurationSource {
    private String fileName;
    private String relativePath;
    private String profile;
    private String fileType;
    private Map<String, String> entries = new LinkedHashMap<>();
}
