# Android setup (Samsung Galaxy S24)

1. Install Android Studio, SDK Platform 35, SDK Build Tools and JDK 17.
2. Clone `https://github.com/mbkurt06/fungi-radar.git` and open the root directory.
3. Let Android Studio download Gradle and sync dependencies.
4. In Samsung Settings > About phone > Software information, tap Build number 7 times to enable Developer options; enable USB debugging.
5. Connect USB, authorize the computer, choose the phone as target, press Run.
6. Grant Camera permission. GPS is optional and requested only when tapping the GPS button.
7. The live preview should start. **No AI detection is possible until a compatible model is added.**

### Notes
- Requires Android 8.0+.
- First Gradle sync needs internet; the installed app does not need an internet connection.
- There is no Gradle wrapper JAR in this source-only initial commit. Android Studio can generate a wrapper or run a locally installed Gradle 8.9. See `gradle/wrapper/gradle-wrapper.properties`.
- Real device verification and camera overlay crop calibration are still outstanding.
