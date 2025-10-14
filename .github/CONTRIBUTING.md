# Contributing to Superwall

We want to make contributing to this project as easy and transparent as
possible, and actively welcome your pull requests. If you run into problems,
please open an issue on GitHub.

## Pull Requests

1. Fork the repo and create your branch from `develop`.
2. Run the setup script `./scripts/setup.sh`
3. If you've added code that should be tested, add tests.
4. If you've changed APIs, update the documentation.
5. Ensure the test suite passes.
6. Make sure your code lints (see below).
7. Tag @ianrumac or @yusuftor in the pull request.
8. Add an entry to the [CHANGELOG.md](../CHANGELOG.md) for any breaking changes, enhancements, or bug fixes.
9. After the PR is merged, delete the branch.

## Coding Style


### Ktlint

To maintain readability and achieve code consistency, we follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
and [Android's Kotlin style guide](https://developer.android.com/kotlin/style-guide).
We use [Ktlint](https://github.com/pinterest/ktlint) to check for style errors.

To do this locally, you can either run the setup script or install ktlint using `brew install ktlint` and either:
* Run `ktlint` from the root folder to detect style issues immediately.
* Use `ktlint installGitPreCommitHook` to install a pre-commit hook that will run ktlint before every commit.

### IntelliJ IDEA

To configure IntelliJ IDEA to work with ktlint, you can use one of the following approaches:

* Install the [ktlint plugin](https://plugins.jetbrains.com/plugin/15057-ktlint) and follow the instructions.
* Use the IntelliJ configuration and follow the official [Ktlint Guide](https://pinterest.github.io/ktlint/latest/rules/configuration-intellij-idea/) to avoid conflicts with IntelliJ IDEA's built-in formatting.

### Documentation

Public classes and methods must contain detailed documentation.

## Editing the code

Open the module: `./superwall/` containing the SDK itself.

For the example app's and test app's, open either the:
* `./app/` for the testing application
* `./example/` for the example application


## Git Workflow

We have two branches: `master` and `develop`.

All pull requests are set to merge into `develop`, with the exception of a hotfix on `master`.

Name your branch `author/feature/<feature name>` for consistency.

When we're ready to cut a new release, we update the `version` in [build.gradle](/superwall/build.gradle.kts) and merge `develop` into `master`. 

## Testing

If you add new code, please make sure it gets tested! When fixing bugs, try to reproduce the bug in a unit test and then fix the test. This makes sure we never regress that issue again.
Before creating a pull request, run all unit and integration tests. Make sure they all pass and fix any broken tests.
We also have a GitHub Action that runs the tests on push.

## Issues

We use GitHub issues to track public bugs. Please ensure your description is clear and has sufficient instructions to be able to reproduce the issue.

## License

By contributing to `Superwall`, you agree that your contributions will be licensed under the LICENSE file in the root directory of this source tree.
