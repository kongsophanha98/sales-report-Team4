# Lab 03 Task 4: Background loading measurements

## Environment and method

| Item | Value |
|---|---|
| CPU | 13th Gen Intel Core i5-13420H |
| Memory | 15.6 GB |
| OS | Windows 11 Home Single Language, build 26200 |
| JDK | Eclipse Temurin 25.0.5 |
| JavaFX | 25.0.4 |
| Input | August 2026; 93 CSV files; 5,000 rows per branch per day (465,000 rows) |
| Runs | 2 warm-ups, then 5 measured runs per mode |

Generated the data with:

```powershell
java -cp target\classes edu.itc.salesreport.ingest.SalesDataGenerator 2026-08 5000 42 C:\Users\U-ser\AppData\Local\Temp\sales-report-lab03-data
```

Ran the benchmark with:

```powershell
mvn -q javafx:run "-Djavafx.mainClass=edu.itc.salesreport.ui.LoadBenchmarkApp" "-Djavafx.args=--data=C:\Users\U-ser\AppData\Local\Temp\sales-report-lab03-data --month=2026-08 --warmup=2 --runs=5"
```

The source loads each selected branch's CSV files only. The caller-thread and
JavaFX `Task` modes use the same source, inputs, and summary calculation.

## Measured runs

Times are milliseconds; each mode had two unrecorded warm-up runs before these
five measurements.

| Mode | Run | Load time | Worst frame gap |
|---|---:|---:|---:|
| Caller thread | 1 | 948 | 1,212 |
| Caller thread | 2 | 981 | 1,123 |
| Caller thread | 3 | 872 | 992 |
| Caller thread | 4 | 871 | 961 |
| Caller thread | 5 | 855 | 978 |
| JavaFX Task | 1 | 890 | 44 |
| JavaFX Task | 2 | 923 | 41 |
| JavaFX Task | 3 | 788 | 45 |
| JavaFX Task | 4 | 768 | 36 |
| JavaFX Task | 5 | 794 | 32 |

| Mode | Mean load time | Worst frame gap |
|---|---:|---:|
| Caller thread | 906 ms | 1,212 ms |
| JavaFX Task | 833 ms | 45 ms |

## Manual interaction at 20,000 rows per branch per day

Not yet recorded. This requires an interactive check in both modes. Generate
the larger data set in a separate directory, run the viewer while loading
August 2026, then try typing in the product filter, moving the window, and
pressing **Cancel**. Record 3–5 sentences here describing whether those actions
remain responsive in each mode and whether the previous report stays visible
after cancellation. Do not treat the automated frame-gap benchmark above as a
substitute for this manual check.
