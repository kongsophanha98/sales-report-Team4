package edu.itc.salesreport.ui;

import java.time.YearMonth;

/** A validated user intention, ready to be applied to the model. */
public sealed interface ReportCommand {
    record LoadMonth(YearMonth month) implements ReportCommand {}
    record SelectBranch(String branch) implements ReportCommand {}
}