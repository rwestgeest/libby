@required-test-gate
Feature: Required tests independently gate accepted work and completion
  Background:
    Given a copy of the factory
    And a new target
    And a seed describing a game of Tetris
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied

  Scenario Outline: Satisfied validation cannot waive required failures
    Given required tests fail at "<boundary>"
    And the doer selects evidence beside the plan
    When the factory runs
    Then it reports the diagnostic "Required tests failed"
    And no task product or selected evidence has been committed
    And the doer has been called once
    And the validator has been called once
    And required-test failure leaves the task pending without completion

    Examples:
      | boundary    |
      | baseline    |
      | new work    |
      | unavailable |
      | unexecutable |

  Scenario: A validator-free line cannot bypass required tests
    Given required tests fail at "baseline"
    And the validator has been taken out, so the doer goes straight to the planner
    When the factory runs
    Then it reports the diagnostic "Required tests failed"
    And no task product or selected evidence has been committed
    And required-test failure leaves the task pending without completion

  Scenario: Planner changes must pass before progress recording or advancement
    Given required tests fail at "progress"
    When the factory runs
    Then it reports the diagnostic "Required tests failed"
    And the doer has been called once
    And only the first implementation is committed without accepted progress

  Scenario: Final planner changes cannot commit or report failing completion
    Given required tests fail at "completion"
    When the factory runs
    Then it reports the diagnostic "Required tests failed"
    And the doer has been called twice
    And completion failure preserves the last committed pending task

  Scenario: Completed named resumption still checks required tests
    Given a plan in which every task is done
    When the factory runs
    Then it accepts it
    Given required tests fail at "baseline"
    When the factory runs the "tetris" run, given only its name
    Then it reports the diagnostic "Required tests failed"
    And there are no new commits
    And the doer has not been called

  Scenario: Passing tests permit the normal lifecycle
    When the factory runs
    Then it accepts it
    And accepted task commits bracket the planner's completion changes
    And required tests ran before implementation and progress commits
    And the plan shows every task as done
