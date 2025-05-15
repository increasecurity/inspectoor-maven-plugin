package io.github.increasecurity.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Spec {
    private String type;
    private String name;
    private String location;
    private String version;
    private String info_version;
    private String content;
    private List<Server> servers = new ArrayList<>();

}
