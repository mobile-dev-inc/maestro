[<img src="./assets/run-on-robin.png" width="300px"/>](https://robintest.com/)

# Maestro

Maestro is the easiest way to UI testing for your mobile or web app.

<img src="https://user-images.githubusercontent.com/847683/187275009-ddbdf963-ce1d-4e07-ac08-b10f145e8894.gif" />

## Why Maestro?

Maestro is built on learnings from its predecessors (Appium, Espresso, UIAutomator, XCTest)

- Built-in tolerance to flakiness. UI elements will not always be where you expect them, screen tap will not always go through, etc. Maestro embraces the instability of mobile applications and devices and tries to counter it.
- Built-in tolerance to delays. No need to pepper your tests with `sleep()` calls. Maestro knows that it might take time to load the content (i.e. over the network) and automatically waits for it (but no longer than required).
- Blazingly fast iteration. Tests are interpreted, no need to compile anything. Maestro is able to continuously monitor your test files and rerun them as they change.
- Declarative yet powerful syntax. Define your tests in a `yaml` file.
- Simple setup. Maestro is a single binary that works anywhere.

## Build and Run Instructions
Build and Package

To build and package the project, run the following commands:

./gradlew build --stacktrace

./gradlew build

./gradlew jar

The JAR file will be generated in the build/libs directory.

## Run the Application

To execute the application, run:

java -jar build/libs/maestro-0.1.0.jar

## Run Tests

To run all tests, execute:

./gradlew test

## Publish 

If you have configured publishing for this project, you can publish it with:

./gradlew publish

## Resources

### Documentation

**Available at [maestro.mobile.dev](https://maestro.mobile.dev)**

To get more background on why we built Maestro, read [the Maestro announcement blogpost][blogpost].

### Community

We invite everyone to [join our public Slack channel](https://docsend.com/view/3r2sf8fvvcjxvbtk).

### Contributing

Maestro is an open-source project and we love getting contributions.

To get started, take a look at [issues with the `good first issue` label][good first issues] and [the contributing guide](./CONTRIBUTING.md).

### Built by

[mobile.dev](https://www.mobile.dev)

![](./logo.png)

[good first issues]: https://github.com/mobile-dev-inc/maestro/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22
[blogpost]: https://blog.mobile.dev/introducing-maestro-painless-mobile-ui-automation-bee4992d13c1
