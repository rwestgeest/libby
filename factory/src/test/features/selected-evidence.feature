@selected-evidence
Feature: Explicitly selected target-owned evidence
  Background:
    Given a copy of the factory
    And a new target, with a seed describing a game of Tetris
    And the target has the machines planner, doer and validator
    And the target has an assembly line "careful" on which the doer's work is validated
    And a run named "tetris", on the "careful" line, with that seed and target
    And the factory has staged and unstaged changes
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied

  Scenario: Selected evidence survives exclusion and name-only resumption
    Given the target ignores factory metadata and contains unrelated staged run records
    And the doer selects evidence beside the plan
    When the factory runs the "tetris" run
    Then selected evidence is committed separately beside the plan for each task
    And unselected run records and assembly-line changes are preserved
    And the factory's own files and unrelated uncommitted changes are as they were
    When the factory runs the "tetris" run, given only its name
    Then selected evidence is committed separately beside the plan for each task
    And unselected run records and assembly-line changes are preserved

  Scenario Outline: Evidence selection cannot escape its run
    Given the doer selects the evidence path "<path>"
    When the factory runs the "tetris" run
    Then it reports the diagnostic "Invalid selected evidence path"
    And no task product or selected evidence has been committed
    Examples:
      | path                              |
      | ../other/validation.md            |
      | /tmp/outside.md                   |
      | plan.md                          |
      | .assembly-lines/config.json       |
      | link.md                          |
