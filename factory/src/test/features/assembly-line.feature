@assembly-graph
Feature: Assembly-line loading and graph decisions preserve current behavior
  Background:
    Given a copy of the factory
    And a new target
    And a seed describing a game of Tetris
    And the planner plans the tasks alpha and beta
    And the doer does the next task in the plan
    And the validator is always satisfied

  Scenario: Parsing ignores non-edge lines and accepts whitespace and blank labels
    Given this assembly line:
      """
      digraph ignored {
        // not an edge
        start -> planner [ label = " " ];
        planner -> finish;
        nonsense -> absent [color=red];
        doer -> planner;
        ignored text
      }
      """
    When the factory reads the assembly line
    Then it accepts it

  Scenario Outline: Structural refusals and availability checks retain their order
    Given this assembly line:
      """
      <edges>
      """
    When the factory reads the assembly line
    Then it refuses it
    And it reports the diagnostic "<diagnostic>"

    Examples:
      | edges                        | diagnostic                            |
      | absent -> doer                | The assembly line has no start        |
      | start -> absent               | The assembly line has no finish       |
      | start -> absent; INVALID      | The assembly line has no start        |
      | start -> finish               | Only planner may lead to finish       |

  Scenario: Missing machines precede the finish predecessor rule
    Given this assembly line:
      """
      start -> absent
      absent -> finish
      """
    When the factory reads the assembly line
    Then it reports that it has no machine called "absent"

  Scenario: The first missing machine is reported in edge encounter order
    Given this assembly line:
      """
      start -> missing-first
      missing-second -> finish
      """
    When the factory reads the assembly line
    Then it reports that it has no machine called "missing-first"

  Scenario: The last machine unable to finish is reported
    Given this assembly line:
      """
      start -> planner
      planner -> finish
      doer -> validator
      validator -> doer
      """
    When the factory reads the assembly line
    Then it reports that finish cannot be reached from validator

  Scenario: An unreachable-from-start machine is permitted if it can finish
    Given this assembly line:
      """
      start -> planner
      planner -> finish
      doer -> planner
      validator -> doer
      """
    When the factory reads the assembly line
    Then it accepts it

  Scenario Outline: Non-boolean routing fields use the missing-field diagnostic
    Given the validator appends the JSON line '<result>' after its result
    When the factory runs
    Then it reports that the result of validator has no field "satisfied"
    And there are no new commits

    Examples:
      | result                                    |
      | {"satisfied": null, "findings": []}         |
      | {"satisfied": 1, "findings": []}            |
      | {"satisfied": {}, "findings": []}           |

  Scenario: A boolean without a matching edge is refused
    Given this assembly line:
      """
      start -> planner
      planner -> doer [label="not complete"]
      planner -> finish [label="complete"]
      doer -> validator
      validator -> planner [label="satisfied"]
      """
    And the validator is never satisfied
    When the factory runs
    Then it reports the diagnostic "The result of validator does not select an edge"
    And there are no new commits

  Scenario: One unconditional edge takes priority over labeled edges
    Given this assembly line:
      """
      start -> planner
      planner -> doer [label="not complete"]
      planner -> finish [label="complete"]
      doer -> validator
      validator -> planner
      validator -> doer [label="missing"]
      """
    When the factory runs
    Then it accepts it
    And the plan shows every task as done

  Scenario: Multiple unconditional edges do not select a route
    Given this assembly line:
      """
      start -> planner
      start -> doer
      planner -> finish
      doer -> planner
      """
    When the factory runs
    Then it reports the diagnostic "The result of start does not select an edge"

  Scenario: First matching labeled edge wins
    Given this assembly line:
      """
      start -> planner
      planner -> doer [label="not complete"]
      planner -> finish [label="complete"]
      doer -> validator
      validator -> planner [label="satisfied"]
      validator -> doer [label="satisfied"]
      """
    When the factory runs
    Then it accepts it
    And the doer has been called twice
