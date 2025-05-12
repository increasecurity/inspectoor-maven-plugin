package com.increasecurity.projects.inspectoor.model;

import java.util.Objects;

public class Server {
    private String url;
    private String description;

    public Server() {
    }

    public Server(String url, String description) {
        this.url = url;
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Server)) return false;
        Server server = (Server) o;
        return Objects.equals(url, server.url) && Objects.equals(description, server.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, description);
    }
}
