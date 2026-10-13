Feature: Fan out keeps branch jobs independent and stops active siblings
  Background:
    Given a copy of the factory
    And a new target, with a seed describing a game of Tetris
    And the target has the machines planner, doer and validator
    And the target has an assembly line "careful" on which the doer's work is validated
    And a run named "tetris", on the "careful" line, with that seed and target
    And the three big brains has replaced the validator on the "careful" line
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And every reviewer is always satisfied
    And the synthesiser is always satisfied
    And a plan with one task, not done

  Scenario: Branches can do different jobs
    Given each branch has its own job
    When the factory runs the "tetris" run
    Then each branch received only its own job
    And there is one new work commit

  Scenario: A missing result stops reviewers still at work
    Given the other reviewers are held at work
    And the second reviewer answers in prose, with no result
    When the factory runs the "tetris" run
    Then it reports that it could not read reviewer_2's result
    And the held reviewers have stopped
    And the synthesiser has not been called
    And there are no new commits

  Scenario: A crashed branch stops reviewers still at work
    Given the other reviewers are held at work
    And the second reviewer crashes
    When the factory runs the "tetris" run
    Then it reports the diagnostic "The reviewer_2 exited unsuccessfully"
    And the held reviewers have stopped
    And the synthesiser has not been called
    And there are no new commits
