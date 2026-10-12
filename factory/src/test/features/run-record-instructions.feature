@run-record-instructions
Feature: Every machine uses the target-owned run record
  # Instruction-contract fixtures, not genuine human acceptance or live-agent checks.
  Background:
    Given a copy of the factory
    And a new target
    And a seed describing a game of Tetris
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied
    And a run named "tetris", on the "careful" line, with that seed and target

  Scenario: First execution and named resumption share authoritative evidence instructions
    When the factory runs
    Then it accepts it
    And every invoked machine receives the authoritative run record instructions
    Given selected evidence and owner feedback are referenced beside the current plan
    And the current plan content is recorded
    And factory execution settings are recorded
    When the factory runs the "tetris" run, given only its name
    Then it accepts it
    And every invoked machine receives the authoritative run record instructions
    And the recorded plan is unchanged
    And the selected run evidence and owner feedback are unchanged
    And there is no plan in the factory's folder
