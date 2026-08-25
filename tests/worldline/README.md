# Worldline tests

This isolated Gradle 8.14.4 project compiles the catalog specifications as
ordinary Java 8 and runs them with Worldline TestKit 0.3.0 from the Gradle
Plugin Portal in host-only mode.
It invokes the repository gate before compiling the specs.

Run `./gradlew worldlineDoctor worldlineTest` (or `gradlew.bat` on Windows).
