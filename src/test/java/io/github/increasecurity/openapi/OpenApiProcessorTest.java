package io.github.increasecurity.openapi;

import io.github.increasecurity.model.Spec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OpenApiProcessorTest {

    OpenApiProcessor processor = new OpenApiProcessor();

    @Test
    void readSpecFile() {
        Spec spec = processor.readSpecFile("src/test/resources/specfiles/user_api.yaml");
        Assertions.assertNotNull(spec);
        Assertions.assertTrue(spec.getName().equals("user_api.yaml"));
    }

    @Test
    void readSpecFileOnline() {
        Spec spec = processor.readSpecFile("https://petstore3.swagger.io/api/v3/openapi.json");
        Assertions.assertNotNull(spec);
        Assertions.assertTrue(spec.getName().equals("openapi.json"));
    }

    @Test
    void testLoadResolvedOpenApiAsYamlJson() {
        String yaml = processor.loadResolvedOpenApiAsYaml("https://petstore3.swagger.io/api/v3/openapi.json");
        Assertions.assertNotNull(yaml);
        Assertions.assertTrue(yaml.startsWith("openapi:"));
    }

    @Test
    void testLoadResolvedOpenApiAsYamlYaml() {
        String yaml = processor.loadResolvedOpenApiAsYaml("https://petstore3.swagger.io/api/v3/openapi.yaml");
        Assertions.assertNotNull(yaml);
        Assertions.assertTrue(yaml.startsWith("openapi:"));
    }


}