@plan-history
Feature: Target-owned planning and accepted work share safe Git history
  Background:
    Given a copy of the factory
    And a new target
    And a seed describing a game of Tetris
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied

  Scenario: Planning and accepted task status are durable before proceeding
    Given the factory has staged and unstaged changes
    And other target metadata has staged and unstaged changes
    When the factory runs
    Then it accepts it
    And accepted task commits bracket the planner's completion changes
    And the committed plan matches the plan on disk
    And all new commits touch only the target
    And other target metadata retains its staged and unstaged changes
    And the factory's own files and unrelated uncommitted changes are as they were

  Scenario: Rejected work leaves only initial planning in history
    Given the validator is never satisfied
    And the factory allows at most one attempt at a task
    When the factory runs
    Then it reports that a task hit its limit
    And there are no new work commits
    And the committed plan has no completed tasks
    And the plan shows every task as not done

  Scenario: Planning-only completion does not commit pending product changes
    Given the target has tracked product changes and private factory metadata
    And this assembly line:
      """
      digraph {
        start -> planner
        planner -> finish
      }
      """
    When the factory runs
    Then it accepts it
    And there are no new work commits
    And the committed plan has no completed tasks

  Scenario: Failed initial plan commit prevents product implementation
    Given Git refuses factory commit number 1
    When the factory runs
    Then it reports the diagnostic "COMMIT_REFUSED"
    And the doer has not been called
    And there are no new commits
    And the plan shows every task as not done

  Scenario: Failed work commit prevents completion planning
    Given Git refuses factory commit number 2
    When the factory runs
    Then it reports the diagnostic "COMMIT_REFUSED"
    And the doer has been called once
    And there are no new work commits
    And the committed plan has no completed tasks
    And the plan shows every task as not done

  Scenario: Failed progress commit stops before the next task
    Given Git refuses factory commit number 3
    When the factory runs
    Then it reports the diagnostic "COMMIT_REFUSED"
    And the doer has been called once
    And there is one new work commit
    And it contains the work for the first task
    And the committed plan has no completed tasks

  Scenario: Status documents are not product acceptance evidence
    When the factory runs
    Then it accepts it
    And the validator was given "Task/subtask status updates are permitted; they do not prove the product meets the seed."
    And the committed plan matches the plan on disk
