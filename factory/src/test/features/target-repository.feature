@repository
Feature: Target repository responsibilities
  Scenario: Preparing a standalone target is repeatable
    Given a standalone repository target
    When the target repository is prepared twice
    Then Git is rooted at the target

  Scenario: Product review and task recording respect a containing repository
    Given a repository target with mixed changes in a containing repository
    When the repository collects product changes
    Then the collected changes contain only tracked and new target products
    When the repository records task changes twice
    Then one task commit includes product and plan changes but not outside work
    And outside staged and unstaged changes are preserved
