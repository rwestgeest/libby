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

  Scenario: Fenced multiline JSON results are accepted
    Given agents format results as fenced multiline JSON
    When the factory runs
    Then it accepts it
    And the plan shows every task as done

  Scenario: Configured lens and model forwarding preserve shared prompt context
    Given the validator uses the lens "responsibility-driven design"
    And the chosen model is "provider/model"
    When the factory runs
    Then it accepts it
    And every machine receives the model "provider/model" and the target context
    And the validator was given "Your validation lens is: responsibility-driven design."
    And the validator was asked for a result with the field "satisfied"
    And the planner was asked for a result with the field "complete"

  Scenario: Review reports beside the plan are named in every prompt
    Given a review report from "plan_reviewer_1" beside the plan
    When the factory runs
    Then it accepts it
    And the validator was given the path of the "plan_reviewer_1" report

  Scenario: A prompt says when there are no review reports
    When the factory runs
    Then it accepts it
    And the validator was given "No review reports beside the plan."

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

  Scenario: A prose-only doer handoff is repaired before independent validation
    Given the pi doer returns only prose
    When the factory runs
    Then it accepts it
    And the plan shows every task as done
    And each prose handoff was repaired once without tools
    And the original prose remains in the run record

  Scenario: A format repair cannot bypass validator rejection
    Given the pi doer returns only prose
    And the validator is never satisfied
    When the factory runs
    Then it reports the diagnostic "The task hit its limit of 3 attempts"
    And there are no new commits

  Scenario: A second prose answer stops without another format repair
    Given the pi doer returns only prose
    And the format repair also returns prose
    When the factory runs
    Then it reports the diagnostic "Could not read the doer's result"
    And the doer has been called twice
    And the validator has not been called
    And there are no new commits

  Scenario: External-input blocking survives handoff formatting
    Given the pi doer returns only prose
    And the format repair reports missing external input
    When the factory runs
    Then it reports the missing operator evidence
    And the validator has not been called
    And there are no new commits

  Scenario: A prose-only pi validator verdict is repaired into the validator's result
    Given the pi validator returns only prose
    When the factory runs
    Then it accepts it
    And the plan shows every task as done
    And each validator prose handoff was repaired once without tools, asking for its result fields

  Scenario: A pi ACP doer can repair its prose handoff without tools
    Given the pi doer returns only prose
    And the pi doer uses the ACP bridge
    When the factory runs
    Then it accepts it
    And each prose handoff was repaired once without tools
    And the original prose remains in the run record
