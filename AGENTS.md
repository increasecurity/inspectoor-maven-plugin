# Inspectoor Maven Plugin

## Project

This repository contains the Inspectoor Maven Plugin.

Before making changes, inspect the existing implementation and understand the current behavior.

## Development Rules

* Use Java 21.
* Keep the implementation simple and readable.
* Prefer small, focused methods.
* Avoid deeply nested `if/else` structures where a simpler design is possible.
* Do not introduce abstractions without a concrete benefit.
* Preserve existing behavior unless the task explicitly requests a change.
* Do not perform unrelated dependency or Maven plugin upgrades.

## Compatibility

* Existing Maven plugin goals must remain compatible unless explicitly changed by a task.
* Existing plugin parameters must not be renamed or removed without explicit instruction.
* Do not silently change generated output or collected metadata.

## Testing

* New functionality requires tests.
* Refactoring must be protected by tests.
* Prefer unit tests for isolated logic.
* Use Maven plugin integration tests where actual Maven execution is relevant.
* `mvn clean verify` must pass before a task is considered complete.

## Task Workflow

For every task:

1. Read the task description in `docs/tasks`.
2. Inspect the relevant existing code before changing anything.
3. Make the smallest reasonable change.
4. Add or update tests.
5. Run `mvn clean verify`.
6. Report:

    * changed files
    * important implementation decisions
    * test results
    * remaining risks or open questions

## Restrictions

* Do not modify unrelated functionality.
* Do not remove existing functionality without explicit instruction.
* Do not change Maven coordinates without explicit instruction.
* Do not commit changes unless explicitly requested.
