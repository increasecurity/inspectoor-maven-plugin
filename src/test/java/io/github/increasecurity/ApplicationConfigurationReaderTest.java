package io.github.increasecurity;

import io.github.increasecurity.model.ConfigurationSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ApplicationConfigurationReaderTest {

    @Test
    void readsPropertiesConfiguration(@TempDir Path projectDirectory) throws Exception {
        Path resources = projectDirectory.resolve("src/main/resources");
        Files.createDirectories(resources);
        Files.writeString(resources.resolve("application-dev.properties"), """
                feature.enabled=true
                api.token=plain-token
                greeting=hello world
                """);

        List<ConfigurationSource> sources = new ApplicationConfigurationReader().read(projectDirectory);

        assertEquals(1, sources.size());
        ConfigurationSource source = sources.get(0);
        assertEquals("application-dev.properties", source.getFileName());
        assertEquals("application-dev.properties", source.getRelativePath());
        assertEquals("dev", source.getProfile());
        assertEquals("properties", source.getFileType());
        assertEquals("true", source.getEntries().get("feature.enabled"));
        assertEquals("[REDACTED]", source.getEntries().get("api.token"));
        assertEquals("hello world", source.getEntries().get("greeting"));
        assertFalse(source.getEntries().containsValue("plain-token"));
    }
}
