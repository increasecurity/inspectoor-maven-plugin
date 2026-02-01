package io.github.increasecurity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.increasecurity.model.Project;
import io.github.increasecurity.model.Spec;
import io.github.increasecurity.openapi.OpenApiProcessor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class InspectoorUtil {

    private InspectoorUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static final String POM_FILE = "pom.xml";

    public static String readSBOMLocation(String pomFile) {
        return pomFile.replace(POM_FILE, "target/bom.json");
    }

    public static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        return sb.toString();
    }

    public static boolean isBlank(String text) {
        return text == null || text.trim().length() == 0;
    }

    public static boolean isBlankList(List<?> objects) {
        return objects == null || objects.isEmpty();
    }

    public static String toJson(Project project) {
        String json = "";
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        try {
            json = objectMapper.writeValueAsString(project);
        } catch (JsonProcessingException e) {
            log.error("Error in toJson {}", e.getMessage());
        }
        return json;
    }

    public static List<String> findOpenApiFiles(String srcPath) {
        try {
            return Files.walk(Paths.get(srcPath))
                    .filter(path -> {
                        String filePath = path.toString();
                        return filePath.endsWith(".json") || filePath.endsWith(".yaml") || filePath.endsWith(".yml");
                    })
                    .map(Path::toString)
                    .filter(InspectoorUtil::isValidOpenApiFile).toList();
        } catch (IOException e) {
            log.warn("Something went wrong in findOpenApiFiles {} ", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static boolean isValidOpenApiFile(String fileName) {
        try {
            String content = new String(Files.readAllBytes(new File(fileName).toPath()));

            if (fileName.endsWith(".json")) {
                JSONObject json = new JSONObject(content);
                return json.has("openapi") || json.has("swagger");
            } else {
                Yaml yaml = new Yaml();
                Map<String, Object> yamlMap = yaml.load(content);
                return yamlMap.containsKey("openapi") || yamlMap.containsKey("swagger");
            }
        } catch (Exception e) {
            log.error("Error in isValidOpenApiFile {}", e.getMessage());
            return false;
        }
    }

    public static List<Spec> readSpecFiles(List<String> openApiFiles) {
        log.info("Found openApiFiles = " + openApiFiles.size());
        OpenApiProcessor processor = new OpenApiProcessor();
        return openApiFiles.stream()
                .map(processor::readSpecFile)
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static String adjustSrcPath(String pomFile) {
        String toBeReplaced = "src" + File.separator + "main";
        return pomFile.replace(POM_FILE, toBeReplaced);
    }

    public static byte[] compress(byte[] jsonAsByte) throws IOException {
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(jsonAsByte);
        gzip.close();
        return obj.toByteArray();
    }

}
