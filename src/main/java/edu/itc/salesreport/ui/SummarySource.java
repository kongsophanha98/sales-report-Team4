package edu.itc.salesreport.ui;

import edu.itc.salesreport.model.BranchSummary;
import java.time.YearMonth;

/** Model-facing port used by the ViewModel to load one branch. */
@FunctionalInterface
public interface SummarySource {
    BranchSummary load(YearMonth month, String branch) throws Exception;
}
