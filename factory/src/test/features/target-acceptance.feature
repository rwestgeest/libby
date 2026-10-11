@target-acceptance
Feature: Acceptance belongs to the target, not execution metadata
  # The deterministic owner/evidence fixture is not a real human or device check.
  Background:
    Given a copy of the factory
    And a new target
    And a seed describing a game of Tetris
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied
    And a run named "tetris", on the "careful" line, with that seed and target
    And automated work is complete but manual acceptance is unavailable

  Scenario: Automated completion does not pass an unavailable manual check
    When the factory runs
    Then it reports the diagnostic "Manual acceptance remains outstanding"
    And the planner reports acceptance false
    And manual acceptance remains outstanding in the target plan
    And the committed plan matches the plan on disk
    And the doer has not been called
    And the validator has not been called
    And there is no plan in the factory's folder
    Given the owner records an unperformed manual check in the target
    And the current plan content is recorded
    And factory execution settings are recorded
    When the factory runs the "tetris" run, given only its name
    Then it reports the diagnostic "Manual acceptance remains outstanding"
    And the planner reports acceptance false
    And the recorded plan is unchanged
    And manual acceptance remains outstanding in the target plan
    And the doer has not been called

  Scenario: Owner evidence in the target allows name-only resumption
    When the factory runs
    Then it reports the diagnostic "Manual acceptance remains outstanding"
    And the planner reports acceptance false
    Given the owner records passing manual evidence in the target
    And the current plan content is recorded
    And factory execution settings are recorded
    When the factory runs the "tetris" run, given only its name
    Then it accepts it
    And the planner reports acceptance true
    And the recorded plan is unchanged
    And the committed plan matches the plan on disk
    And there are no new commits
    And the doer has not been called
    And the validator has not been called
    And there is no plan in the factory's folder
