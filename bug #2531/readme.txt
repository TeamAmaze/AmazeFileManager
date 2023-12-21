crash_stack_#3075成功复现一次，但后续不能稳定复现，未能录制视频。
大致复现操作为一次删除多个项目（包括文件和文件夹），随后软件自动刷新导致崩溃。

## Issue explanation (write below this line)

## Exception
* __App Name:__ Amaze File Manager
* __Package:__ com.amaze.filemanager.debug
* __Version:__ 3.6.5
* __User Action:__ UI Error
* __Request:__ Application crash
* __OS:__ Linux Android 9 - 28
* __Device:__ generic_x86_arm
* __Model:__ AOSP on IA Emulator
* __Product:__ sdk_gphone_x86_arm
<details><summary><b>Crash log </b></summary><p>

```
java.lang.NullPointerException: null cannot be cast to non-null type kotlin.String
	at com.amaze.filemanager.asynchronous.handlers.FileHandler.handleMessage(FileHandler.kt:53)
	at android.os.Handler.dispatchMessage(Handler.java:106)
	at android.os.Looper.loop(Looper.java:193)
	at android.app.ActivityThread.main(ActivityThread.java:6669)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:493)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:858)

```
</details>
<hr>


crash_stack_#2531
复现成功一次，后续出现无法退出对压缩包的浏览的未提及bug。
