# Platform mapping: ReportViewModel on Android Jetpack and WPF

| `ReportViewModel` member (JavaFX) | Android Jetpack | WPF (.NET) |
|---|---|---|
| `month` (`ObjectProperty<YearMonth>`, two-way) | `MutableStateFlow<YearMonth?>` in the `ViewModel`, view calls `setMonth(m)` on change (Compose: `state` + `onValueChange`) | Property with `INotifyPropertyChanged`, `{Binding Month, Mode=TwoWay}` |
| `branchSelected(b)` flags | `MutableStateFlow<Set<String>>` (or `Map<String,Boolean>`), `toggleBranch(b)` | `ObservableCollection<BranchItem>` where `BranchItem.IsSelected` raises `PropertyChanged` |
| `productFilter` | `MutableStateFlow<String>`; `visibleProducts` = `combine(products, filter)` | `ICollectionView` with `Filter` predicate, or a `string` property that re-filters |
| `summaries`, `products` (`ObservableList`) | `StateFlow<List<BranchSummary>>` (immutable list replaced as a whole) | `ObservableCollection<BranchSummary>` (`CollectionChanged`) |
| `visibleProducts` (`FilteredList`) | derived `StateFlow` via `combine`/`map` + `stateIn(viewModelScope)` | `CollectionViewSource` / `ListCollectionView` |
| `loading`, `progress`, `status`, `loadError` (read-only) | `StateFlow<Boolean/Float/String>` exposed with `asStateFlow()`, or one `UiState` data class | Properties with private setters raising `PropertyChanged` |
| `monthError`, `branchError`, `validationMessage`, `canGenerate`, `totalRevenueText` (bindings) | `combine(...).map {...}.stateIn(viewModelScope, ...)` | Computed properties raising `PropertyChanged` when inputs change (or `IDataErrorInfo` / `INotifyDataErrorInfo` for validation) |
| `generateCommand`, `cancelCommand` (`Command`) | Plain functions `generate()`, `cancel()`; enabled flag = `canGenerate` `StateFlow` | `ICommand` (`RelayCommand`/`AsyncRelayCommand`) with `CanExecute` + `CanExecuteChanged` |
| `BackgroundJobs` (port + `Task`) | `viewModelScope.launch(Dispatchers.IO)`, `Job.cancel()`, progress via `StateFlow`, results back on `Dispatchers.Main` | `async/await` with `Task.Run`, `CancellationTokenSource`, `IProgress<double>`; continuation on the UI `SynchronizationContext` |
| `Clock` injection | `java.time.Clock` or a `TimeProvider` interface | `TimeProvider` (.NET 8) |
| `SummarySource` | Repository interface | Service interface (DI) |

## What would not carry over

Jetpack has no two-way binding: state flows down as `StateFlow` and events flow up as function calls, so the `ComboBox`/`CheckBox` bidirectional bindings become explicit `onChange` handlers and the ViewModel must expose setters. The lifetime differs too: an Android `ViewModel` survives configuration changes such as rotation but not process death, so the loaded month and the filter would need `SavedStateHandle` or a repository, whereas our JavaFX ViewModel lives exactly as long as the application object that creates it. Our `ObservableList` and `FilteredList` have no direct equivalent, since Compose wants immutable lists that are replaced rather than mutated in place. Threading is another change: JavaFX listeners run on the FX thread by contract of `BackgroundJobs`, while coroutines need explicit dispatchers and structured cancellation, which actually makes cancel simpler than our `Task` wrapper. The WPF mapping is the closest (properties, collections and commands are almost one-to-one), but `INotifyPropertyChanged` must be written by hand or generated, and `async/await` replaces the whole `BackgroundJobs`/`JobListener` port.
