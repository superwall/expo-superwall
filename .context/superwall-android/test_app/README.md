# Superwall Test App

This is a Testing app for automated UI tests of Superwall Flutter plugin

## Running Maestro Tests

Maestro is a UI testing framework that allows writing and running UI automation tests in YAML format. This README explains how to run the Maestro tests in this directory.

## Prerequisites

1. Install Maestro CLI
   ```bash
   # macOS
    brew tap mobile-dev-inc/tap
    brew install maestro
   ```

2. Ensure you have a running simulator/emulator or connected physical device.

## Running Tests
Most tests require you to be logged in into Google Play.
The flows will automatically run the login flow if the user is not logged in already.
To purchase products, the account has to be listed in allowed Licensed Testers. 

To succesfully log in, you need to pass in the email and password of the account by appending
extra arguments to the command:
`-e TEST_USER=<email here> -e TEST_PW=<password here>`

### Run a single test

```bash
maestro test test_app/maestro/flow.yaml 
```

### Run all tests

```bash
maestro test test_app/maestro/
```

### Run tests with specific tags

```bash
maestro test -t tagname test_app/maestro/
```


### Run tests for specific SDK versions
From the root directory, run:
`./gradlew :test_app:buildForMaestro -Psdk-version=2.5.0 -Penv=dev`
with the SDK version (`2.*.*` onwards) and environment you want to test.
Supported enviroments are:
- dev
- release
- custom (add argument `-Pendpoint=<endpointHere>`)

### Run the test matrix
To run the test matrix, you will need to run the `./github/workflows/maestro-run.yml` workflow,
which will run a list of sdk versions on maestro cloud.


## Test Structure

This directory contains the following types of tests:
- Main flow tests (.yaml files)
- Helper flows that can be reused
- Specialized directories for specific testing scenarios:
    - `handler/`: Handler-related tests
    - `delegate/`: Delegate-related tests
    - `purchasecontroller/`: Purchase controller tests


For more detailed information, refer to the full Maestro documentation. 