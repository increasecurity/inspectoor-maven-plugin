# IMP-005 - Read Application Configuration Metadata

## Goal

Extend the Inspectoor Maven Plugin to discover and read Spring-style application configuration files from the analyzed Maven project and attach the extracted information as metadata to:

`io.github.increasecurity.model.Project`

This task is about collecting configuration metadata only.

No assumptions should be made yet about how the metadata will later be analyzed or used.

## Supported Files

Detect configuration files with names such as:

* `application.properties`
* `application.yml`
* `application.yaml`
* `application-<profile>.properties`
* `application-<profile>.yml`
* `application-<profile>.yaml`

Examples:

* `application-dev.properties`
* `application-test.yml`
* `application-prod.yaml`

The implementation should primarily consider standard Maven resource locations such as:

`src/main/resources`

Do not scan unrelated directories recursively without a concrete reason.

## Metadata Model

Attach the discovered configuration metadata to:

`io.github.increasecurity.model.Project`

Prefer a structured representation that preserves the source file and optional profile.

Conceptually, each configuration source should contain information such as:

* file name
* relative path
* profile name, if present
* file type (`properties` or `yaml`)
* extracted configuration entries

Avoid storing configuration from all files in one flat map where values from different profiles could overwrite each other.

The exact model design should follow the existing conventions in `io.github.increasecurity.model`.

## Parsing

### Properties

Read standard Java `.properties` files and preserve their key/value pairs.

### YAML

Support YAML configuration files.

Nested YAML values should be represented consistently.

For example:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost/test
```

may be represented as:

```text
spring.datasource.url = jdbc:postgresql://localhost/test
```

if flattening fits the existing metadata model.

Use one consistent representation for all YAML files.

## Profile Detection

For files named:

`application-<profile>.<extension>`

derive the profile from the filename.

Examples:

* `application-dev.yml` → `dev`
* `application-prod.properties` → `prod`

Do not attempt to reproduce the complete Spring Boot profile activation or property resolution algorithm as part of this task.

## Security

Configuration files may contain sensitive values.

The plugin must not blindly expose secrets as metadata.

At minimum, review handling of keys that may contain sensitive information, for example:

* password
* secret
* token
* api-key / apikey
* credentials
* private-key

Sensitive values should be masked, omitted, or otherwise protected before being attached to the `Project` metadata.

Do not log sensitive configuration values.

Document the chosen handling strategy.

## Error Handling

Invalid or unreadable configuration files must not unnecessarily fail the complete Maven build.

Handle parsing errors in a controlled way consistent with the existing plugin behavior.

Errors should provide enough context to identify the affected file without exposing sensitive content.

## Multi-Module Projects

Consider that the plugin may run against Maven projects with multiple modules.

Configuration metadata must remain attributable to the Maven project/module from which it was collected.

Do not merge configuration from unrelated modules without explicit design.

## Tests

Add tests covering at least:

* `application.properties`
* `application.yml`
* `application.yaml`
* profile-specific `.properties` configuration
* profile-specific YAML configuration
* multiple application configuration files
* nested YAML properties
* sensitive values
* invalid configuration input
* project without application configuration

Use test resources rather than relying on external files or services.

## Restrictions

Do not:

* implement Spring Boot's complete configuration resolution logic
* resolve active Spring profiles
* merge configuration according to Spring Boot precedence rules
* resolve environment variables
* resolve external configuration servers
* change unrelated metadata collection
* perform unrelated refactoring
* expose credentials or secrets
* change Maven plugin goals or public parameters unless required by this task

## Deliverables

* application configuration discovery
* `.properties` parsing
* YAML parsing
* profile detection from filenames
* metadata representation in `io.github.increasecurity.model.Project`
* sensitive-value handling
* automated tests
* summary of the chosen metadata model

## Acceptance Criteria

* supported `application*` configuration files are discovered
* `.properties`, `.yml`, and `.yaml` files are parsed
* profile-specific files remain distinguishable
* configuration sources do not overwrite each other unintentionally
* metadata is attached to `io.github.increasecurity.model.Project`
* sensitive values are not exposed in plain text
* malformed files are handled gracefully
* `mvn clean verify` succeeds
* existing plugin behavior remains unchanged outside this feature
