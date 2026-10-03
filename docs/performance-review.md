# Cash Tracker performance improvements

Updated 4 October 2026. This change implements the source-level improvements identified in the initial review. Physical-device frame measurements and generated application Baseline Profiles remain pending; no phone was accessed or app installed during this work.

## UI and scrolling

| Review item | Implementation |
| --- | --- |
| Repeated work on the UI thread | The home month is prepared once on Default from a bounded database date query. Enrichment, filtering, grouping, balances and analytics run in background flows; derived snapshots are shared. |
| Expanded weekly scrolling | Individual transaction rows are lazy items with stable keys and content types. Expanded-week state survives recycling and saved-state restoration. Vertical gestures yield to scrolling; horizontal gestures retain month navigation. |
| Compose compatibility | Compose BOM 2025.07.00 aligns UI and test libraries on Compose 1.8.3, including the simulator-aware lazy-list prefetch scheduler. |
| Search stalls | Debounced, cancellable SQL filters with 50-record Paging pages. Totals and counts cover all matches through an aggregate query. Account history also uses Paging. |
| Loading, missing and failure states | Initial loading is distinct from an empty list or deleted record. Database errors expose retry; pagination exposes initial and append errors. The navigation host stays mounted when an error dialog appears. |
| Save errors | Save validation and transaction/bookmark writes are atomic. Busy state resets in finally, errors reach the draft, and successful saves navigate only after completion. |
| State and filter consistency | Saveable drafts, selected calendar date, expanded weeks and tab/month list state. Search and home share query/type/account/category/subcategory/receipt/date/payee/tag predicates. Date bounds appear as chips. |
| Accessibility | 48 dp month controls, readable secondary text, selected semantics, explicit descriptions and a stacked transaction layout at large font scale. |
| Charts | Only active analytics subscribe. Category, comparison and drilldown lists use separate lazy rows; rank bars retain a common scale. Decorative chart animations no longer restart from zero on every data refresh. |
| Navigation and deletion | Transactions replaces the abbreviated navigation label. Accounts is visible by default for new setups, while saved preferences are retained. Transaction deletion offers Undo. |

## Data and backend

| Review item | Implementation |
| --- | --- |
| Recurring/installment processing never reached | Use first() for snapshots, then re-read each rule inside an insert/advance transaction. A unique occurrence key makes concurrent runs and retries safe. Catch-up work is bounded to 128 occurrences per rule per run. |
| Persistent processing | WorkManager schedules unique launch work and a 12-hour periodic job with bounded retries. Android controls execution time; this is not an exact alarm. |
| Large histories | Home and comparison queries bound dates; search/account history page 50 records. Monthly history uses SQL aggregates. Account balance calculation scans the ledger once, instead of once per account. |
| Partial restore/import/bulk operations | Validate backups before clearing, including duplicate scheduled occurrences; restore, imports, onboarding settings and bulk operations run in Room transactions. CSV rows stream into 500-record batches, with complete rollback on a bad row. Named CSV columns prevent optional fields from reading unrelated values. CSV exports retain cents. |
| Database upgrades | Schema version 2 adds occurrence uniqueness through an explicit 1-to-2 migration; destructive migration fallback is removed. Exported Room schema and a frozen v1 SQL fixture provide migration regression coverage. |
| Errors and cancellation | Mutations expose an error channel, save/import busy states recover, and coroutine cancellation is propagated. Read failures expose a retry path. |
| PC Manager load | Four executor threads and a 16-client queue, 15-second socket timeout, header/body limits, 32 MiB response limit, closed connections on stop, and 100-record default / 500-record maximum pages. Refreshes read a consistent database snapshot; exports avoid replaying cached UI data. PC history shows loaded/total counts and Load more. |
| Release performance | R8 and resource shrinking enabled, Moshi reflection keep rules included, unused AI/network dependencies removed, wrapper restored, Java 21 CI added. Macrobenchmarks and the Baseline Profile generator cover launch, daily scroll, weekly expansion, search and stats navigation. |

## Verification

Regression coverage includes concurrent date formatting, lazy weekly scrolling, gesture direction, saved-state restoration, concurrent recurring processing, installment completion, restore/import rollback, SQL search totals, transfers/fees/liability balances, a 50,000-record ledger, the v1 database migration, failed-save retry and PC request/page limits.

See [build and performance instructions](build-and-performance.md) for commands, profile capture and device measurement. Local validation results are recorded there after the final build.

## Remaining measurements

Run the optimized build on a dedicated mid-range Android device with 1,000, 10,000 and 50,000 records. Compare frame-duration percentiles, missed frames, startup time, memory and query latency on the same device and dataset before/after. Test rapid month changes, large fonts, light/dark themes, simultaneous PC edits and import failures. Generate and commit the application Baseline Profile, then rerun measurements with and without it. The implemented changes remove identified sources of excessive work; they do not establish a measured frame-rate improvement or guarantee zero jank.

Reference guidance: [Compose performance](https://developer.android.com/develop/ui/compose/performance/bestpractices), [Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile), [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview).
