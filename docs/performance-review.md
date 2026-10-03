# Cash Tracker performance review

Reviewed on 3 October 2026. Findings below come from source inspection; no device trace or frame-time benchmark was available.

## Implemented scroll changes

- Prepare the home month once in `MainViewModel.monthPageData`, on `Dispatchers.Default`. The totals strip and animated content now share that result instead of running separate full-history calculations during composition.
- Remove home subscriptions to unused summary/grouping flows, which previously triggered additional calculations and recompositions.
- Move transaction enrichment, account balances, summaries, filtering and analytics transformations off the UI thread. Suppress equal derived results before downstream collection where applicable.
- Give daily headers and transaction rows distinct lazy content types for better reuse.
- Flatten expanded weekly transactions into individual lazy items. Preserve expanded week state outside recycled items, keyed by week number; add bottom clearance for the add button.
- Cache transaction row amount/time strings across selection changes.
- Yield the month swipe recognizer to consumed child gestures and multiple pointers; use a monotonic clock for its cooldown.
- Make financial analytics date formatters thread-local so concurrent background computations cannot corrupt date groups.

These changes target identified sources of unnecessary work. They do not establish a measured frame-rate improvement or a guarantee that every screen is free of jank.

## UI/UX improvements to prioritize

| Priority | Improvement | Reason and relevant code |
| --- | --- | --- |
| High | Move search filtering, grouping and totals into a background result flow; debounce query changes and cancel superseded work. | `SearchFilterScreen.kt` scans, sorts and groups all records inside composition while typing. |
| High | Distinguish loading, empty, missing-record and error states. | Several lists begin with an empty `StateFlow`; account details initially show "Account not found" before accounts arrive. |
| High | Recover from failed saves with an inline explanation and retry; retain the draft. | `AddEditTransactionScreen.kt` has an `isSaving` guard, but `MainViewModel.saveTransaction` provides only a success callback and no failure result. |
| Medium | Keep filters consistent between search and home, show active date bounds and include subcategories. | Search chips primarily change local state; home and search predicates differ. |
| Medium | Preserve list position, selected calendar date and form drafts when switching tabs or rotating. | Several screen states use `remember`; crossfades can dispose the outgoing tab. Scope scroll state to the month/tab, rather than restoring one position into another month. |
| Medium | Improve touch targets, large-text layouts and screen-reader descriptions. | Some month controls are explicitly 32 dp; transaction labels have narrow fixed widths and tiny secondary text. Test long payees, large amounts, font scaling and both themes. |
| Medium | Reduce chart animation restarts and avoid rendering large category/subcategory lists inside one lazy item. | `CustomCharts.kt` includes eager `forEach` content; analytics pages collect results for inactive views. |
| Medium | Clarify navigation and destructive actions. | Rename "Trans" to "Transactions", make account visibility easier to discover, and add undo for deletion where feasible. |

## Backend/data improvements to prioritize

The app uses a local Room database and an optional embedded PC Manager server.

| Priority | Improvement | Reason and relevant code |
| --- | --- | --- |
| High | Fix recurring/installment snapshots, then make each occurrence atomic and safe to retry. | `RecurringProcessor.kt` calls `collect` on never-ending Room flows before its processing loops. `return@collect` only returns from one emission; the loops are never reached during normal collection. Use `first()` for a snapshot, a database transaction for insert/advance, and uniqueness for each occurrence. |
| High | Use bounded date/account database queries and Paging for large histories. | Repository/ViewModel load all transactions and filter them repeatedly. `Daos.kt` already has date/account queries and individual indexes; use those before adding more indexes. |
| High | Make restore/import and related multi-write operations atomic. | `BackupManager.restoreJsonBackup` clears the database and then performs separate writes. Failure can leave partial data and intermediate emissions. Validate first, then use a Room transaction. |
| High | Add explicit migrations and exported schemas. | `AppDatabase.kt` disables schema export and uses destructive migration fallback. Future schema updates should preserve users' financial records. |
| High | Return typed operation errors and reset UI busy states reliably. | Many ViewModel mutations launch writes without an error channel. Handle errors without swallowing coroutine cancellation. |
| Medium | Share repository snapshots and aggregate account balances in one pass or in SQL. | `FinanceRepository.netWorth` can recollect the cold balance flow; `FinancialEngine.calculateAccountBalances` scans all transactions once per account. |
| Medium | Bound PC Manager concurrency, headers, body size and response sizes. | `PCManagerServer.kt` uses a cached thread pool and allocates the declared request length. A 15-second socket timeout already exists; add capacity/request limits and paginated responses. |
| Medium | Use persistent scheduled work for recurring processing and chunk large imports/bulk edits. | Launch-only processing misses periods when the app is closed. Batch writes to limit invalidations and memory pressure. |

## Validation and release gates

- Added `FinancialEngineConcurrencyTest`: eight workers repeatedly check date grouping and totals with different dates.
- Added `ScrollRegressionTest`: a 500-row expanded week remains lazy and expanded after scrolling; vertical gestures scroll without changing months, while horizontal month navigation still works.
- `git diff --check` passed.
- Automated Kotlin/Android tests were not executed. This checkout lacks `gradlew`, `gradlew.bat` and `gradle-wrapper.jar`; no Java, Gradle or Android SDK toolchain was found in the checked local locations.
- Before release, restore a complete build setup and run `:app:testDebugUnitTest` and `:app:lintDebug`.
- Measure scrolling in an optimized release build on a physical mid-range device. Enable R8 with the required reflection rules after validating Moshi-backed backup/import features; add Baseline Profiles and Macrobenchmark coverage for launch, daily scrolling, weekly expansion, search and tab changes.
- Exercise 1,000 / 10,000 / 50,000 records, rapid month changes, simultaneous PC edits, recurring processing and import failures. Record frame times, missed-frame counts, memory and query latency against the same device/build/data baseline. At 60 Hz a frame has roughly 16.7 ms; at 120 Hz roughly 8.3 ms.

## Reference guidance

- [Compose performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
- [Lazy lists, keys and content types](https://developer.android.com/develop/ui/compose/lists)
- [Compose performance and release profiling](https://developer.android.com/develop/ui/compose/performance)
