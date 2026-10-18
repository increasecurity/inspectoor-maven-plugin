# IMP-004 - Migrate to Java 21

## Goal

Migrate the Inspectoor Maven Plugin from Java 17 to Java 21.

The migration must be focused on the Java version change and required build compatibility changes only.

Existing plugin behavior must remain unchanged.

## Preconditions

Before starting this task:

* `IMP-002` test baseline is complete
* existing tests are green
* `mvn clean verify` succeeds with the current Java version

## Scope

Review and update all project configuration required to build and test the plugin with Java 21.

This includes, where applicable:

* Java compiler configuration
* Maven compiler settings
* Maven plugin versions required for Java 21 compatibility
* test plugin configuration
* JaCoCo configuration
* source/target or `release` configuration
* Java version properties
* documentation referring to Java 17

Prefer using:

```xml
<maven.compiler.release>21</maven.compiler.release>
```

where compatible with the existing project setup.


## Runtime Compatibility

The Inspectoor Maven Plugin itself shall be migrated to Java 21.

However, the plugin may be used to analyze Maven projects that target different Java versions, for example:

* Java 11
* Java 17
* Java 21

The migration of the plugin to Java 21 must not implicitly assume that the analyzed Maven project also targets Java 21.

Verify and document:

* The minimum Java version required to execute the Inspectoor Maven Plugin.
* Whether Maven itself must therefore run with Java 21.
* How the plugin determines the Java version of the analyzed project.
* Whether projects targeting Java 11 or Java 17 can still be analyzed when Maven and the Inspectoor Maven Plugin run on Java 21.
* Whether any existing metadata collection incorrectly uses the runtime JVM version instead of the analyzed project's configured Java version.

Do not change the Java version of analyzed projects.


## Dependency and Plugin Updates

Do not perform general dependency modernization.

Only update Maven plugins or dependencies when required for Java 21 compatibility.

For every required version change:

* explain why the update is necessary
* keep the change as small as possible
* avoid unrelated upgrades

## Production Code

Do not refactor production code as part of this task.

Do not introduce Java 21 language features unless they are strictly required for compilation.

The purpose of this task is migration, not modernization.

Existing behavior must remain unchanged.

## Compatibility Analysis

Check whether the Maven plugin itself must run only on Java 21 or whether compatibility with projects using older Java versions is relevant.

Document the distinction between:

* the Java version used to build and execute the Inspectoor Maven Plugin
* the Java version used by Maven projects analyzed by the plugin

Do not make assumptions that the analyzed project must also use Java 21.

## Testing

Run the complete test suite using Java 21:

`mvn clean verify`

Verify that:

* all existing unit tests pass
* all integration tests pass
* plugin execution still behaves as before
* generated output remains unchanged unless explicitly required by Java 21 compatibility

## Deliverables

* Java 21 build configuration
* required compatibility-related Maven plugin/dependency updates
* updated documentation where necessary
* successful full Maven build
* summary of all required migration changes
* note of any remaining compatibility risks

## Restrictions

Do not:

* add new features
* refactor unrelated production code
* change Maven plugin goals
* change public plugin parameters
* change collected metadata
* change generated output
* perform unrelated dependency upgrades
* change Maven coordinates
* introduce breaking changes without explicit approval

## Acceptance Criteria

* project is configured for Java 21
* `mvn clean verify` succeeds using Java 21
* all tests remain green
* existing plugin behavior remains unchanged
* no unnecessary dependency or plugin upgrades are introduced
* no unrelated refactoring is performed
* Java 21 requirements are documented
* compatibility with projects using older Java versions has been considered and documented
