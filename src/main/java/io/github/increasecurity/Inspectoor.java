package io.github.increasecurity;

import io.github.increasecurity.model.Project;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.util.Collections;
import java.util.List;

@Mojo(name = "inspectoor", requiresDependencyResolution = ResolutionScope.COMPILE, aggregator = true)
public class Inspectoor extends AbstractMojo {

    @Parameter(property = "checkSpecs", defaultValue = "false")
    private boolean checkspecs;

    @Parameter(property = "command")
    private int command = 1;

    @Parameter(property = "ou", required = true)
    String ou;

    @Parameter(property = "system", required = true)
    String system;

    @Parameter(property = "tag")
    String tag;

    @Parameter(property = "url")
    String url;

    @Parameter(property = "apikey")
    String apikey;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "reactorProjects", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Parameter(defaultValue = "${session}")
    private MavenSession mavenSession;

    @org.apache.maven.plugins.annotations.Component
    private BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("############################## execute: inspectoor ##############################");
        Project newProjekt = new PomReader().readPom(project, reactorProjects, "realm1", this.ou, this.system, this.tag, project.getVersion());
        newProjekt.setGroupId(project.getGroupId());
        newProjekt.setArtifactId(project.getArtifactId());

        if (checkspecs) {
            checkSpecs();
        }

        switch (command) {
            case 1:
                printInfos(newProjekt);
                break;
            case 2:
                printInfos(newProjekt);
                uploadProject(newProjekt);
                break;
            case 3:
                printInfos(newProjekt);
                uploadProject(newProjekt);
                uploadSbom();
                break;
            default:
                getLog().warn("unknown command: " + command);
                break;
        }
    }

    private void uploadSbom() {
        File pomFile = project.getModel().getPomFile();
        Plugin sbomPlugin = this.project.getPlugin("org.cyclonedx:cyclonedx-maven-plugin");
        if (sbomPlugin == null) {
            getLog().info("No Plugin was found, we will use the plugin directly");
            sbomPlugin = getMvnPlugin("json");
        }

        try {
            MojoExecutor.ExecutionEnvironment executionEnvironment = MojoExecutor.executionEnvironment(this.project, mavenSession, pluginManager);
            MojoExecutor.executeMojo(sbomPlugin, "makeAggregateBom", getPluginConfiguration("json"), executionEnvironment);
            String sbomLocation = InspectoorUtil.readSBOMLocation(pomFile.getAbsolutePath());
            HttpClient.uploadSBOM(sbomLocation, this.system, project.getName(), this.url + "/upload", this.apikey);
        } catch (Exception ex) {
            getLog().error("error trying to process SBOM", ex);
        }

    }

    private void uploadProject(Project newProjekt) {
        String json = InspectoorUtil.toJson(newProjekt);
        getLog().info("JSON outcome:                    ");
        getLog().info(json);
        HttpClient.doPostRequest(json, this.url + "/projects", this.apikey);
    }

    /**
     * Validate the spec files. Not
     * @throws MojoExecutionException
     */
    private void checkSpecs() throws MojoExecutionException {
        getLog().info("*** checkSpecs ***");
        throw new MojoExecutionException("CheckSpecs not implemented now");
    }

    private void printInfos(Project project) {
        getLog().info("name:                    " + project.getName());
        getLog().info("artifactId:              " + project.getArtifactId());
        getLog().info("groupId:                 " + project.getGroupId());
        getLog().info("name:                    " + project.getName());
        getLog().info("Version:                 " + project.getVersion());
        getLog().info("OU:                      " + project.getOu());
        getLog().info("System:                  " + project.getSystem());
        getLog().info("Mono:                    " + project.isMono());
        getLog().info("Tag:                     " + project.getTag());
        getLog().info("Number of Projects:      " + project.getProjects().size());
        getLog().info("Number of Specs:         " + project.getSpecs().size());
    }

    /**
     * Building the cyclonedx plugin
     * @param output
     * @return
     */
    private Plugin getMvnPlugin(String output) {
        Plugin plugin = new Plugin();

        plugin.setGroupId("org.cyclonedx");
        plugin.setArtifactId("cyclonedx-maven-plugin");
        plugin.setVersion("2.9.1");

        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setGoals(Collections.singletonList("makeAggregateBom"));
        pluginExecution.setPhase("package");
        plugin.setExecutions(Collections.singletonList(pluginExecution));

        plugin.setConfiguration(getPluginConfiguration(output));

        return plugin;
    }

    /**
     * The configuration for the cyclonedx plugin
     * @param output
     * @return
     */
    private Xpp3Dom getPluginConfiguration(String output) {
        MojoExecutor.Element projectType = new MojoExecutor.Element("projectType", "library");
        MojoExecutor.Element schemaVersion = new MojoExecutor.Element("schemaVersion", "1.4");
        MojoExecutor.Element includeBomSerialNumber = new MojoExecutor.Element("includeBomSerialNumber", "false");
        MojoExecutor.Element includeCompileScope = new MojoExecutor.Element("includeCompileScope", "true");

        MojoExecutor.Element includeProvidedScope = new MojoExecutor.Element("includeProvidedScope", "true");
        MojoExecutor.Element includeRuntimeScope = new MojoExecutor.Element("includeRuntimeScope", "true");
        MojoExecutor.Element includeSystemScope = new MojoExecutor.Element("includeSystemScope", "true");

        MojoExecutor.Element includeTestScope = new MojoExecutor.Element("includeTestScope", "false");
        MojoExecutor.Element outputFormat = new MojoExecutor.Element("outputFormat", output);

        return MojoExecutor.configuration(projectType, schemaVersion, includeBomSerialNumber,
                includeCompileScope, includeProvidedScope, includeRuntimeScope, includeSystemScope,
                includeTestScope, outputFormat);
    }
}
