name: Report a bug
description: You have a problem with Maestro.
body:
  - type: markdown
    attributes:
      value: |
        Thank you for using Maestro.

        Before creating a new issue, please first search the 
        [existing issues database] first and make sure it wasn't reported
        before.

        ---

        If you are sure that you have found a bug that hasn't been reported yet,
        or if our documentation doesn't have an answer to what you're looking
        for, then please fill out the template below.

        [existing issues]: https://github.com/mobile-dev-inc/maestro
  - type: checkboxes
    attributes:
      label: Is there an existing issue for this?
      description: |
        Please search to see if an issue already exists for the bug you encountered.
      options:
      - label: I have searched the existing issues and didn't find mine.
        required: true
  - type: textarea
    attributes:
      label: Steps to reproduce
      description: |
        Create a [minimal, reproducible example] that:

        1. Demonstrates the problem
        2. Explains how to reproduce the problem with detailed step-by-step
        instructions

        **In addition to the detailed step-by-step instructions**, you must include
        information about the device you're encountering the issue on
        (e.g. physical Android or iOS simulator), and the OS version
        (e.g. Android 9, Android 14 with Play Services, or iOS 18).

        Aside from the mandatory information, include as much additional details
        as possible to make it easier for us to understand and fix the problem.
        Screenshots and videos are welcome.

        **Issues that cannot be reproduced will be closed.**

        [minimal, reproducible example]: https://stackoverflow.com/help/minimal-reproducible-example
      placeholder: |
        1. Clone https://github.com/your_username/your_repo_with_bug and `cd` into it
        2. Start Android emulator (Pixel 7, API 34, with Google Play)
        3. Build app: `./gradlew :app:assembleDebug`
        4. Run the problematic flow and see it fail: `maestro test .maestro/flow.yaml`
    validations:
      required: true
  - type: textarea
    attributes:
      label: Actual results
      description: Please explain what is happening.
    validations:
      required: true
  - type: textarea
    attributes:
      label: Expected results
      description: Please explain what did you expect to happen.
    validations:
      required: true
  - type: textarea
      attributes: About app
      description: |
        Include information about the app you're testing.

        ### Info that is usually useful
        
        - Is this an open source or closed source project?
          - If open source, please share link to the repo
          - If closed source, please share app binary and/or an isolated,
            reproducible sample
        - Is this a native or cross-platform app?
        - Framework used to build the app
          - e.g. UIKit, SwiftUI, Android Views, Compose, React Native, SwiftIUI, or NativeScript
          - If applicable, version of the framework (e.g. Flutter 3.22.0, Compose 1.62.0)

      placeholder: |
        The info you enter here will make it easier to help resolve your issue.
  - type: textarea
      attributes: About environment
      description: |
        Include information about machine you're running Maestro on:
        
        - OS and its version (e.g. macOS 13.1 Ventura, Ubuntu 24.04)
        - Processor architecture (x86_64, arm64)
        
      placeholder: |
        The info you enter here will make it easier to help resolve your issue. 
  - type: textarea
    attributes:
      label: Logs
      description: |
        Include the full logs of the command you're running. The zip files
        created with `maestro bugreport` can be uploaded here as well.

        ### Things to keep in mind

        - If you're running more than single command, include its logs in a
          separate backticks block.

        - If the logs are too large to be uploaded to Github, you may upload
          them as a `txt` file or use online tools like https://pastebin.com and
          share the link.

        - **Do not upload screenshots of text**. Instead, use code blocks or the
          above mentioned ways to upload logs.

        - **Make sure the logs are well formatted**. If you post garbled logs, it
          will make it harder for us to help you.
      value: |
        <details>
        <summary>Logs</summary>

        ```
        <!-- Replace this line with your logs. *DO NOT* remove the backticks! -->
        ```

        </details>
  - type: input
    attributes:
      label: Maestro version
      description: Provide version of Maestro CLI where the problem occurs.
      placeholder: 1.36.0
    validations:
      required: true
  - type: dropdown
    id: download
    attributes:
      label: How did you install Maestro?
      options:
        - install script (`curl | bash`)
        - Homebrew
        - built from source (please include commit hash in the text area below)
        - other (please specify in the text area below)
      default: 0
    validations:
      required: true
  - type: textarea
    attributes:
      label: Anything else?
      description: |
        Links? Other issues? StackOverflow threads? Anything that will give us
        more context about the issue you are encountering will be helpful.

        Tip: You can attach images or log files by clicking this area to highlight it and then dragging files in.
    validations:
      required: false
  - type: markdown
    attributes:
      value: |
        ---
        Now that you've filled all the required information above, you may
        create the issue.

        **Please check what your issue looks like after creating it**. If it
        contains garbled code and logs, please take some time to adjust it so
        it's easier to parse.

        Try reading your issue as if you were seeing it for the first time. Does
        it read well? Is it easy to understand? Is the formatting correct? If
        not, please edit it and make it look right.

        Thank you for helping us improve Maestro and keeping our issue tracker
        in a good shape!
