package io.github.increasecurity;

import io.github.increasecurity.model.Project;
import io.github.increasecurity.model.Spec;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

import static io.github.increasecurity.InspectoorUtil.isBlank;
import static io.github.increasecurity.InspectoorUtil.isBlankList;

public class PomReader {

    public Project readPom(MavenProject mavenProject, MavenProject rootParent, List<MavenProject> reactorProjects, String realm, String ou, String system, String tag, String version) {
        Project project = new Project();
        setProjectMetadata(project, mavenProject, realm, ou, system, tag, version);
        addSpecifications(project, mavenProject);
        addSubProjects(project, mavenProject, rootParent, reactorProjects, realm, ou, system, tag, version);
        project.setFramework(detectFramework(mavenProject, rootParent));
        return project;
    }

    private void setProjectMetadata(Project project, MavenProject mavenProject, String realm, String ou,
                                    String system, String tag, String version) {
        project.setName(mavenProject.getName());
        project.setVersion(version);
        project.setOu(ou);
        project.setSystem(system);
        project.setTag(tag);
        project.setRealm(realm);
    }

    private void addSpecifications(Project project, MavenProject mavenProject) {
        File pomFile = mavenProject.getModel().getPomFile();
        SpecsFinder specsFinder = new SpecsFinder();
        String srcPath = InspectoorUtil.adjustSrcPath(pomFile.getAbsolutePath());
        List<Spec> specs = specsFinder.readSpecs(specsFinder.findSpecFiles(srcPath));
        specsFinder.mergeSpecFiles(specsFinder.findSpecFilesInPom(mavenProject), specs);
        project.getSpecs().addAll(specs);
    }

    private void addSubProjects(Project project, MavenProject mavenProject, MavenProject rootParent,
                                List<MavenProject> reactorProjects, String realm, String ou, String system,
                                String tag, String version) {
        List<String> modules = mavenProject.getModules();
        project.setMono(!isBlankList(modules));
        if (reactorProjects == null) {
            return;
        }

        reactorProjects.stream()
                .filter(reactorProject -> modules.contains(reactorProject.getName()))
                .map(reactorProject -> readPom(reactorProject, rootParent, reactorProjects, realm, ou, system, tag, version))
                .forEach(project.getProjects()::add);
    }

    private String detectFramework(MavenProject project, MavenProject rootParent) {
        String result = frameworkForGroupId(rootParent == null ? null : rootParent.getGroupId(), false);
        if (result == null) {
            result = "java";
        }

        if (project.getBuild() != null && project.getBuild().getPlugins() != null) {
            for (Plugin plugin : project.getBuild().getPlugins()) {
                String framework = frameworkForGroupId(plugin.getGroupId(), true);
                if (framework != null) return framework;
            }
        }

        for (Dependency dep : project.getDependencies()) {
            String framework = frameworkForGroupId(dep.getGroupId(), true);
            if (framework != null) return framework;
        }

        return result;
    }

    private String frameworkForGroupId(String groupId, boolean includeJakarta) {
        if (groupId == null) return null;
        if (groupId.contains("springframework.boot")) return "spring-boot";
        if (groupId.contains("quarkus")) return "quarkus";
        if (groupId.contains("micronaut")) return "micronaut";
        if (includeJakarta && groupId.contains("helidon")) return "helidon";
        if (includeJakarta && groupId.contains("dropwizard")) return "dropwizard";
        if (includeJakarta && (groupId.contains("jakarta.") || groupId.contains("javax."))) return "jakarta-ee";
        return null;
    }
}
