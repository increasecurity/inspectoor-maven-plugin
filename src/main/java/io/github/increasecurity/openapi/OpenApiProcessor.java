package io.github.increasecurity.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.github.increasecurity.InspectoorPluginException;
import io.github.increasecurity.InspectoorUtil;
import io.github.increasecurity.model.Server;
import io.github.increasecurity.model.Spec;
import io.github.increasecurity.model.security.SecurityScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class OpenApiProcessor {


    public static final String SWAGGER_VALUE = "swagger";
    public static final String OPENAPI_VALUE = "openapi";

    public Spec readSpecFile(String sourcePath) {
        log.info("readSpecFile = " + sourcePath);
        try {
            byte[] bytes = readBytes(sourcePath);
            String filename = extractFilename(sourcePath);

            Spec spec = new Spec();
            spec.setName(filename);
            spec.setLocation(sourcePath);

            boolean hasExternalRefs = containsExternalRefs(bytes);
            spec.setRequiresResolution(hasExternalRefs);



            String content = new String(bytes, StandardCharsets.UTF_8);
            if (sourcePath.endsWith(".json")) {
                parseJsonSpec(content, spec);
                if(!hasExternalRefs){
                    ObjectMapper jsonMapper = new ObjectMapper();
                    Object jsonObject = jsonMapper.readValue(bytes, Object.class);

                    YAMLFactory yamlFactory = new YAMLFactory();
                    yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);

                    ObjectMapper yamlMapper = new ObjectMapper(yamlFactory);
                    bytes = yamlMapper.writeValueAsBytes(jsonObject);
                }
            } else {
                parseYamlSpec(content, spec);
            }

            byte[] openApiContent = hasExternalRefs
                    ? loadResolvedOpenApiAsYaml(sourcePath).getBytes(StandardCharsets.UTF_8)
                    : bytes;

            byte[] compressed = InspectoorUtil.compress(openApiContent);
            spec.setContent(Base64.getEncoder().encodeToString(compressed));

            return spec;
        } catch (Exception ex) {
            log.warn("Failed to process OpenAPI spec '{}': {}", sourcePath, ex.getMessage());
            log.debug("Stacktrace:", ex);
            return null;
        }
    }

    public String loadResolvedOpenApiAsYaml(String filePath) {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        options.setResolveFully(false);

        OpenAPI openAPI = parser.read(filePath, null, options);

        if (openAPI == null) {
            throw new InspectoorPluginException("OpenAPI can not be parsed: " + filePath);
        }

        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);

        ObjectMapper mapper = JsonMapper.builder(yamlFactory)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .defaultPropertyInclusion(
                        JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_NULL)
                )
                .disable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                .addMixIn(Schema.class, IgnoreSwaggerInternalFields.class)
                .addMixIn(Object.class, IgnoreSwaggerInternalFields.class)
                .build();

        try {
            Map<String, Object> map = mapper.convertValue(openAPI, new TypeReference<>() {});
            removeFieldsRecursively(map, Set.of("style", "explode", "exampleSetFlag", "jsonSchema"));
            return mapper.writeValueAsString(openAPI);
        } catch (Exception e) {
            log.error("Error in loadResolvedOpenApiAsYaml {}" , e.getMessage());
            throw new InspectoorPluginException("Error during Serialisieration " + e.getMessage());
        }
    }

    private byte[] readBytes(String sourcePath) throws IOException {
        if (sourcePath.startsWith("http://") || sourcePath.startsWith("https://")) {
            try (InputStream in = new URL(sourcePath).openStream()) {
                return in.readAllBytes();
            }
        } else {
            return Files.readAllBytes(Paths.get(sourcePath));
        }
    }

    private String extractFilename(String path) {
        if (path.startsWith("http")) {
            try {
                URL url = new URL(path);
                String urlPath = url.getPath();
                return urlPath.substring(urlPath.lastIndexOf('/') + 1);
            } catch (MalformedURLException e) {
                return "unknown";
            }
        } else {
            return new File(path).getName();
        }
    }

    private void parseJsonSpec(String content, Spec spec) {
        JSONObject json = new JSONObject(content);
        if (json.has(OPENAPI_VALUE)) {
            spec.setType("OpenAPI");
            spec.setVersion(json.getString(OPENAPI_VALUE));
        } else if (json.has(SWAGGER_VALUE)) {
            spec.setType(SWAGGER_VALUE);
            spec.setVersion(json.getString(SWAGGER_VALUE));
        }

        if (json.has("info")) {
            JSONObject infoMap = json.getJSONObject("info");
            spec.setInfo_version(extractInfoVersion(infoMap));
        }

        List<Server> servers = extractServers(json);
        spec.getServers().addAll(servers);
        spec.setSecuritySchemes(extractSecuritySchemes(json.toMap()));
    }


    @SuppressWarnings("unchecked")
    private void parseYamlSpec(String content, Spec spec) {
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(content);

        if (yamlMap == null || !yamlMap.containsKey("info")) return;

        LinkedHashMap infoMap = (LinkedHashMap) yamlMap.get("info");
        spec.setInfo_version(extractInfoVersion(infoMap));

        if (yamlMap.containsKey(OPENAPI_VALUE)) {
            spec.setType(OPENAPI_VALUE);
            spec.setVersion(yamlMap.get(OPENAPI_VALUE).toString());
        } else if (yamlMap.containsKey(SWAGGER_VALUE)) {
            spec.setType(SWAGGER_VALUE);
            spec.setVersion(yamlMap.get(SWAGGER_VALUE).toString());
        }

        List<Server> servers = extractServers(yamlMap);
        spec.getServers().addAll(servers);
        spec.setSecuritySchemes(extractSecuritySchemes(yamlMap));
    }


    private List<SecurityScheme> extractSecuritySchemes(Object root) {
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

    public boolean containsExternalRefs(byte[] yamlBytes) {
        String content = new String(yamlBytes, StandardCharsets.UTF_8);

        Pattern pattern = Pattern.compile("(?m)^\\s*(\"\\$ref\"|\\$ref)\\s*[:=]\\s*[\"']?(?!#/)([^\"'\n]+)[\"']?");
        Matcher matcher = pattern.matcher(content);

        return matcher.find();
    }

    private String extractInfoVersion(LinkedHashMap infoMap) {
        if (infoMap == null)
            return null;
        return infoMap.get("version").toString();
    }

    private String extractInfoVersion(JSONObject jsonObject) {
        if (jsonObject == null)
            return null;
        return jsonObject.get("version").toString();
    }


    private List<Server> extractServers(JSONObject json) {
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

    private List<Server> extractServers(Map<String, Object> yamlMap) {
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

    private String extractValue(Object url) {
        return url != null ? url.toString() : null;
    }

    public static void removeFieldsRecursively(Object obj, Set<String> fieldsToRemove) {
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            for (String field : fieldsToRemove) {
                map.remove(field);
            }
            for (Object value : map.values()) {
                removeFieldsRecursively(value, fieldsToRemove);
            }
        } else if (obj instanceof List) {
            for (Object item : (List<?>) obj) {
                removeFieldsRecursively(item, fieldsToRemove);
            }
        }
    }
}
