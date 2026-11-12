package edu.itc.salesreport.ui;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.engine.SalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ingest.MonthData;
import edu.itc.salesreport.model.BranchSummary;
import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Objects;

/** Adapts the CSV reader and sales aggregator to the ViewModel's load port. */
public final class CsvSummarySource implements SummarySource {

    private final Path dataDir;
    private final CsvMonthReader reader =
            new CsvMonthReader(new CsvTransactionParser());
    private final SalesAggregator aggregator = new DefaultSalesAggregator();

    public CsvSummarySource(Path dataDir) {
        this.dataDir = Objects.requireNonNull(dataDir);
    }

    @Override
    public BranchSummary load(YearMonth month, String branch)
            throws IOException {
        MonthData data = reader.readBranch(dataDir, month, branch);
        return aggregator.summarize(branch, data.rows());
    }
}
