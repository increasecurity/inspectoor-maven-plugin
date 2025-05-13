package com.increasecurity.projects.inspectoor;

import com.increasecurity.projects.inspectoor.model.Project;
import com.increasecurity.projects.inspectoor.model.Spec;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

import static com.increasecurity.projects.inspectoor.InspectoorUtil.isBlank;
import static com.increasecurity.projects.inspectoor.InspectoorUtil.isBlankList;

public class PomReader {

    public Project readPom(MavenProject project, List<MavenProject> reactorProjects, String realm, String ou, String system, String tag, String version) {
        File pomFile = project.getModel().getPomFile();
        SpecsFinder specsFinder = new SpecsFinder();
        Project newProject = new Project();
        newProject.setName(project.getName());
        newProject.setVersion(version);
        newProject.setOu(ou);
        newProject.setSystem(isBlank(system) ? project.getName() : system);
        newProject.setSystem(system);
        newProject.setTag(tag);

        String srcPath = InspectoorUtil.adjustSrcPath(pomFile.getAbsolutePath());

        List<File> specsFiles = specsFinder.findSpecFiles(srcPath);

        List<Spec> specs = specsFinder.readSpecs(specsFiles);
        List<String> pomSpecFiles = specsFinder.findSpecFilesInPom(project);
        specsFinder.mergeSpecFiles(pomSpecFiles, specs);

        newProject.getSpecs().addAll(specs);

        List<String> modules = project.getModules();
        if(!isBlankList(project.getModules()) ){
            newProject.setMono(true);
        }
        if (reactorProjects != null) {
            for (MavenProject proj : reactorProjects) {
                if (modules.contains(proj.getName())) {
                    Project subProject = readPom(proj, reactorProjects, realm, ou, system, tag, version);
                    newProject.getProjects().add(subProject);
                }
            }
        }

        return newProject;
    }
}
