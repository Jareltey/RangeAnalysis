# Range Analysis on Integers using Soot (Java)

This project features range analysis on integers using the Soot framework in Java. By performing static analysis on variables
as they change throughout a program, we can collect information about their ranges (minimum and maximum integer values) at
different program points without ever executing the program. This is informative when there are array indexing operations in
the program, since warnings or errors about negative array indexes or array indexing that exceeds the array length can be raised.

## Requirements
The only prerequisite installation is a JDK between version 8 to 14.
This repo has been tested with Java 14, but it may even work with Java 15.
It will currently *not work* with Java 16 or higher. If you have a different default Java version,
check out tools such as [jEnv](https://www.jenv.be), which help with switching between different
Java versions for use in specific directories.

The project uses the Gradle build system, which ships with a wrapper that will
download all of the project dependencies for you. Building/testing should be as simple as running `./gradlew build` on
a *nix system, or `gradlew.bat build` on Windows. The project should build successfully.

Running `./gradlew test --info` will run range analysis on the test programs in the src/test directory and display the ranges of
local variables at each program point.

## Recommendataions
- We highly recommend an IDE such as [IntelliJ IDEA](https://www.jetbrains.com/idea/) for this project. 
If you'd like to use IDEA, you can import this as a Gradle project directly from GitHub using 
`File -> New -> Project from Version Control... -> Git`. 
