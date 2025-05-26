package io.github.increasecurity;

import io.github.increasecurity.model.Spec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class InspectoorUtilTest {

    final static String SPEC_FILE1 = "src/test/resources/specfiles/api.yaml";
    final static String SPEC_FILE2 = "src/test/resources/specfiles/petstore.yaml";
    final static String SPEC_FILE3 = "src/test/resources/specfiles/petstore2.json";
    final static String SPEC_FILE4 = "src/test/resources/specfiles/petstore3.json";
    final static String SPEC_FILE5 = "src/test/resources/specfiles/swagger2.yaml";
    final static String SPEC_FILE_WRONG_LOCATION = "inspectoor-maven-plugin/src/test/resources/specfiles/petstore2.json";

    @Test
    void extractOpenApiVersions() {
        List<File> apiFiles = new ArrayList<>();
        apiFiles.add(new File(SPEC_FILE1));
        apiFiles.add(new File(SPEC_FILE2));
        apiFiles.add(new File(SPEC_FILE3));
        apiFiles.add(new File(SPEC_FILE4));
        apiFiles.add(new File(SPEC_FILE5));
        List<Spec> specs = InspectoorUtil.extractOpenApiVersions(apiFiles);
        Assertions.assertFalse(specs.isEmpty());
    }

    @Test
    void extractExtractSecuritySchemes() {
        List<File> apiFiles = new ArrayList<>();
        apiFiles.add(new File(SPEC_FILE1));
        List<Spec> specs = InspectoorUtil.extractOpenApiVersions(apiFiles);
        Assertions.assertFalse(specs.isEmpty());
    }

    @Test
    void extractOpenApiVersionsNotFound() {
        List<File> apiFiles = new ArrayList<>();
        apiFiles.add(new File(SPEC_FILE_WRONG_LOCATION));
        List<Spec> specs = InspectoorUtil.extractOpenApiVersions(apiFiles);
        Assertions.assertTrue(specs.isEmpty());
    }

}