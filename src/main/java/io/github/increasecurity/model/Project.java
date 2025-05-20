package io.github.increasecurity.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Project extends Parent{
    private String name;
    private String version;
    private String description;
    private boolean mono = false;
    private String ou;
    private String system;
    private String tag;
    private String realm;
    private String framework;
    private String date;
    private Parent parent;
    private String packaging;
    private String srcPath;
    private List<Project> projects = new ArrayList();
    private List<Spec> specs = new ArrayList();
}
