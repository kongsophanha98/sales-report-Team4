# Task 1: ViewModel design

## Lab 2 controller inventory

The Lab 2 implementation uses `ReportController`, `InputRules`, `SalesModel`, and a JavaFX view. The repository’s data and model packages are present; the controller is toolkit-free. The implementation below puts screen state and commands in `ReportViewModel`, keeps sales aggregation/loading behind `SummarySource`, and delegates scheduling to `BackgroundJobs`.

| Existing code / interaction | Concern | New owner |
|---|---|---|
| `ReportView.monthField` | Month text entry | View (bound to ViewModel month) |
| `ReportController.monthEntered` | Parse and validate month | ViewModel |
| `InputRules.month` | Month validity rules | ViewModel |
| `ReportView.branchBox` | Branch selection control | View (bound to ViewModel branch flag) |
| `ReportController.branchChosen` | Branch selection handling | ViewModel |
| `ReportController.execute(LoadMonth)` | Start monthly load | ViewModel command |
| `SalesModel.load` / `CsvMonthReader` | Read monthly transactions | Model via `SummarySource` |
| `SalesAggregator.summarize` | Calculate report totals | Model via `SummarySource` |
| `SalesModel.visibleTransactions` | Select report rows | Model |
| `ReportView.modelChanged` | Render table and chart | View (bindings) |
| `ReportView.showError` / `clearError` | Display load and validation state | ViewModel state, rendered by View |
| `SalesModelListener` | Notify view of model changes | Replaced by observable ViewModel properties |

## ViewModel members

| Group | Member | View may write? | Purpose |
|---|---|---:|---|
| Input | `monthProperty()` | Yes | Requested report month |
| Input | `branchSelectedProperty(branch)` | Yes | Selected branches |
| Input | `productFilterProperty()` | Yes | Case-insensitive product filter |
| Output | `summaries()` | No | Loaded branch summaries, exposed unmodifiable |
| Output | `visibleProducts()` | No | Filtered product totals, exposed unmodifiable |
| Output | `loadingProperty()` | No | Active-load indicator |
| Output | `progressProperty()` | No | Load progress fraction |
| Output | `statusProperty()` | No | User-facing load status |
| Output | `loadErrorProperty()` | No | Last load error |
| Output | `monthChoices()` | No | Last twelve months from injected clock |
| Computed | `monthErrorProperty()` | No | Month validation message |
| Computed | `branchErrorProperty()` | No | Branch-selection validation message |
| Computed | `validationMessageProperty()` | No | Combined validation text |
| Computed | `canGenerateProperty()` | No | Generate command availability |
| Computed | `totalRevenueProperty()` | No | Sum of loaded branch revenues |
| Computed | `totalRevenueTextProperty()` | No | Formatted chain revenue |
| Command | `generateCommand()` | No | Start loading selected branch reports |
| Command | `cancelCommand()` | No | Cancel the current load |
| Port | `SummarySource` | No | Model boundary for one-branch summary loading |
| Port | `BackgroundJobs` | No | Worker execution and UI-thread callbacks |

## Class diagram

```mermaid
classDiagram
    class ReportView { +root() Parent }
    class ReportViewerApp { +start(Stage) }
    class ReportViewModel {
      +monthProperty() ObjectProperty~YearMonth~
      +branchSelectedProperty(String) BooleanProperty
      +productFilterProperty() StringProperty
      +monthChoices() ObservableList~YearMonth~
      +summaries() ObservableList~BranchSummary~
      +visibleProducts() ObservableList~ProductTotal~
      +loadingProperty() ReadOnlyBooleanProperty
      +progressProperty() ReadOnlyDoubleProperty
      +statusProperty() ReadOnlyStringProperty
      +loadErrorProperty() ReadOnlyStringProperty
      +monthErrorProperty() StringExpression
      +branchErrorProperty() StringExpression
      +validationMessageProperty() StringExpression
      +canGenerateProperty() ObservableBooleanValue
      +totalRevenueProperty() ObservableObjectValue~BigDecimal~
      +totalRevenueTextProperty() StringExpression
      +generateCommand() Command
      +cancelCommand() Command
    }
    class Command {
      <<interface>>
      +executableProperty() ObservableBooleanValue
      +execute()
    }
    class DelegateCommand
    class SummarySource {
      <<interface>>
      +load(YearMonth, String) BranchSummary
    }
    class BackgroundJobs {
      <<interface>>
      +submit(Work, JobListener) JobHandle
    }
    class SalesModel
    class CsvMonthReader
    class SalesAggregator
    class SaleTransaction
    class BranchSummary
    class ProductTotal
    class ManualJobs {
      <<test double>>
    }
    class FakeSummarySource {
      <<test double>>
    }
    ReportViewerApp --> ReportView : creates
    ReportViewerApp --> ReportViewModel : creates
    ReportView --> ReportViewModel : binds
    ReportViewModel --> SummarySource : loads through
    ReportViewModel --> BackgroundJobs : schedules through
    ReportViewModel --> Command : exposes
    Command <|.. DelegateCommand
    SummarySource --> CsvMonthReader : adapter uses
    SummarySource --> SalesAggregator : adapter uses
    SalesModel --> CsvMonthReader : reads
    SalesModel --> SalesAggregator : summarizes
    SalesModel --> SaleTransaction : contains
    SalesAggregator --> BranchSummary : creates
    BranchSummary --> ProductTotal : contains
    ManualJobs ..|> BackgroundJobs
    FakeSummarySource ..|> SummarySource
```

`ManualJobs` and `FakeSummarySource` are test-double roles shown to make the
dependency boundaries explicit; their implementations belong with the
toolkit-free ViewModel tests in Task 5. All methods in the member table return
observable read-only types or unmodifiable lists except the three declared
inputs and the two triggerable commands.

## Repository adaptation

This Team 4 checkout already includes Lab 2’s `model`, `ingest`, and `engine` packages and the supplied root Maven project. Its original MVC controller has one branch selector and loads a whole month before selecting one branch. Task 1’s `SummarySource` is the intended per-branch port; its adapter can use `CsvMonthReader` and `SalesAggregator` without changing the existing model records. The legacy UI classes remain available as the comparison baseline.

## Screen state machine

```mermaid
stateDiagram-v2
    [*] --> Incomplete
    Incomplete --> Ready: month ok and a branch ticked
    Incomplete --> Invalid: future month or no branch
    Invalid --> Ready: input fixed
    Ready --> Invalid: input changed
    Ready --> Loading: generate
    Loading --> Loaded: succeeded
    Loading --> Failed: failed
    Loading --> Ready: cancelled
    Loaded --> Loading: generate again
    Failed --> Loading: generate again
```

| State | month | validationMessage | canGenerate | loading | status | loadError |
|---|---|---|---|---|---|---|
| Incomplete | null | "Choose a month." | false | false | "Choose a month." | "" |
| Invalid | future, or no branch ticked | rule message | false | false | unchanged | unchanged |
| Ready | valid | "" | true | false | unchanged | "" |
| Loading | valid | "" | false | true | "Loading 2026-09..." | "" |
| Loaded | valid | "" | true | false | "Loaded 3 branch(es) for 2026-09." | "" |
| Failed | valid | "" | true | false | "Load failed, previous data kept." | file message |
