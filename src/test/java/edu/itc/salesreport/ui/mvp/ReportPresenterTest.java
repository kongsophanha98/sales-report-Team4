package edu.itc.salesreport.ui.mvp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.itc.salesreport.TestData;
import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.SalesModel;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** No JavaFX: the screen is replaced by a recording fake. */
class ReportPresenterTest {
    static final class FakeScreen implements ReportScreen {
        List<RowItem> rows = List.of();
        Map<String, BigDecimal> categories = Map.of();
        String status = "";
        String error = "";
        @Override public void showRows(List<RowItem> r) { rows = r; }
        @Override public void showCategoryRevenue(Map<String, BigDecimal> c) { categories = c; }
        @Override public void showStatus(String t) { status = t; }
        @Override public void showError(String m) { error = m; }
    }

    @TempDir Path dataDir;
    private final FakeScreen screen = new FakeScreen();
    private ReportPresenter presenter;

    @BeforeEach
    void setUp() throws IOException {
        TestData.writeSample(dataDir);
        var model = new SalesModel(
                new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        var clock = Clock.fixed(Instant.parse("2026-09-27T03:00:00Z"),
                ZoneId.of("Asia/Phnom_Penh"));
        presenter = new ReportPresenter(model, screen, dataDir, clock);
    }

    @Test
    void statusAndRowsAreFormattedForOneBranch() {
        presenter.monthEntered("2026-09");
        presenter.branchChosen("PNH");
        assertEquals("PNH · 1 receipts · revenue USD 31.00 · basket USD 31.00",
                screen.status);
        assertEquals(new RowItem("2026-09-01", "PNH-000001",
                "Power Bank 10000mAh", "Electronics", "1", "18.00"),
                screen.rows.get(1));
    }

    @Test
    void moneyUsesGroupingAndTwoDecimals() {
        assertEquals("1,234.50", ReportPresenter.money(new BigDecimal("1234.5")));
    }
}
