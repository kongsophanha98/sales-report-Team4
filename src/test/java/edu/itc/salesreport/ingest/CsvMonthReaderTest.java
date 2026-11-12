package edu.itc.salesreport.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.itc.salesreport.model.SaleTransaction;
import java.nio.file.Path;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvMonthReaderTest {

    @TempDir
    Path dataDir;

    @Test
    void readBranchOnlyLoadsThatBranchAndMonthReadKeepsAllBranches()
            throws Exception {
        YearMonth month = YearMonth.of(2026, 8);
        SalesDataGenerator.generateMonth(dataDir, month, 2, 42);
        CsvMonthReader reader = new CsvMonthReader(new CsvTransactionParser());

        assertEquals(186, reader.read(dataDir, month).rows().size());
        MonthData branch = reader.readBranch(dataDir, month, "PNH");
        assertEquals(62, branch.rows().size());
        assertEquals(1, branch.rows().stream()
                .map(SaleTransaction::branch).distinct().count());
        assertThrows(IllegalArgumentException.class,
                () -> reader.readBranch(dataDir, month, "unknown"));
    }
}
