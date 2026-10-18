# IMP-003 - Refactor Existing Implementation

## Goal

Improve the internal structure, readability and maintainability of the Inspectoor Maven Plugin without changing its existing behavior.

The test baseline created in `IMP-002` must be used as the safety net for this task.

## Scope

Analyze the current implementation and identify high-value refactoring opportunities.

Focus especially on:

* deeply nested `if/else` structures
* long methods
* duplicated logic
* mixed responsibilities
* unclear method or variable names
* unnecessary complexity
* tightly coupled code
* logic that is difficult to test or understand
* repeated Maven project metadata access
* avoidable null-handling branches

Prefer simple improvements over large redesigns.

## Refactoring Principles

* Preserve existing behavior.
* Keep changes small and reviewable.
* Prefer clear control flow.
* Prefer early returns where they improve readability.
* Extract methods when they represent a meaningful responsibility.
* Extract classes only when there is a clear separation of responsibilities.
* Avoid introducing abstractions without a concrete benefit.
* Avoid speculative architecture.
* Prefer composition over unnecessary inheritance.
* Do not optimize prematurely.

## Tests

Before changing production code:

* review the tests created in `IMP-002`
* identify which tests protect the behavior being refactored

During refactoring:

* keep all existing tests green
* add tests only if an uncovered behavior is discovered

After refactoring:

`mvn clean verify`

must succeed.

## Restrictions

Do not:

* change existing Maven plugin goals
* change plugin parameters
* change collected metadata
* change generated output
* add new features
* migrate to Java 21
* perform unrelated dependency upgrades
* change Maven coordinates
* introduce breaking changes

## Deliverables

* Refactored production code
* Updated or additional tests where necessary
* A summary of the main refactoring changes
* A list of intentionally deferred refactoring opportunities

## Acceptance Criteria

* `mvn clean verify` succeeds
* Existing behavior remains unchanged
* Existing tests remain green
* Code readability is improved
* Complexity is reduced where reasonably possible
* No new feature is introduced
* No Java version migration is performed
* Changes remain focused on maintainability
