package edu.itc.salesreport.ui;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.itc.salesreport.TestData;
import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;

class SalesModelTest {
    @TempDir
    Path tempDir;

    private SalesModel model;
    private List<SalesEvent> events;

    @BeforeEach
    void setUp() throws IOException {
        TestData.writeSample(tempDir);
        var reader = new CsvMonthReader(new CsvTransactionParser());
        var aggregator = new DefaultSalesAggregator();
        model = new SalesModel(reader, aggregator);
        events = new ArrayList<>();
        model.addListener(events::add);
    }

    @Test
    void loadFiresMonthLoadedAndComputesChainRevenue() throws IOException {
        model.load(tempDir, YearMonth.of(2026, 9));
        assertEquals(1, events.size());
        assertInstanceOf(SalesEvent.MonthLoaded.class, events.get(0));
        var loaded = (SalesEvent.MonthLoaded) events.get(0);
        assertEquals(3, loaded.rows());
                assertEquals(1, loaded.rejected());
        assertEquals(new BigDecimal("52.00"), model.summary().revenue());
    }

    @Test
    void selectingBranchTwiceFiresOnce() throws IOException {
        model.load(tempDir, YearMonth.of(2026, 9));
        model.selectBranch("PNH");
        model.selectBranch("PNH"); // unchanged, should not fire again

        long branchSelectedCount = events.stream()
                .filter(e -> e instanceof SalesEvent.BranchSelected)
                .count();
        assertEquals(1, branchSelectedCount);
        assertEquals(new BigDecimal("31.00"), model.summary().revenue());
        assertEquals(new BigDecimal("31.00"), model.summary().averageBasket());
    }

    @Test
    void loadingMissingMonthThrowsAndKeepsOldState() throws IOException {
        model.load(tempDir, YearMonth.of(2026, 9));
        int rowsBefore = model.visibleTransactions().size();

        assertThrows(NoSuchFileException.class, () -> model.load(tempDir, YearMonth.of(2026, 7)));
        // Fires nothing new for failed load, keeps old data
        assertEquals(1, events.size());
        assertEquals(rowsBefore, model.visibleTransactions().size());
    }

    @Test
    void removedListenerIsNotCalled() throws IOException {
        SalesModelListener dummy = e -> fail("Should not be called");
        model.addListener(dummy);
        model.removeListener(dummy);

        model.load(tempDir, YearMonth.of(2026, 9));
        assertEquals(1, events.size());
    }
}