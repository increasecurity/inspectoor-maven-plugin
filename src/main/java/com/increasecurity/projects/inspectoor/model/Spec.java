package com.increasecurity.projects.inspectoor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Spec {
    private String type;
    private String name;
    private String location;
    private String version;
    private String info_version;
    private String content;
    private List<Server> servers = new ArrayList<>();

    public Spec() {
    }

    public Spec(String type, String name, String location, String version, String info_version, String content, List<Server> servers) {
        this.type = type;
        this.name = name;
        this.location = location;
        this.version = version;
        this.info_version = info_version;
        this.content = content;
        this.servers = servers;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInfo_version() {
        return info_version;
    }

    public void setInfo_version(String info_version) {
        this.info_version = info_version;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Spec)) return false;
        Spec spec = (Spec) o;
        return Objects.equals(type, spec.type) && Objects.equals(name, spec.name) && Objects.equals(location, spec.location) && Objects.equals(version, spec.version) && Objects.equals(info_version, spec.info_version) && Objects.equals(content, spec.content) && Objects.equals(servers, spec.servers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, location, version, info_version, content, servers);
    }
}
