@machine-invocation
Feature: Machine invocation and job context preserve the current protocol
  Background:
    Given a copy of the factory
    And a new target
    And a seed describing a game of Tetris
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied

  Scenario: Missing configuration selects pi
    Given the planner configuration is absent
    When the factory runs
    Then it accepts it
    And pi has been called
    And the plan shows every task as done

  Scenario: Null harness configuration selects pi
    Given the planner configuration is "{\"harness\": null}"
    When the factory runs
    Then it accepts it
    And pi has been called

  Scenario: An absent validator configuration uses the default lens and pi
    Given the validator configuration is absent
    When the factory runs
    Then it accepts it
    And pi has been called

  Scenario: Default lens is included when no lens is configured
    When the factory runs
    Then it accepts it
    And the validator was given "Your validation lens is: testability."

  Scenario: Configured lens and model forwarding preserve shared prompt context
    Given the validator uses the lens "responsibility-driven design"
    And the chosen model is "provider/model"
    When the factory runs
    Then it accepts it
    And every machine receives the model "provider/model" and the target context
    And the validator was given "Your validation lens is: responsibility-driven design."
    And the validator was asked for a result with the field "satisfied"
    And the planner was asked for a result with the field "complete"

  Scenario Outline: Invalid configuration is refused before invocation
    Given the planner configuration is "<configuration>"
    When the factory runs
    Then it reports the diagnostic "Could not read the configuration for planner"
    And no agent has been called
    And there are no new commits
    Examples:
      | configuration |
      | broken        |
      | []            |

  Scenario: Process startup failure retains its diagnostic
    Given the doer cannot be run
    When the factory runs
    Then it reports that it could not run the doer
    And there are no new commits

  Scenario: A nonzero process exit reports stdout and inherits stderr
    Given the doer exits unsuccessfully with output
    When the factory runs
    Then it reports the diagnostic "The doer exited unsuccessfully"
    And it reports the diagnostic "failure stdout"
    And it reports the diagnostic "failure stderr"
    And there are no new commits
