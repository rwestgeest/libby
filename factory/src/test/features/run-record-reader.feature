Feature: Reading a growing run record
  Scenario: Unicode output and partial appends retain every complete event
    Then the run record reader preserves Unicode and waits for complete appended events
