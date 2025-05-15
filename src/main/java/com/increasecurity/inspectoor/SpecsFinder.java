package com.increasecurity.inspectoor;

import com.increasecurity.inspectoor.model.Spec;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SpecsFinder {

    public List<File> findSpecFiles(String srcPath) {
        return InspectoorUtil.findOpenApiFiles(srcPath);
    }

    public List<Spec> readSpecs(List<File> specsFiles) {
        List<Spec> specs = new ArrayList<>();
        if (!specsFiles.isEmpty()) {
            specsFiles.stream()
                    .map(File::getAbsolutePath);
            specs = InspectoorUtil.extractOpenApiVersions(specsFiles);
        }
        return specs;
    }

    public List<String> findSpecFilesInPom(MavenProject project) {
        List<String> foundFiles = new ArrayList<>();
        List<String> targetPlugins = List.of("org.openapitools:openapi-generator-maven-plugin", "ein.anderes.plugin"); // Falls weitere Plugins geprüft werden sollen

        project.getBuildPlugins().stream()
                .filter(plugin -> targetPlugins.contains(plugin.getKey()))
                .map(Plugin::getExecutions)
                .filter(Objects::nonNull)
                .forEach(executions -> {
                    executions.stream()
                            .map(PluginExecution::getConfiguration)
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .forEach(config -> {
                                try {
                                    foundFiles.addAll(extractYamlJsonPaths(config));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                });

        return foundFiles;
    }

    public void mergeSpecFiles(List<String> pomSpecFiles, List<Spec> specs) {
        List<String> missingLocations = findMissingLocations(pomSpecFiles, specs);
        if (missingLocations != null && !missingLocations.isEmpty()) {
            //Merge new files
        }


    }

    private List<String> findMissingLocations(List<String> neuListe, List<Spec> existingSpecs) {
        Set<String> existingLocations = existingSpecs.stream()
                .map(Spec::getLocation)
                .collect(Collectors.toSet());

        return neuListe.stream()
                .filter(location -> !existingLocations.contains(location)).toList();
    }

    private void secureXMLFactory(DocumentBuilderFactory factory) throws ParserConfigurationException {
        // to be compliant, completely disable DOCTYPE declaration:
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        // or completely disable external entities declarations:
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        // or prohibit the use of all protocols by external entities:
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setExpandEntityReferences(false);
    }

    private List<String> extractYamlJsonPaths(String xmlFile) throws Exception {
        List<String> result = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        secureXMLFactory(factory);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlFile.getBytes()));
        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("*"); // Alle Knoten durchsuchen

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String textContent = node.getTextContent().trim();
                if (textContent.matches(".*\\.(yaml|yml|json)$")) {
                    result.add(textContent);
                }
            }
        }
        return result;
    }
}
