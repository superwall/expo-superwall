## Superwall integration & screenshot tests

This module contains the Superwall integration tests and screenshot tests, using the [Dropshot](https://github.com/dropbox/dropshots) library
for screenshot testing. The tests need to run on an Android device, emulator or a device lab (such as Firebase Test Lab).
Note: currently screenshots are recorded only for Pixel 7, API 33. Devices with different resolutions might fail.

### Running the tests

To run the tests, just run `./gradlew :app:connectedCheck` from the root of the project.
This will run the integration tests on your adb connected device or emulator.

### Viewing tests results

The tests results will be saved under `app/build/reports/androidTests/connected` folder in this directory.
If there are failing tests, the screenshots will be saved under `app/build/outputs/androidTest-results/connected/debug` folder.

### Recording the screenshots

To record the screenshots, run `./gradlew :app:connectedCheck -Pdropshots.record` from the root of the project.
This will record new screenshots on your current device.

### Viewing the recorded screenshots

To recorded screenshots are saved under `screenshots` folder in this directory.




