package io.github.increasecurity.openapi;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"exampleSetFlag", "types", "jsonSchema"})
public class IgnoreSwaggerInternalFields {
}
