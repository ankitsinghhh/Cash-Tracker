# Appearance and expense insights

## Where to find the additions

- **Settings → Appearance & Themes → Quiet Studio** selects the new neutral/teal palette. It supports light, dark, system and AMOLED modes. All eight existing palettes, their IDs, their colour definitions and the saved selection remain available. The application does not switch your palette automatically.
- **Settings → Charts & insights → Visible charts** lets you show or hide each of the 19 chart/insight options. Changes persist in Room settings. Show all/Hide all write one database transaction. Newly introduced options default to visible without overwriting older choices.
- **Settings → Charts & insights → Calendar heatmap overlay** independently controls shading in Home's Calendar tab. It defaults to off. The Stats heatmap has its own visibility choice.
- **Stats → Insights** contains the eleven additions below. Existing Stats tabs remain available for their enabled charts. If all charts are hidden, a Choose charts action makes it possible to enable them again.
- **Stats → expand icon** opens the current section fullscreen. Individual donut, daily-trend, cash-flow, weekday and new insight cards also have an expand control. Close or Android Back returns to the inline view.

## Visual changes

Home presents monthly spending as the main figure with income and net cash flow underneath, retaining decimal precision. Transaction rows show a softly tinted category icon, merchant/name hierarchy, category and account metadata, and aligned amounts with tabular digits. Notes remain visible in the metadata when a merchant name is present. Large text uses the existing stacked row layout.

Typography, card shapes, margins and control spacing are more consistent across Accounts, Budget, transaction entry and charts. Loading states use static placeholders with a small activity indicator, and empty/error states use the same illustration treatment. Chart titles and labels compete less with the numbers. Financial colour changes, dark tonal layers and adjusted icon contrast are confined to Quiet Studio; existing palettes retain their financial colours.

The unselected navigation redesign from the aesthetic suggestion list is not part of this change. The additional Insights tab and hiding disabled chart sections support the requested chart controls.

## Added charts and insights

| Addition | What it shows |
| --- | --- |
| Spending heatmap | Recorded daily spending, with a day selector and links to its five largest spending records. |
| Category trends | Six months of stacked category totals; five leading categories plus Other cover all spending. Tap a bar for exact values. |
| Spending change explanation | A waterfall from previous to selected-period spending, identifying category increases/decreases. Five drivers plus Other reconcile to the complete change. |
| Recurring cost trend | Six months of expenses actually posted from recurring rules and installment plans. An expense linked to both counts once under installments. |
| Small purchase impact | Count, total and spending share for purchases at or below an editable threshold. The threshold is saved for each primary currency. |
| Frequency vs purchase size | Six-month bubble plot: purchase count, average purchase amount and total. Tap a bubble or select its label. |
| Merchant habits | Current-month counts, averages and totals for the ten largest merchants. Names are trimmed and matched without case differences. |
| Spending spikes | Days above 1.75 times the mean of active spending days, requiring at least three active days. Contributor links explain the recorded amounts. |
| Annual seasonality | Monthly spending across the current and previous calendar year. Missing records are gaps, and partial months are labelled. Requires records in two years. |
| Savings what-if | A chosen category and reduction percentage show an illustrative saving and remaining spending. Transactions and budgets are not changed. |
| Weekly digest | Latest seven elapsed days, the preceding seven-day comparison, largest category driver and expense, actual monthly budget usage, and next saved bill occurrences due within seven days when viewing the current month. |

Budget forecasting is intentionally excluded. Upcoming bills display saved schedule amounts, and savings scenarios describe the user's chosen reduction in recorded spending. Neither predicts month-end spending.

## Calculation rules

New insights use the selected primary currency's source accounts. Other-currency and missing-account records are omitted, with a count displayed. No currency conversion is inferred. Hidden accounts' historical records remain eligible when their currency matches. Transactions excluded from statistics are omitted. Transfers contribute only their fees; their principal is never spending. Purchase-count and average-purchase charts omit transfers, including their fees. All money is stored/calculated in integer minor units.

Future dates beyond the current day are omitted. A completed selected month compares full months; a partial month compares elapsed days with the previous month, capped at the previous month's last day. The digest crosses month boundaries when needed. An unstarted month has no elapsed-week spending. Comparisons describe entered records, so missing entries affect the results. Spikes do not label a purchase as wasteful or fraudulent.

Recurring trends require a recurring-rule/installment link on the posted transaction; manually entered bills are not guessed from their merchant names. Seasonality distinguishes a month with records but no expenses from a month without any eligible records. Current-month frequency and seasonality are partial, without extrapolation.

## Performance and verification

### Fullscreen chart viewing

Fullscreen is a single overlay in the existing app window. The same chart composition moves between its inline slot and the overlay, retaining selected points, chart modes, controls and effects. The inline slot keeps its measured height to prevent a lazy-list scroll jump. A stable saveable scope restores chart choices when Android recreates the screen while it is expanded. Opening a chart does not create another ViewModel subscription or query.

Plots enlarge within bounded dimensions based on the actual app window, and long legends and details remain vertically scrollable. The title and close control stay above the scrolling content, with safe system-bar insets, portrait/landscape support, an Escape shortcut and Back handling. Keyboard focus moves to Close and returns to the expand control; background content is hidden from accessibility while expanded. Navigation to a different screen, removal of the selected chart, or the existing PIN gate dismisses the financial overlay.

Fullscreen regression coverage exercises repeated opening/closing without effect duplication, preserved selection and list position, Back, saved-state restoration, removal of an open chart, nested section expansion, actual heatmap drilldown, and an interactive donut in landscape. Native desktop-rendered previews include `fullscreen-heatmap.png` and `fullscreen-donut-landscape.png` in `app/build/reports/visuals`.

### Insight preparation

Insight preparation runs on `Dispatchers.Default`, outside composition. SQL date bounds expand only for enabled insights: the selected month, previous month, six months, or at most the current and previous calendar year. The insight flow subscribes only while that Stats view is active; hiding all insights avoids the query. Chart preferences use one shared settings observer, including atomic bulk changes.

Each chart card is a stable lazy item. Charts use small static canvases without continuous animation. Daily drilldowns contain at most five records; spikes at most five days; merchant plots at most ten merchants. Category reduction chips show six choices, with a lazy chooser for remaining categories. Historical plotting data is aggregated before the UI receives it.

Regression tests cover calendar boundaries and daylight-saving time, comparable periods, transfers/fees, excluded records, mixed currencies, merchant matching, scheduled expense double counting, digest boundaries, future months, a 50,000-record insight ledger, saved preferences after reopening the database, interactive heatmap links and lazy scrolling to the digest. Desktop-rendered light/dark, heatmap and digest screenshots are written to `app/build/reports/visuals` with Roborazzi recording enabled.

Local validation on 4 October 2026: all **42 JVM/Robolectric tests passed**, with zero failures, errors or skipped tests, including eight fullscreen regressions. Lint completed with **zero errors, 89 warnings and 16 hints**. Both the debug APK and R8/resource-shrunk `benchmarkRelease` APK built successfully. The light/dark, heatmap, digest and fullscreen portrait/landscape screenshots were rendered and visually reviewed. Tests were run in a fresh process with access to the Android test runtime, followed by a separate fresh lint/build run without disabling checks.

Physical-device frame rates have not been measured for this change. No phone, ADB connection or installation was used for this validation.
