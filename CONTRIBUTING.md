Amaze was created with a vision to provide all the basic and advanced features a file manager in Android is supposed to have, with a friendly material design.
As with any other open source project, contributions are the key to achieve this goal of ours.

Please keep in mind below few points before considering contributing to Amaze.
- The source code has two flavours, ````fDroid```` and ````play````. 
Both of these include their own MainActivity. ````fDroid````'s MainAcitivity is generally outdated and is only updated during new release.
Hence you're requested to make changes and create a PR with changers only to ````play's```` MainActivity.
Any changes made to [fDroid's MainActivity](https://github.com/TeamAmaze/AmazeFileManager/blob/master/app/src/fdroid/java/com/amaze/filemanager/activities/MainActivity.java) will be overwritten. 
So please make sure to compile and work on [play's MainAcitivty](https://github.com/TeamAmaze/AmazeFileManager/blob/master/app/src/play/java/com/amaze/filemanager/activities/MainActivity.java).
- Make sure to write your name and Email-ID in format ````Name<email>```` in the license declaration above every file you make change to.
You won't be able to claim the license for changes made by you unless you do that. 
If there's no license header in any file, please include one from [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.en.html) webpage.
- Please follow [Android/JAVA code style](https://source.android.com/source/code-style.html) for writing any code. 
And [Android Material Design guidelines](https://material.io/guidelines/material-design/introduction.html) in case you make changes to any UI element.
- To file a bug report, it is recommended to capture a bug report along with reporting the steps to reproduce it. 
It is also recommended to enroll to our beta program from Play Store to test and verify any fix for the same.
