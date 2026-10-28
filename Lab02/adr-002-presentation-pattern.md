# ADR-002: Presentation pattern for the report viewer

**Status:** Accepted

## Context
The viewer must be testable without a display and must later get a web page
(Lab-01 scenarios: testability — "model, rules and display text are tested
without a screen in under 1 s"; modifiability — "a new chart type is added in
under a day without touching parsing"). We built the same screen as MVC
(ReportView + ReportController) and as MVP (FxReportScreen + ReportPresenter).

## Decision
We keep **MVP (passive view)** for Lab-03. <!-- or MVC: change this to your real choice -->

## Consequences
+ All display text (status line, row formatting, money) is tested headless in
  ReportPresenterTest; in MVC the view formats text and cannot be unit-tested.
+ The screen has no SalesModel reference, so a Swing or web screen only
  implements ReportScreen.
- More classes/interfaces, and a new column touches RowItem, presenter, screen
  and test (see mvc-vs-mvp.md).
