package io.github.increasecurity;

import io.github.increasecurity.model.Project;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PomReaderTest {

    @Test
    void readsProjectMetadataAndOpenApiFiles() {
        MavenProject mavenProject = project("sample-service", "com.example", "1.0.0");

        Project project = new PomReader().readPom(
                mavenProject,
                null,
                List.of(),
                "saas",
                "security",
                "payments",
                "baseline",
                "1.0.0");

        assertEquals("sample-service", project.getName());
        assertEquals(null, project.getGroupId());
        assertEquals(null, project.getArtifactId());
        assertEquals("payments", project.getSystem());
        assertEquals("security", project.getOu());
        assertEquals("baseline", project.getTag());
        assertEquals("saas", project.getRealm());
        assertEquals("java", project.getFramework());
        assertFalse(project.getSpecs().isEmpty());
    }

    @Test
    void detectsFrameworkFromBuildPlugin() {
        MavenProject mavenProject = project("spring-service", "com.example", "1.0.0");
        mavenProject.getBuild().addPlugin(plugin("org.springframework.boot", "spring-boot-maven-plugin"));

        Project project = new PomReader().readPom(
                mavenProject, null, List.of(), "saas", "ou", "system", null, "1.0.0");

        assertEquals("spring-boot", project.getFramework());
    }

    @Test
    void readsApplicationConfigurationSources() {
        MavenProject mavenProject = project("configured-service", "com.example", "1.0.0");
        mavenProject.getModel().setPomFile(new File("src/test/resources/application-config/pom.xml"));

        Project project = new PomReader().readPom(
                mavenProject, null, List.of(), "saas", "ou", "system", null, "1.0.0");

        assertEquals(4, project.getConfigurationSources().size());
        assertEquals("dev", project.getConfigurationSources().get(0).getProfile());
        assertEquals("test", project.getConfigurationSources().get(2).getProfile());
        assertEquals("jdbc:postgresql://localhost/test",
                project.getConfigurationSources().get(2).getEntries().get("spring.datasource.url"));
        assertEquals("[REDACTED]", project.getConfigurationSources().get(3).getEntries().get("spring.datasource.password"));
        assertEquals("8080", project.getConfigurationSources().get(3).getEntries().get("server.port"));
        assertEquals("[REDACTED]", project.getConfigurationSources().get(3).getEntries().get("spring.datasource.password"));
    }

    @Test
    void projectWithoutApplicationConfigurationHasNoSources() {
        Project project = new PomReader().readPom(
                project("plain-service", "com.example", "1.0.0"),
                null, List.of(), "saas", "ou", "system", null, "1.0.0");

        assertEquals(0, project.getConfigurationSources().size());
    }

    private MavenProject project(String name, String groupId, String version) {
        Model model = new Model();
        model.setName(name);
        model.setArtifactId(name);
        model.setGroupId(groupId);
        model.setVersion(version);
        model.setPomFile(new File("src/test/resources/specfiles/petstore.yaml"));
        Build build = new Build();
        build.setPlugins(new ArrayList<>());
        model.setBuild(build);
        MavenProject project = new MavenProject(model);
        assertNotNull(project.getBuild());
        return project;
    }

    private org.apache.maven.model.Plugin plugin(String groupId, String artifactId) {
        org.apache.maven.model.Plugin plugin = new org.apache.maven.model.Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        return plugin;
    }
}
