# Maestro samples

This directory contains samples that are downloaded by the `maestro
download-samples` command.

`maestro download-samples` provides a set of flows and apps so that users can
quickly try out Maestro, without having to write any flows for their own app.

`download-samples` downloads these files and apps from our publicly-available
Google Cloud Storage bucket (hosted on `storage.googleapis.com`).

### Update the samples

The samples are automatically updated by the GitHub Action on every new commit
to the `main` branch.

Although the samples are checked in, updating them requires a few manual steps:

* Change the samples in this directory and merge these changes
* Run `maestro download-samples`
* Copy *.yaml to the samples directory created by download-samples
* Run `(cd samples && zip -r "$OLDPWD/samples.zip" . -x "/**/.*" -x "__MACOSX")`
* Open https://console.cloud.google.com/storage/browser/mobile.dev/samples
* Upload samples.zip
* Adjust the permissions of samples.zip to "Public to Internet"
* Run `maestro download-samples` and verify that the change was successful
