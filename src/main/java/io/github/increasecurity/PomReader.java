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

    public Project readPom(MavenProject mavenProject, MavenProject rootParent,List<MavenProject> reactorProjects, String realm, String ou, String system, String tag, String version) {
        File pomFile = mavenProject.getModel().getPomFile();
        SpecsFinder specsFinder = new SpecsFinder();
        Project project = new Project();
        project.setName(mavenProject.getName());
        project.setVersion(version);
        project.setOu(ou);
        project.setSystem(isBlank(system) ? mavenProject.getName() : system);
        project.setSystem(system);
        project.setTag(tag);
        project.setRealm(realm);

        String srcPath = InspectoorUtil.adjustSrcPath(pomFile.getAbsolutePath());
        List<File> specsFiles = specsFinder.findSpecFiles(srcPath);

        List<Spec> specs = specsFinder.readSpecs(specsFiles);
        List<String> pomSpecFiles = specsFinder.findSpecFilesInPom(mavenProject);
        specsFinder.mergeSpecFiles(pomSpecFiles, specs);
        project.getSpecs().addAll(specs);

        List<String> modules = mavenProject.getModules();
        if (!isBlankList(mavenProject.getModules())) {
            project.setMono(true);
        }
        if (reactorProjects != null) {
            for (MavenProject proj : reactorProjects) {
                if (modules.contains(proj.getName())) {
                    Project subProject = readPom(proj,rootParent,reactorProjects, realm, ou, system, tag, version);
                    project.getProjects().add(subProject);
                }
            }
        }

        String framework = detectFramework(mavenProject, rootParent);
        project.setFramework(framework);

        return project;
    }

    private String detectFramework(MavenProject project, MavenProject rootParent) {
        // 1. parent
        String result = "java";
        if (rootParent != null && rootParent.getGroupId() != null) {
            String g = rootParent.getGroupId();
            if (g.contains("springframework.boot")) {
                result = "spring-boot";
            }
            if (g.contains("quarkus")) {
                result = "quarkus";
            }
            if (g.contains("micronaut")){
                result = "micronaut";
            }
        }

        // 2. plugins
        if (project.getBuild() != null && project.getBuild().getPlugins() != null) {
            for (Plugin plugin : project.getBuild().getPlugins()) {
                String g = plugin.getGroupId();
                if (g.contains("springframework.boot")) return "spring-boot";
                if (g.contains("quarkus")) return "quarkus";
                if (g.contains("micronaut")) return "micronaut";
                if (g.contains("helidon")) return "helidon";
                if (g.contains("dropwizard")) return "dropwizard";
            }
        }

        // 3. dependencies
        for (Dependency dep : project.getDependencies()) {
            String g = dep.getGroupId();
            if (g == null) continue;
            if (g.contains("springframework.boot")) return "spring-boot";
            if (g.contains("quarkus")) return "quarkus";
            if (g.contains("micronaut")) return "micronaut";
            if (g.contains("helidon")) return "helidon";
            if (g.contains("dropwizard")) return "dropwizard";
            if (g.contains("jakarta.") || g.contains("javax.")) return "jakarta-ee";
        }

        return result;
    }
}
