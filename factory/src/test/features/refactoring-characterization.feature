@characterization
Feature: Current factory behavior before refactoring
  These examples deliberately exercise the current target-local plan protocol,
  not the newer run-oriented homework protocol in the fetched specification.

  Background:
    Given a copy of the factory
    And a new target
    And a seed describing a game of Tetris
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied

  Scenario: The last JSON line wins over earlier results and prose
    Given the validator says "{\"satisfied\": false, \"findings\": [\"obsolete\"]}" before its result
    When the factory runs
    Then it accepts it
    And the doer has been called twice
    And the plan shows every task as done
    And the target has no uncommitted changes

  Scenario: A valid non-object JSON line after an object is not ignored
    Given the validator appends the JSON line "[]" after its result
    When the factory runs
    Then it reports that it could not read the validator's result
    And the doer has been called once
    And there are no new commits

  Scenario: Routing uses the field named by the edge
    Given the edges from validator are labelled "approved" and "not approved"
    And the validator returns its decision in "approved"
    And the validator is not satisfied the first time
    When the factory runs
    Then it accepts it
    And the validator was asked for a result with the field "approved"
    And the doer has been called three times
    And the doer was given the validator's findings
    And the plan shows every task as done

  Scenario: A routing label absent from the result is refused
    Given the edges from validator are labelled "approved" and "not approved"
    When the factory runs
    Then it reports that the result of validator has no field "approved"
    And there are no new commits

  Scenario: Retry exhaustion leaves work uncommitted and the task unfinished
    Given the factory allows at most three attempts at a task
    And the validator is never satisfied
    When the factory runs
    Then it reports that a task hit its limit
    And the doer has been called three times
    And the doer was given the validator's findings
    And the plan shows every task as not done
    And there are no new commits

  Scenario: Accepted work is committed before planner completion and its plan afterward
    When the factory runs
    Then it accepts it
    And accepted task commits bracket the planner's completion changes
    And the work for alpha and beta has been committed
    And the committed plan matches the plan on disk
    And the target has no uncommitted changes

  Scenario: Validation and commits are restricted to product changes in the target
    Given the target has tracked product changes and private factory metadata
    And the factory has staged and unstaged changes
    When the factory runs
    Then it accepts it
    And the validator sees tracked and untracked target product changes only
    And the target uses the containing repository
    And the factory's own files and unrelated uncommitted changes are as they were
    And all new commits touch only the target
    And the committed plan matches the plan on disk
    And the target has no uncommitted changes
