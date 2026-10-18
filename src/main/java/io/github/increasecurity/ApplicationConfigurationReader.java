package io.github.increasecurity;

import io.github.increasecurity.model.ConfigurationSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApplicationConfigurationReader {
    private static final Pattern FILE_NAME = Pattern.compile("application(?:-([^.]+))?\\.(properties|ya?ml)");
    private static final String REDACTED = "[REDACTED]";

    public List<ConfigurationSource> read(Path projectBaseDirectory) {
        Path resources = projectBaseDirectory.resolve("src/main/resources");
        if (!Files.isDirectory(resources)) {
            return Collections.emptyList();
        }

        try (var paths = Files.list(resources)) {
            return paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(path -> readFile(resources, path))
                    .flatMap(java.util.Optional::stream)
                    .toList();
        } catch (IOException ex) {
            return Collections.emptyList();
        }
    }

    private java.util.Optional<ConfigurationSource> readFile(Path resources, Path path) {
        Matcher matcher = FILE_NAME.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            return java.util.Optional.empty();
        }

        try {
            ConfigurationSource source = new ConfigurationSource();
            source.setFileName(path.getFileName().toString());
            source.setRelativePath(resources.relativize(path).toString());
            source.setProfile(matcher.group(1));
            source.setFileType(matcher.group(2).equals("properties") ? "properties" : "yaml");
            Map<String, String> entries = source.getEntries();
            if (source.getFileType().equals("properties")) {
                readProperties(path, entries);
            } else {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    flatten(new Yaml().load(reader), "", entries);
                }
            }
            return java.util.Optional.of(source);
        } catch (Exception ex) {
            return java.util.Optional.empty();
        }
    }

    private void readProperties(Path path, Map<String, String> entries) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        properties.stringPropertyNames().stream().sorted()
                .forEach(key -> entries.put(key, safeValue(key, properties.getProperty(key))));
    }

    private void flatten(Object value, String prefix, Map<String, String> entries) {
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, child) -> flatten(child, join(prefix, String.valueOf(key)), entries));
        } else if (value instanceof List<?> list) {
            for (int index = 0; index < list.size(); index++) {
                flatten(list.get(index), prefix + "[" + index + "]", entries);
            }
        } else if (!prefix.isEmpty() && value != null) {
            entries.put(prefix, safeValue(prefix, String.valueOf(value)));
        }
    }

    private String join(String prefix, String key) {
        return prefix.isEmpty() ? key : prefix + "." + key;
    }

    private String safeValue(String key, String value) {
        String normalized = key.toLowerCase().replace("_", "-");
        return normalized.matches(".*(password|secret|token|api-key|apikey|credential|private-key).*")
                ? REDACTED : value;
    }
}
