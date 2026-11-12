package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportViewModelTest {

    static final Clock SEP_27 = Clock.fixed(
            Instant.parse("2026-09-27T03:00:00Z"),
            ZoneId.of("Asia/Phnom_Penh"));
    static final YearMonth AUG = YearMonth.of(2026, 8);

    ManualJobs jobs = new ManualJobs();
    List<String> loaded = new ArrayList<>();
    List<Double> progressSeen = new ArrayList<>();
    boolean failNext;
    ReportViewModel vm;

    @BeforeEach
    void setUp() {
        SummarySource source = (month, branch) -> {
            if (failNext) {
                throw new IOException(branch + "-2026-08-03.csv: unreadable");
            }
            loaded.add(branch);
            progressSeen.add(vm.progressProperty().get());
            return summary(branch, "200.00");
        };
        vm = new ReportViewModel(source, jobs, SEP_27);
    }

    static BranchSummary summary(String branch, String revenue) {
        var rice = new ProductTotal("SKU-1001", "Jasmine Rice 5kg",
                new BigDecimal(revenue), 5);
        return new BranchSummary(branch, new BigDecimal(revenue),
                BigDecimal.ZERO, 10, Map.of(), List.of(rice), Map.of());
    }

    private void loadAugust() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        jobs.runNext();
    }


    @Test
    void futureMonthIsRejected() {
        vm.monthProperty().set(YearMonth.of(2026, 10));
        assertEquals("Month 2026-10 is in the future.",
                vm.monthErrorProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    @Test
    void cannotGenerateBeforeAMonthIsChosen() {
        assertEquals("Choose a month.", vm.validationMessageProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    @Test
    void emptyBranchSelectionIsRejected() {
        vm.monthProperty().set(AUG);
        for (String b : ReportViewModel.BRANCHES) {
            vm.branchSelectedProperty(b).set(false);
        }
        assertEquals("Select at least one branch.",
                vm.validationMessageProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
        vm.monthProperty().set(null);
        assertEquals("Choose a month. Select at least one branch.",
                vm.validationMessageProperty().get());
        vm.monthProperty().set(YearMonth.of(2026, 9));
        vm.branchSelectedProperty("PNH").set(true);
        assertEquals("", vm.validationMessageProperty().get());
        assertTrue(vm.generateCommand().isExecutable());
    }


    @Test
    void generateStartsLoadingAndDisablesItself() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        assertTrue(vm.loadingProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
        assertTrue(vm.cancelCommand().isExecutable());
        assertEquals(1, jobs.pendingCount());
    }

    @Test
    void generateIsIgnoredWhileInvalid() {
        vm.monthProperty().set(YearMonth.of(2026, 10));
        vm.generateCommand().execute();
        assertFalse(vm.loadingProperty().get());
        assertEquals(0, jobs.pendingCount());
        assertFalse(vm.cancelCommand().isExecutable());
    }


    @Test
    void successfulLoadFillsSummariesAndTotal() {
        loadAugust();
        assertEquals(List.of("PNH", "REP", "BTB"), loaded);
        assertEquals(3, vm.summaries().size());
        assertEquals(new BigDecimal("600.00"), vm.totalRevenueProperty().get());
        assertEquals("USD 600.00", vm.totalRevenueTextProperty().get());
        assertEquals("Loaded 3 branch(es) for 2026-08.",
                vm.statusProperty().get());
        assertFalse(vm.loadingProperty().get());
        assertTrue(vm.generateCommand().isExecutable());
        assertEquals(1, vm.visibleProducts().size()); 
    }

    @Test
    void totalIsFormattedWithThousandsSeparator() {
        var big = new BranchSummary("PNH", new BigDecimal("12345.60"),
                BigDecimal.ZERO, 10, Map.of(), List.of(), Map.of());
        var vm2 = new ReportViewModel((m, b) -> big, jobs, SEP_27);
        vm2.monthProperty().set(AUG);
        vm2.branchSelectedProperty("REP").set(false);
        vm2.branchSelectedProperty("BTB").set(false);
        vm2.generateCommand().execute();
        jobs.runNext();
        assertEquals("USD 12,345.60", vm2.totalRevenueTextProperty().get());
    }

    @Test
    void progressFollowsTheBranches() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        assertEquals(0.0, vm.progressProperty().get());
        jobs.runNext();
        assertEquals(List.of(0.0, 1.0 / 3, 2.0 / 3), progressSeen);
        assertEquals(1.0, vm.progressProperty().get(), 1e-9);
    }

    @Test
    void failureKeepsPreviousDataAndShowsTheError() {
        loadAugust();
        failNext = true;
        vm.generateCommand().execute();
        assertEquals("", vm.loadErrorProperty().get()); // cleared at start
        jobs.runNext();
        assertEquals("PNH-2026-08-03.csv: unreadable",
                vm.loadErrorProperty().get());
        assertEquals("Load failed, previous data kept.",
                vm.statusProperty().get());
        assertEquals(3, vm.summaries().size());
        assertEquals("USD 600.00", vm.totalRevenueTextProperty().get());
        assertFalse(vm.loadingProperty().get());
        assertTrue(vm.generateCommand().isExecutable());
    }

    @Test
    void cancelReenablesGenerateAndKeepsOldData() {
        loadAugust();
        vm.generateCommand().execute();
        assertTrue(vm.cancelCommand().isExecutable());
        vm.cancelCommand().execute();
        assertFalse(vm.loadingProperty().get());
        assertEquals("Cancelled, previous data kept.",
                vm.statusProperty().get());
        assertEquals(3, vm.summaries().size());
        assertTrue(vm.generateCommand().isExecutable());
        assertFalse(vm.cancelCommand().isExecutable());
        assertEquals(0, jobs.pendingCount());
    }

    @Test
    void productFilterIsCaseInsensitiveOnNameAndSku() {
        loadAugust();
        vm.productFilterProperty().set("JASMINE");
        assertEquals(1, vm.visibleProducts().size());
        vm.productFilterProperty().set("sku-10");
        assertEquals(1, vm.visibleProducts().size());
        vm.productFilterProperty().set("tea");
        assertEquals(0, vm.visibleProducts().size());
        vm.productFilterProperty().set("");
        assertEquals(1, vm.visibleProducts().size());
    }

    @Test
    void workerUsesASnapshotOfTheInputs() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        vm.branchSelectedProperty("REP").set(false);
        vm.monthProperty().set(YearMonth.of(2026, 7));
        jobs.runNext();
        assertEquals(List.of("PNH", "REP", "BTB"), loaded);
        assertEquals("Loaded 3 branch(es) for 2026-08.",
                vm.statusProperty().get());
    }

  

    @Test
    void twoViewsReceiveTheSameTotal() {
        List<String> viewer = new ArrayList<>();
        List<String> dashboard = new ArrayList<>();
        vm.totalRevenueTextProperty()
                .addListener((o, a, b) -> viewer.add(b));
        vm.totalRevenueTextProperty()
                .addListener((o, a, b) -> dashboard.add(b));
        loadAugust();
        assertEquals(List.of("USD 600.00"), viewer);
        assertEquals(List.of("USD 600.00"), dashboard);
    }
}
