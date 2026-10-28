# MVC vs MVP (fill the numbers with the measured values)

Measure: `wc -l src/main/java/edu/itc/salesreport/ui/fx/ReportView.java src/main/java/edu/itc/salesreport/ui/ReportController.java src/main/java/edu/itc/salesreport/ui/fx/FxReportScreen.java src/main/java/edu/itc/salesreport/ui/mvp/ReportPresenter.java`

| Criterion | MVC (ReportView + ReportController) | MVP (FxReportScreen + ReportPresenter) |
|---|---|---|
| Lines of code (view / controller-presenter) | 128/56 | 89 /79 |
| Tests that run without JavaFX | InputRulesTest, ReportControllerTest (rules + commands; display text NOT testable) | ReportPresenterTest (status text and rows testable) + same rules |
| Where money and status text are formatted | In ReportView (inside the toolkit class) | In ReportPresenter.money / modelChanged (toolkit-free) |
| Files changed to add a "Discount" column | 1 (ReportView) | 4 (RowItem, ReportPresenter, FxReportScreen, ReportPresenterTest) |
| Does the view hold a model reference? | Yes (reads model in modelChanged) | No |
| Effort for a second screen (web, Swing) | Re-implement formatting + model reading | Implement 4 setters of ReportScreen |
