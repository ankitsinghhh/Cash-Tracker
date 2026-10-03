# Build and performance checks

Use Java 21, Android SDK platform 36.1 and build tools 36.0.0. Set sdk.dir in the untracked local.properties file or ANDROID_HOME. The Gradle wrapper is included. Release signing uses KEYSTORE_PATH, STORE_PASSWORD and KEY_PASSWORD; ordinary debug/benchmark builds use the standard debug key.

In a Windows local.properties file, escape the drive colon and use forward slashes, for example `sdk.dir=C\:/Android/Sdk`.

## Local and CI validation

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleBenchmarkRelease :benchmarks:assembleBenchmarkRelease --console=plain
```

These tasks build and run JVM/Robolectric tests without connecting to a phone. CI runs the same checks and retains reports/APKs. The benchmark app uses release optimization with a debug signature solely for local measurement. Its signing identity differs from a published release; use a dedicated test install.

Local validation on 4 October 2026: all 20 JVM/Robolectric tests passed (zero failures or skipped tests). Lint passed with zero errors, 89 warnings and 16 hints. Debug APK, R8/resource-shrunk benchmarkRelease APK and the benchmark test APK all built successfully. PC Manager JavaScript passed `node --check`. Compose scrolling and draft tests use API 35; database, migration, save and PC server tests use API 36. The Compose 1.8.3 update fixes the simulated lazy-list prefetch loop observed with 1.7; this is functional regression coverage, not a device frame-rate measurement.

## Explicit device measurement

Only run these commands when the device is available for testing. They install and use the device; they were not run for this change.

Prepare a dedicated test install with onboarding complete, PIN disabled, the daily view selected and a populated month. Never seed/reset a user's live financial database. Use deterministic 1,000 / 10,000 / 50,000-record CSV fixtures. Close other device tooling, and keep the same device, month, brightness and thermal conditions for comparison.

```powershell
.\gradlew.bat :benchmarks:connectedBenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.benchmarks.CashTrackerBenchmarks
```

Cold launch captures StartupTimingMetric; the scroll/navigation journey captures FrameTimingMetric. Store raw JSON and Perfetto traces from benchmarks/build/outputs. Compare p50/p90/p95/p99 frameDurationCpuMs and frameOverrunMs, startup timings, missed frames and memory. At 60 Hz a frame budget is about 16.7 ms; at 120 Hz it is about 8.3 ms.

## Generate application Baseline Profiles

The Baseline Profile Gradle plugin links the producer module to the app. Automatic capture during ordinary builds is disabled so building cannot claim a connected phone. No application-specific profile was fabricated or captured in this change.

On an available dedicated test device running API 33 or later (or an appropriate emulator), with the prepared dataset:

```powershell
.\gradlew.bat :app:generateReleaseBaselineProfile -Pandroid.testInstrumentationRunnerArguments.class=com.example.benchmarks.CashTrackerBaselineProfile
```

The plugin copies the generated profile to the app's generated baselineProfiles source directory. Review and commit that generated text, rebuild the optimized app, and compare CompilationMode.None() with CompilationMode.Partial() measurements. The generator fails clearly if onboarding/PIN or an empty month prevents the intended journey.

See the official [Baseline Profile generation](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile) and [Macrobenchmark guide](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview) for device requirements and interpreting measurements.
