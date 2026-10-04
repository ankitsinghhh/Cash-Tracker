# Haptics and device-folder CSV backups

## Haptic controls

Open **More → General Preferences → Haptic feedback**. The switch enables or disables app feedback, and the slider sets strength from 0 to 100 percent in five-percent steps. Zero is silent. Turning feedback off retains the chosen strength. Test vibration plays one sample at the saved setting. Both settings persist in Room.

The root composition supplies one haptic controller to calculator keys, the PIN screen and Compose touch gestures. Calculator keys no longer stack three separate vibration calls. Disabled feedback does not invoke the vibrator. The controller also respects Android's touch-feedback setting. Devices with amplitude control use the selected amplitude; other devices vary the brief pulse duration. The initial controller stays silent until saved settings have loaded.

Strength describes an app setting, not a calibrated physical measurement. The sensation depends on the device's motor and Android settings. Implementation follows the [Android haptics API documentation](https://developer.android.com/develop/ui/views/haptics/haptics-apis).

## CSV backup location

Open **More → Backup & Restore → CSV backup to device folder**. Choose folder opens Android's system folder picker. Pick a writable folder on the device and grant access. The application saves the folder URI/name and takes the persistable read/write grant so subsequent backups can use it after restart. Change folder opens the picker again.

**Back up CSV** reads a complete, consistent transaction snapshot directly from Room, then writes a UTF-8 spreadsheet into that folder off the UI thread. It includes records outside the selected month and transactions from all accounts, preserving each source account's currency. The standard CSV includes transfer fees, quoted notes, payees and tags. A timestamp and random suffix give each backup a distinct filename. Saving disables repeat saves and folder changes; success reports the saved filename and folder.

A storage failure removes the incomplete new document where the provider permits deletion. Earlier backups are never overwritten or removed. Missing/revoked folder access produces an error and asks the user to choose the folder again. Android may require a subfolder rather than a restricted storage root. The feature uses the [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files), without broad storage permissions or guessed filesystem paths.

CSV backs up the transaction spreadsheet. The existing JSON backup remains the complete app-data backup for categories, budgets, recurring plans and other ledger records. Existing sharing/export/import actions remain available; CSV sharing now also reads the database snapshot directly.

## Fullscreen control

The separate expand button in the Stats top bar has been removed. Use the expand icon beside an individual chart/card. The existing fullscreen overlay, fixed close button, Back handling and preserved chart/list state remain in place.

## Validation

Regression coverage includes off/zero/system-disabled haptics, one pulse per calculator key, slider/switch/preview behavior, saved choices after database reopening, transaction CSV snapshots and fee/quoted-note imports, actual document-provider folder writes and retained URI grants, distinct backup files, failed-write cleanup, read-only/revoked folders, backup button states, and the real Stats screen's single chart expand action.

Validation on 4 October 2026: all **57 JVM/Robolectric tests passed**, with zero failures, errors or skipped tests. Lint completed with **zero errors, 90 warnings and 16 hints**. Debug and R8/resource-shrunk `benchmarkRelease` APKs built successfully. Desktop-rendered haptic settings, CSV backup settings and Stats previews were visually reviewed. These checks do not access a physical phone; device motor strength and real storage-provider behavior need physical-device validation.
