# Lab-03 · MVVM refactoring of the Angkor Mart report viewer

Team: `<team name>` · Members: `<member A>`, `<member B>`
Pull request: `<link to "Lab-03 – <team name>">`

## (a) Files of this lab

| Path | Purpose | Author |
|---|---|---|
| `src/main/java/edu/itc/salesreport/ui/Command.java`, `DelegateCommand.java` | Command abstraction with observable "can run" flag | `<A>` |
| `.../ui/SummarySource.java`, `BackgroundJobs.java` | Ports: model and threading seen by the ViewModel | `<A>` |
| `.../ui/ReportViewModel.java` | State, rules, commands (javafx.base only) | `<A>` |
| `.../ui/ReportView.java`, `ReportViewerApp.java` | Bound view and composition root | `<B>` |
| `.../ui/CallerThreadJobs.java`, `FxBackgroundJobs.java` | Same-thread baseline and `Task`-based jobs | `<B>` |
| `.../ui/FrameGapMonitor.java`, `LoadBenchmarkApp.java` | Responsiveness measurement | `<B>` |
| `src/test/.../ui/ManualJobs.java` | Test double: jobs run when the test says | `<A>` |
| `src/test/.../ui/ReportViewModelTest.java` | 13 toolkit-free ViewModel tests (incl. two-view test) | `<A>` |
| `src/test/.../ui/ViewModelImportsTest.java` | Guard: no `javafx.scene/stage/application/concurrent` in the ViewModel | `<A>` |
| `.../ui/DashboardView.java`, `DashboardWindow.java` | Challenge: second view on the same ViewModel | `<B>` |
| `lab03/viewmodel-design.md` | Controller inventory, class and state diagrams | `<A>` |
| `lab03/measurements.md` | 10 measured runs, hardware, observations | `<B>` |
| `lab03/mvc-vs-mvvm.md` | Metrics and conclusion | `<A+B>` |
| `lab03/platform-mapping.md` | Challenge: Jetpack / WPF mapping | `<B>` |
| `lab03/screenshots/viewer.png`, `dashboard.png` | Evidence | `<B>` |

## (b) Inputs reused from Lab-02

| Input | Source |
|---|---|
| Maven project, model records, CSV loader | tag `lab-02-final` (or: fallback starter A) |
| Lab-02 MVC viewer (baseline for Task 5) | tag `lab-02-final` (or: fallback starter B) |
| Sample data | generated with `SalesDataGenerator 2026-08 5000 42 data` |

## (c) Change log

| Date | What changed | Who | Model package changed? |
|---|---|---|---|
| `<2026-11-xx>` | MVC controller replaced by `ReportViewModel` + bound `ReportView` | `<A+B>` | No (`git diff --stat lab-02-final -- src/main/java/edu/itc/salesreport/model` is empty) |
| `<2026-11-xx>` | Background loading with `Task`, progress, cancel | `<B>` | No |
| `<2026-11-xx>` | 13 ViewModel tests, import guard, coverage `<n>` % | `<A>` | No |
| `<2026-11-xx>` | Challenge: `DashboardView` on the same ViewModel | `<B>` | No |

## Challenge: ViewModel reuse

Tag the Task 5 commit first: `git tag lab-03-task5`. After adding the dashboard:

    git diff lab-03-task5 -- src/main/java/edu/itc/salesreport/ui/ReportViewModel.java   # must print nothing

Only `ReportViewerApp.start` changes (one line after `stage.show()`):

    DashboardWindow.open(vm);
