# IMP-002 - Establish Test Baseline

## Goal

Establish a reliable automated test baseline for the current behavior of the Inspectoor Maven Plugin.

The tests created by this task will serve as a safety net for the upcoming refactoring.

Existing production behavior must not be changed.

## Requirements

Analyze the current implementation and the findings documented in `docs/current-state.md`.

Identify the important existing behavior that should be protected by automated tests.

Add tests for the most relevant execution paths, including:

* normal plugin execution
* metadata currently collected from Maven projects
* generated output
* important conditional behavior
* relevant edge cases
* error handling where applicable

Choose the appropriate test level:

* Unit tests for isolated logic where useful
* Maven plugin integration tests where actual Maven execution is required

Prefer testing observable behavior over implementation details.

## Production Code

Production code should remain unchanged whenever possible.

Small changes to improve testability are allowed only when necessary and must not change existing behavior.

Do not refactor production code as part of this task.

## Build

The complete test suite must be executable through:

`mvn clean verify`

## Deliverables

* Automated tests protecting the current plugin behavior
* Required test configuration and test resources
* A short summary of which behavior is now covered
* Identification of important behavior that could not reasonably be tested

## Acceptance Criteria

* `mvn clean verify` succeeds
* Important existing plugin behavior is covered by automated tests
* Tests are deterministic and repeatable
* Tests do not depend on external services
* Existing production behavior remains unchanged
* No unrelated refactoring is performed
* No Java version migration is performed
* No unrelated dependency upgrades are performed
