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

  @test-evolution
  Scenario: Machines distinguish an authorized test change from weakening acceptance
    When the factory runs
    Then it accepts it
    And the planner was given these instructions:
      """
      An explicitly requested behavior change in the seed or recorded product-owner instructions takes precedence over an older, conflicting test expectation.
      Plan the necessary test updates as part of the authorized change, rather than declaring obsolete expectations an external blocker.
      """
    And the doer was given these instructions:
      """
      An explicitly requested behavior change in the seed or recorded product-owner instructions takes precedence over an older, conflicting test expectation.
      Use TDD: add or update the test to assert the requested behavior, run it and inspect the result, then change the implementation and rerun the required suite.
      You are authorized and expected to update conflicting tests and their step definitions inside the target, including fetched specs when they express the superseded behavior.
      Do not delete, skip, filter out, or weaken checks merely to get green; preserve coverage of requirements that still apply.
      """
    And the validator was given these instructions:
      """
      Review changed tests against the requested behavior, not the superseded expectation; reject weakened coverage of requirements that still apply.
      A red phase during TDD is expected; acceptance still requires all required tests to pass.
      """

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
    And there are no new work commits

  Scenario: A nonzero process exit reports stdout and inherits stderr
    Given the doer exits unsuccessfully with output
    When the factory runs
    Then it reports the diagnostic "The doer exited unsuccessfully"
    And it reports the diagnostic "failure stdout"
    And it reports the diagnostic "failure stderr"
    And there are no new work commits
