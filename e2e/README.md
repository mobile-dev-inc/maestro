# e2e

This directory contains glue code for testing Maestro itself.

## Testing

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

## Samples

This directory also contains samples that are downloaded by the `maestro download-samples` command,
and some glue code to facilitate updating those samples.

`maestro download-samples` provides a set of flows and apps so that users can
quickly try out Maestro, without having to write any flows for their own app.

`download-samples` downloads these files and apps from our publicly-available
Google Cloud Storage bucket (hosted on `storage.googleapis.com`).

### Intro

The samples are automatically updated by the GitHub Action on every new commit
to the `main` branch.

There zip archive that is downloaded by `download-samples` consists of 2 things:
- the Maestro workspace with flows (located in the `workspaces/wikipedia` directory)
- the app binary files that are used in the flows (located in the `apps` directory)

App binary files are heavy, so we don't store them in the repository. Instead, they are hosted
on publicly available directory in Google Cloud Storage:

### Update the samples

Move files To generate the zip archive:

```console

```

Then use `gsutil` to upload the zip archive to the Google Cloud Storage bucket.

```console
gsutil cp samples.zip "gs://mobile.dev/samples/samples.zip"
```

gsutil acl ch -r -u AllUsers:R gs://mobile.dev/samples/e2e_apps

Although the samples are checked in, updating them requires a few manual steps:

* Change the samples in this directory and merge these changes
* Run `maestro download-samples`
* Copy *.yaml to the samples directory created by download-samples
* Run `(cd samples && zip -r "$OLDPWD/samples.zip" . -x "/**/.*" -x "__MACOSX")`
* Open https://console.cloud.google.com/storage/browser/mobile.dev/samples
* Upload samples.zip
* Adjust the permissions of samples.zip to "Public to Internet"
* Run `maestro download-samples` and verify that the change was successful
