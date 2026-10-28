# Intent: Log visit bookings through the logging framework
Author: Eval Author

## Problem
`VisitScheduler.book` writes each booking to standard output with `System.out.println` (Sonar rule java:S106). The output bypasses log levels and the log pipeline, and it includes owner names.

## Proposed outcome
Bookings are logged through the application's logger at an appropriate level, and Sonar no longer reports java:S106 in `VisitScheduler`. Out of scope: other logging changes.

## Affected users and systems
Operators reading the logs; `VisitScheduler`.

## Constraints
No behaviour change for users.

## Open questions
None.
