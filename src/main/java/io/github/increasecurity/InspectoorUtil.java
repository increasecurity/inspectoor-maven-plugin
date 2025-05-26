package io.github.increasecurity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.increasecurity.model.Project;
import io.github.increasecurity.model.Server;
import io.github.increasecurity.model.Spec;
import io.github.increasecurity.model.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class InspectoorUtil {

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
        return objects == null || objects.size() == 0;
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

    public static List<File> findOpenApiFiles(String srcPath) {

        try {
            return Files.walk(Paths.get(srcPath))
                    .filter(path -> {
                        String filePath = path.toString();
                        return filePath.endsWith(".json") || filePath.endsWith(".yaml") || filePath.endsWith(".yml");
                    })
                    .map(Path::toFile)
                    .filter(InspectoorUtil::isValidOpenApiFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("Something went wrong in findOpenApiFiles {} ", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static boolean isValidOpenApiFile(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));

            if (file.getName().endsWith(".json")) {
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

    public static List<Spec> extractOpenApiVersions(List<File> openApiFiles) {
        List<Spec> specs = new ArrayList<>();

        for (File file : openApiFiles) {
            Spec spec = detectApiSpecType(file);
            if (spec != null) {
                spec.setName(file.getName());
                spec.setLocation(file.getAbsolutePath());
                specs.add(spec);
            }
        }

        return specs;
    }

    private static List<SecurityScheme> extractSecuritySchemes(Object root) {
        Map<String, Object> map = (Map<String, Object>) root;

        Map<String, Object> componentsOrSecurityDefinitions = null;

        if (map.containsKey("components")) {
            componentsOrSecurityDefinitions = (Map<String, Object>) map.get("components");
        } else if (map.containsKey("securityDefinitions")) {
            componentsOrSecurityDefinitions = map; // swagger 2.0
        }

        if (componentsOrSecurityDefinitions == null) return Collections.emptyList();

        Map<String, Object> securityMap = (Map<String, Object>) (
                componentsOrSecurityDefinitions.containsKey("securitySchemes")
                        ? componentsOrSecurityDefinitions.get("securitySchemes")  // OAS 3.x
                        : componentsOrSecurityDefinitions.get("securityDefinitions")  // Swagger 2.0
        );

        if (securityMap == null) return Collections.emptyList();

        List<SecurityScheme> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : securityMap.entrySet()) {
            Map<String, Object> scheme = (Map<String, Object>) entry.getValue();
            try {
                result.add(SecuritySchemeFactory.create(scheme));
            } catch (Exception e) {
                log.warn("Could not parse SecurityScheme: {}", entry.getKey());
            }
        }
        return result;
    }

    private static Spec detectApiSpecType(File file) {
        Spec spec = new Spec();
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] compressedByte = compress(bytes);
            spec.setContent(new String(Base64.getEncoder().encode(compressedByte)));
            String content = new String(bytes);

            if (file.getName().endsWith(".json")) {
                JSONObject json = new JSONObject(content);
                if (json.has("openapi")) {
                    spec.setType("OpenAPI");
                    spec.setVersion(json.getString("openapi"));
                    JSONObject infoMap = (JSONObject) json.get("info");
                    spec.setInfo_version(extractInfoVersion(infoMap));
                    List<Server> servers = extractServers(json);
                    spec.getServers().addAll(servers);
                    spec.setSecuritySchemes(extractSecuritySchemes(json.toMap()));
                } else if (json.has("swagger")) {
                    spec.setType("swagger");
                    spec.setVersion(json.getString("swagger"));
                    JSONObject infoMap = (JSONObject) json.get("info");
                    spec.setInfo_version(extractInfoVersion(infoMap));
                    List<Server> servers = extractServers(json);
                    spec.getServers().addAll(servers);
                    spec.setSecuritySchemes(extractSecuritySchemes(json.toMap()));
                }
            } else {
                Yaml yaml = new Yaml();
                Map<String, Object> yamlMap = yaml.load(content);
                LinkedHashMap infoMap = (LinkedHashMap) yamlMap.get("info");
                spec.setInfo_version(extractInfoVersion(infoMap));
                if (yamlMap.containsKey("openapi")) {
                    spec.setType("OpenAPI");
                    spec.setVersion(yamlMap.get("openapi").toString());
                    List<Server> servers = extractServers(yamlMap);
                    spec.getServers().addAll(servers);
                    spec.setSecuritySchemes(extractSecuritySchemes(yamlMap));
                } else if (yamlMap.containsKey("swagger")) {
                    spec.setType("swagger");
                    spec.setVersion(yamlMap.get("swagger").toString());
                    List<Server> servers = extractServers(yamlMap);
                    spec.getServers().addAll(servers);
                    spec.setSecuritySchemes(extractSecuritySchemes(yamlMap));
                }
            }
        } catch (Exception ex) {
            log.error("File not found and will be ignored {}", ex.getMessage());
            spec = null;
        }

        return spec;
    }

    public static String adjustSrcPath(String pomFile) {
        String toBeReplaced = "src" + File.separator + "main";
        return pomFile.replace(POM_FILE, toBeReplaced);
    }

    private static List<Server> extractServers(Map<String, Object> yamlMap) {
        List<Server> servers = new ArrayList<>();
        Object serversObject = yamlMap.get("servers");
        if (serversObject != null) {
            List serverList = (ArrayList) serversObject;

            serverList.forEach(item -> {
                HashMap map = (HashMap) item;
                Server server = new Server();
                server.setUrl(extractValue(map.get("url")));
                server.setDescription(extractValue(map.get("description")));
                servers.add(server);
            });

        }

        return servers;
    }

    private static String extractValue(Object url) {
        return url != null ? url.toString() : null;
    }

    private static List<Server> extractServers(JSONObject json) {
        List<Server> servers = new ArrayList<>();
        try {
            Object serverObject = json.get("servers");
            if (serverObject != null) {
                JSONArray jsonArray = (JSONArray) serverObject;
                for (int i = 0; i < jsonArray.length(); i++) {
                    Server server = new Server();
                    JSONObject item = jsonArray.getJSONObject(i);
                    String url = item.optString("url", "No URL");
                    server.setUrl(url);
                    String description = item.optString("description", "No Description");
                    if (description != null) {
                        server.setDescription(description);
                    }

                    servers.add(server);
                }

            }
        } catch (Exception e) {
            log.error("Error in extractServers {}", e.getMessage());
        }

        return servers;
    }

    private static String extractInfoVersion(LinkedHashMap infoMap) {
        if (infoMap == null)
            return null;
        return infoMap.get("version").toString();
    }

    private static String extractInfoVersion(JSONObject jsonObject) {
        if (jsonObject == null)
            return null;
        return jsonObject.get("version").toString();
    }

    public static byte[] compress(byte[] jsonAsByte) throws IOException {
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(jsonAsByte);
        gzip.close();
        return obj.toByteArray();
    }

}
