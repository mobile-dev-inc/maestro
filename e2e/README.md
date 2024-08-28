# e2e

This directory contains glue code for testing Maestro itself.

Typical workflow is:

1. Start Android emulator and iOS simulator
2. `download_apps`
3. `install_apps`
4. `run_tests`

We try to keep shell code in separate files, so we don't get too tightly coupled
to GitHub Actions.

### Expected failures

Let's say a critical bug is introduced that causes Maestro to always mark all
tests as passed. If our e2e test suite only was only checking if all tests pass
(i.e. `maestro test` exit code is 0), then wouldn't catch such a bug.

To prevent this, all flows in this directory MUST have a `passing` or `failing`
label, so the correct outcome can be asserted.
