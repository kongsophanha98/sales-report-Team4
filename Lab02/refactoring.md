# Refactoring note: FatReportController

`ui/fx/legacy/FatReportController.java` was copied from the lab as a deliberate
anti-example. Its `onLoad()` handler reads files, parses CSV, filters by branch and
adds up revenue inside a JavaFX event handler. This note lists what is wrong with it
and what changed.

## Smells and their consequences

| # | Smell in `onLoad()` | Consequence |
|---|---------------------|-------------|
| 1 | `Files.list(dir)` is never closed (no try-with-resources) | The directory handle leaks; on Windows the month folder stays locked until the JVM exits. |
| 2 | No row validation: `new BigDecimal(c[6])` and arithmetic on whatever is in the file | A row with quantity -1 is not rejected: it silently **subtracts** 6.50 from the revenue. Nobody is told. |
| 3 | `catch (Exception e)` shows only `"Error"` | The user cannot tell a missing folder from a malformed number or a bad month. The cause is swallowed and there is nothing to debug with. |
| 4 | I/O, parsing, filtering and totals all live in a JavaFX handler | The logic cannot be tested without a toolkit (0 headless tests), duplicates `CsvMonthReader`/`DefaultSalesAggregator`, and every change to a business rule means editing UI code. |
| 5 | `branchBox.getValue()` is read inside the row loop | Widget access per row ties the loop to the FX thread and mixes presentation with data work; it also re-reads state that could change mid-loop. |
| 6 | `split(",")` without a limit of -1 | Trailing empty fields are dropped, so a short row throws `ArrayIndexOutOfBoundsException` (reported as "Error") or is misread instead of being rejected as "Expected 10 fields". |
| 7 | Hard-coded `Path.of("data", ...)` and raw `monthField.getText()` | The month is not validated (`2026/09`, `2026-13`, a future month and path-like input all go straight to the file system) and the data folder cannot be replaced by a temp folder in a test. |
| 8 | Every file in the folder is read and `lines.subList(1, ...)` assumes a header | A stray file such as `notes.txt`, or an empty CSV (`subList(1, 0)` throws), breaks the whole load. The branch/date file-name pattern is never checked. |
| 9 | No rounding or scale handling | Totals depend on the scale of the inputs, so figures can differ from the model's (for example `26715.4` versus `26715.40`). |

## Before and after

| | Fat controller | After refactoring |
|---|---|---|
| 2026-09, ALL, broken row of Task 3 present | `Revenue: 26715.40` (wrong, no warning) | `USD 26721.90 · 1 rejected` |
| Month `2026/09` | `"Error"` | `'2026/09' is not a month in the form yyyy-MM` |
| Month `2026-07` (no folder) | `"Error"` | `No sales data found in data\2026-07` |
| Lines in the handler `onLoad()` (signature to closing brace) | 20 | 3 |
| Statements in the body of `onLoad()` | 18 lines | 1 line |
| Lines in the whole file | 43 | 19 |
| Tests possible without JavaFX | 0 | all rules (`InputRulesTest`, `ReportControllerTest`, `SalesModelTest`, parser tests) |

How the fat figure is obtained: with the broken row
`PNH,2026-09-15,PNH-999999,SKU-1001,Jasmine Rice 5kg,Grocery,-1,6.50,0.00,KHQR`
present, the handler computes `unit_price * quantity - discount` = `6.50 * -1 - 0.00`
= **-6.50** for it and adds it, so `26721.90 - 6.50 = 26715.40`. The refactored path
rejects that row (`PNH-2026-09-15.csv:42 quantity must be positive: -1`), counts it as
rejected, and leaves the revenue at 26721.90.

## What the refactored class does

`onLoad()` now only forwards `monthField.getText()` to `ReportController`. Validation
(`InputRules`), file reading (`CsvMonthReader`), totals (`DefaultSalesAggregator`)
and error text (`ErrorDisplay`) each live in one place and are covered by tests that
run without a display.