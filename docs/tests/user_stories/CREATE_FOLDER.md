---
title: Create a new folder  
source: <Not implemented>
---

## Story

As a user, I want to create a new folder in the Documents folder of my internal storage, to
organize my files.

## Preconditions
- Storage permission granted
- App opened at internal storage root
- There exists a Documents folder in the internal storage

## Scenarios

Scenario: Folder is created with a valid name  
    Given I am viewing the internal storage  
    And the folder "Documents" does not contain a folder named "Test Folder"  
    When I navigate to the "Documents" folder, I tap the FAB, choose folder,
        enter "Test Folder" and press create  
    Then "Test Folder" appears in the file list  

Scenario: Empty name is rejected  
    Given the new folder dialog is open  
    When I type the folder name and then clear it  
    Then the create button is disabled  
    And no folder can be created  
    And an error message shows up in the dialog

Scenario: Duplicated name is rejected  
    Given the new folder dialog is open  
    When I use a name that is already a file or a folder name  
    Then the create button is disabled  
    And no folder can be created  
    And an error message shows up in the dialog