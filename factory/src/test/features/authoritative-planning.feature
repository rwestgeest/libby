@authoritative-planning
Feature: Authoritative planning is a prerequisite for product work
  Background:
    Given a copy of the factory
    And a new target
    And a seed describing a game of Tetris
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied
    And the seed content is recorded

  Scenario Outline: A successful planner result without the authoritative plan cannot proceed
    Given the planner's planning behavior is "<behavior>"
    When the factory runs
    Then it reports the diagnostic "Planning did not establish the authoritative plan"
    And the doer has not been called
    And the validator has not been called
    And the authoritative plan is absent
    And there is no plan in the factory's folder
    And the seed content is unchanged
    Examples:
      | behavior        |
      | missing         |
      | missing-complete |
      | alternative     |

  Scenario: Planner process failure never starts product work
    Given the planner's planning behavior is "failure"
    When the factory runs
    Then it reports the diagnostic "Planning failed"
    And the doer has not been called
    And the validator has not been called
    And the authoritative plan is absent
    And the seed content is unchanged

  Scenario: A failed planning invocation can retry by name at the same location
    Given a run named "tetris", on the "careful" line, with that seed and target
    And the planner's planning behavior is "missing"
    When the factory runs
    Then it reports the diagnostic "Planning did not establish the authoritative plan"
    And the doer has not been called
    Given the planner's planning behavior is "normal"
    When the factory runs the "tetris" run, given only its name
    Then it accepts it
    And all machines share the target-owned plan path
    And the planner is told to preserve authoritative progress
    And the seed content is unchanged

  Scenario: Name-only resumption reads existing progress rather than regenerating it
    Given a run named "tetris", on the "careful" line, with that seed and target
    And the "tetris" run has been started
    And the authoritative plan contains recorded completion
    When the factory runs the "tetris" run, given only its name
    Then it accepts it
    And the recorded plan is unchanged
    And the doer has not been called
    And the planner is told to preserve authoritative progress

  Scenario: Planner failure preserves existing progress for a later retry
    Given a run named "tetris", on the "careful" line, with that seed and target
    And the "tetris" run has been started
    And the authoritative plan contains recorded completion
    And the planner's planning behavior is "failure"
    When the factory runs the "tetris" run, given only its name
    Then it reports the diagnostic "Planning failed"
    And the recorded plan is unchanged
    And the doer has not been called
    Given the planner's planning behavior is "normal"
    When the factory runs the "tetris" run, given only its name
    Then it accepts it
    And the recorded plan is unchanged
    And the doer has not been called

  Scenario: A planner cannot discard the authoritative plan at a later task boundary
    Given the planner's planning behavior is "delete-after-work"
    When the factory runs
    Then it reports the diagnostic "Planning did not establish the authoritative plan"
    And the doer has been called once
    And the authoritative plan is absent

  Scenario: Routing directly to a doer does not bypass the planning prerequisite
    Given this assembly line:
      """
      digraph {
        start -> doer
        doer -> planner
        planner -> finish [label="complete"]
        planner -> doer [label="!complete"]
      }
      """
    When the factory runs
    Then it reports the diagnostic "Planning did not establish the authoritative plan"
    And no agent has been called
    And the authoritative plan is absent
