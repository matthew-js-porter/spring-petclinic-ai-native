# Intent: Upgrade embedded Tomcat
Author: Eval Author

## Problem
Snyk reports that the embedded Tomcat, `tomcat-embed-core` 11.0.0, has a known high-severity vulnerability (SNYK-EVAL-OSS-1003, request smuggling). The clinic app runs on it in production.

## Proposed outcome
The app runs on a Tomcat release without SNYK-EVAL-OSS-1003, and the Snyk dependency scan no longer reports it. Out of scope: other dependency upgrades.

## Affected users and systems
Everyone using the app; the build's dependency set.

## Constraints
No behaviour change for users.

## Open questions
None.
