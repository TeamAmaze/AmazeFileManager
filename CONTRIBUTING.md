# Contributing to Amaze

Happy to see you in here! Amaze was created with a vision to provide all the basic and advanced features a file manager in Android is supposed to have, with a friendly material design. As with any other open source project, contributions are the key to achieve this goal of ours. :)

Contributions are always welcome!

> ### Imposter's syndrome disclaimer: We want your help. No, really! Any contribution counts, no matter how small it be!

# How to contribute?

- **Translation**: We use [Transifex](https://www.transifex.com/amaze/amaze-file-manager/) for our translations, feel free to contribute translations there.
- **Monetary Contributions**: You can do monetary contributions via [OpenCollective](https://opencollective.com/TeamAmaze), [LiberaPay](https://liberapay.com/Team-Amaze/donate) or PayPal](https://www.paypal.me/vishalnehra).
- **Bug Reports**: Reporting bugs clearly & concisely helps us improve Amaze. It would be great for us to pinpoint the cause of a bug if there are logs attached to the bug report. Or clearcut steps to reproduce the issue you are facing. Yes, good bug reports are considered as contributions too!
- **Code Contributions**: This file discusses about code contributions.

To start contributing, we assume you know how to use git and write and debug Android apps.

## How to get started?

 - You can use GitHub web interface to fork `TeamAmaze/AmazeFileManager` to `<YOUR-USERNAME>/AmazeFileManager`.
 - The next step is to import it into Android Studio by `New Project -> Get from Version Control`.
 - There you can paste the link to your fork.
 - Let Android Studio import the project & download all the necessary dependencies
 - Now you can build & run the project on an emulator/real device.

## What to do next?

 - Go to [issues]() section & have a look at [good-first-issues](https://github.com/TeamAmaze/AmazeFileManager/issues?q=is%3Aopen+is%3Aissue+label%3A%22Issue-Easy+%28good+first+issue%29%22). These are low hanging fruits ready to be picked up!
 - Have a look at [NPE Crashes](https://github.com/TeamAmaze/AmazeFileManager/issues?q=is%3Aopen+is%3Aissue+label%3ACrash-NullPointerException) too. These must be literally one line fixes to bugs.
 - Or if you wanna work on a feature, please make sure no one's working on it by commenting on the thread (we'll assign it to you then).
 - Once you have made all the necessary changes, and everything works as expected, please run `./gradlew spotlessCheck` on your local and handle any resulting formatting issues. Most of them can be fixed by running `./gradlew spotlessApply` (others would need lil manual changes)
 - If everything looks good, push it to your fork & make a PR (please make sure to fill the PR template!)
 - We'll look into your PR soon, give feedback, and upon the code working as expected (i.e, fixes the bug/implements feature), the code get merged to the next release branch! Yay!

If we feel your PR is a significant help to us, we'll award you a bounty with any of your preferred mode of payment.

## Points to note

 - Please follow [Android/JAVA code style](https://source.android.com/docs/setup/contribute/code-style) for writing any code. Please see [this](https://github.com/TeamAmaze/AmazeFileManager/issues/986) issue too.
 - Also, follow [Android Material Design guidelines](https://m3.material.io/get-started) in case you make changes to any UI element.

## PS: please make sure to

- Follow best practices & to write _clean code_.
- Before opening a PR, run `./gradlew spotlessCheck` on your local and handle any resulting formatting issues. Most of them can be fixed by running `./gradlew spotlessApply` (others would need lil manual changes)
- Fill in the pull request template clearly (for eg: `fixes #XXXX` for bugfixes)
- Once you've opened PR, look out for CI builds (`Checks` section on the top of your PR). If it's all green, you're good to go! Else, please fix the issues specified in the logs (you can get logs by clicking on the failed workflow & then the failed action).
- Include tests (either Unit tests or automated tests like [Robolectric](http://robolectric.org/)/[Espresso](https://developer.android.com/training/testing/espresso/)) if possible to your PR

### Finally!

- We have our day time work, so except security vulnerabilities, your submission may be left cold for a lil while before being picked up by us :)
- Please be patient with us while we review our code. Try to avoid favoritism, hate speech & adhere to our [code of conduct](https://github.com/TeamAmaze/AmazeFileManager/blob/release/4.0/CODE_OF_CONDUCT.md).

## Ready to roll? Start [forking](https://github.com/TeamAmaze/AmazeFileManager/fork)! ;)
