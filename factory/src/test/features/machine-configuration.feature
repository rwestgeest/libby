Feature: Harness configuration is isolated for every supported coding agent
  Background:
    Given a copy of the factory
    And a new target, with a seed describing a game of Tetris
    And the target has the machines planner, doer and validator
    And the target has an assembly line "careful" on which the doer's work is validated
    And a run named "tetris", on the "careful" line, with that seed and target
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied

  Scenario Outline: Each adapter uses only the machine's instructions and settings
    Given a plan with one task, not done
    And the doer uses <harness>
    And the doer's AGENTS.md says "You are the doer. ONLY_DOER_INSTRUCTIONS"
    And personal and target instructions conflict with the machine
    When the factory runs the "tetris" run
    Then it accepts it
    And the doer ran with isolated <harness> configuration
    And there is one new work commit

    Examples:
      | harness |
      | pi      |
      | claude  |
      | codex   |

  Scenario: A later machine's missing credentials prevent the first call
    Given a plan with one task, not done
    And the validator has no login or API key
    When the factory runs the "tetris" run
    Then it reports that the validator cannot sign in
    And no agent has been called
    And there are no new commits

  Scenario: A personal login is not used as a fallback
    Given a plan with one task, not done
    And personal and target instructions conflict with the machine
    And the doer has no login
    And there is no API key for the doer's provider
    When the factory runs the "tetris" run
    Then it reports that the doer cannot sign in
    And no agent has been called

  Scenario: API-key fallback uses the chosen model's provider
    Given a plan with one task, not done
    And the doer has no login
    And the doer's model is "google/example-model"
    And there is no API key for the doer's provider
    And the Google provider has an API key
    When the factory runs the "tetris" run
    Then it accepts it
    And the doer signed in with the API key

  Scenario: A key for another provider does not permit a call
    Given a plan with one task, not done
    And the doer has no login
    And the doer's model is "google/example-model"
    And there is an API key for the doer's provider
    When the factory runs the "tetris" run
    Then it reports that the doer cannot sign in
    And no agent has been called
