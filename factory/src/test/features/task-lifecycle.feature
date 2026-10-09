@lifecycle
Feature: Task lifecycle follows assembly-line routing
  Background:
    Given a copy of the factory
    And a new target
    And a seed describing a game of Tetris
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied

  Scenario: A validator-free line still brackets planner completion with commits
    Given the validator has been taken out, so the doer goes straight to the planner
    When the factory runs
    Then it accepts it
    And accepted task commits bracket the planner's completion changes
    And the validator has not been called
    And the target has no uncommitted changes

  Scenario: Retry counts and findings reset after each planner execution
    Given the factory allows at most two attempts at a task
    And the validator rejects the first attempt of each task
    When the factory runs
    Then it accepts it
    And the doer has been called four times
    And each task starts without previous findings
    And accepted task commits bracket the planner's completion changes

  Scenario: Custom machines between work and planning do not reset the lifecycle
    Given a custom machine called inspector
    And this assembly line:
      """
      digraph {
        start -> planner
        planner -> doer [label="not complete"]
        planner -> finish [label="complete"]
        doer -> inspector
        inspector -> planner
      }
      """
    When the factory runs
    Then it accepts it
    And accepted task commits bracket the planner's completion changes
    And the target has no uncommitted changes

  Scenario: Final commit records a new plan even when no work was attempted
    Given this assembly line:
      """
      digraph {
        start -> planner
        planner -> finish
      }
      """
    When the factory runs
    Then it accepts it
    And the doer has not been called
    And the plan shows every task as not done
    And there are no new work commits
    And the committed plan matches the plan on disk
    And the target has no uncommitted changes
    And it prints the stop message

  Scenario: A validator may route to planner despite exhausted attempts
    Given the factory allows at most one attempt at a task
    And the validator is never satisfied
    And this assembly line:
      """
      digraph {
        start -> planner
        planner -> doer [label="not complete"]
        planner -> finish [label="complete"]
        doer -> validator
        validator -> planner
      }
      """
    When the factory runs
    Then it accepts it
    And the doer has been called twice
    And the plan shows every task as done
    And accepted task commits bracket the planner's completion changes
    And the target has no uncommitted changes

  Scenario: Planning without work does not claim acceptance
    Given a plan in which every task is done
    When the factory runs
    Then it accepts it
    And the doer has not been called
    And there are no new commits
    And the initial planner is asked only for plan status
    And it prints the stop message
