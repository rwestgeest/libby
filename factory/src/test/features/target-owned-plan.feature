@target-owned-plan
Feature: One target-owned plan identity per named run
  Background:
    Given a copy of the factory
    And a new target
    And a seed describing a game of Tetris
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied
    And the seed content is recorded

  Scenario: All machines share a target-local plan on first invocation and retry
    Given the validator is not satisfied the first time
    When the factory runs
    Then it accepts it
    And all machines share the target-owned plan path
    And there is no plan in the factory's folder
    And the seed content is unchanged

  Scenario: Name-only resumption preserves recorded progress rather than recreating from seed
    Given a run named "tetris", on the "careful" line, with that seed and target
    And the "tetris" run has been started
    And the authoritative plan contains recorded completion
    When the factory runs the "tetris" run, given only its name
    Then it accepts it
    And the recorded plan is unchanged
    And the doer has not been called
    And there is no plan in the factory's folder
    And the seed content is unchanged

  Scenario: Two runs in one target have independent plans including resumption
    Given a run named "first", on the "careful" line, with that seed and target
    When the factory runs the "first" run
    Then it accepts it
    Given the current plan content is recorded
    And a run named "second", on the "careful" line, with that seed and target
    When the factory runs the "second" run
    Then it accepts it
    And each run has its own plan
    And the recorded plan is unchanged
    And there is no plan in the factory's folder
    Given the current plan content is recorded
    When the factory runs the "first" run, given only its name
    Then it accepts it
    And the recorded plan is unchanged
    And the seed content is unchanged
    And there is no plan in the factory's folder

  Scenario: Factory-owned legacy progress is refused even when a target plan exists
    Given factory-owned legacy progress exists
    And the authoritative plan contains recorded completion
    When the factory runs
    Then it reports the diagnostic "legacy or ambiguous plan ownership"
    And no agent has been called
    And the recorded plan is unchanged
    And the seed content is unchanged

  Scenario: Legacy settings without a plan are ambiguous and cannot silently restart
    Given legacy run settings exist without a plan
    When the factory runs the "tetris" run, given only its name
    Then it reports the diagnostic "resumption is unsupported"
    And no agent has been called
    And the recorded plan is unchanged

  Scenario Outline: Unsafe names cannot escape the per-run namespace
    Given a run named "<name>", on the "careful" line, with that seed and target
    When the factory runs
    Then it reports the diagnostic "run name is invalid"
    And no agent has been called
    And the seed content is unchanged
    Examples:
      | name       |
      | ../escape  |
      | /absolute  |
      | first/next |
      | ..         |

  Scenario Outline: Filesystem aliases cannot redirect the authoritative plan
    Given the target plan component "<component>" aliases "<destination>"
    When the factory runs
    Then it reports the diagnostic "must not contain filesystem aliases"
    And no agent has been called
    And the seed content is unchanged
    Examples:
      | component                        | destination    |
      | .factory                         | outside        |
      | .factory/runs                    | outside        |
      | .factory/runs/tetris              | target/alias   |
      | .factory/runs/tetris/plan.md      | outside        |

  Scenario: A hard-linked seed cannot become the plan
    Given the plan aliases the seed by hard link
    When the factory runs
    Then it reports the diagnostic "plan must be separate from the seed"
    And no agent has been called
    And the seed content is unchanged
