---
name: run-emulator
description: Runs a headless Android emulator. Use when user asks to run
emulator tests. Use to verify new or fixed tests.
license: GPL-3.0
compatibility: requires kvm access and the Android SDK
---

# Run emulator

## 1. Identify an emulator

List emulators with `emulator -list-avds`. Find a suitable emulator for the
current task. If it is not available, tell the user, DO NOT create an emulator.

## 2. Run the headless emulator

Spawn a new low cost agent, given ${NAME} is the emulator name, the new agent
should run:
```
emulator -avd ${NAME} -no-window -no-audio -no-boot-anim -no-snapshot \
        -no-metrics -gpu swiftshader_indirect \
        -no-skin
```
And if the app is already installed, the agent should uninstall it:
```
adb -s <emulator> uninstall <package>
```

Wait until the spawned agent finished.

