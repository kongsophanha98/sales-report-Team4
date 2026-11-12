package edu.itc.salesreport.ui;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;


public final class ReportViewModel {

    public static final List<String> BRANCHES = List.of("PNH", "REP", "BTB");

    private final SummarySource source;
    private final BackgroundJobs jobs;
    private final Clock clock;

    
    private final ObjectProperty<YearMonth> month =
            new SimpleObjectProperty<>(this, "month");
    private final Map<String, BooleanProperty> branchSelected =
            new LinkedHashMap<>();
    private final StringProperty productFilter =
            new SimpleStringProperty(this, "productFilter", "");

    
    private final ObservableList<YearMonth> monthChoices =
            FXCollections.observableArrayList();
    private final ObservableList<BranchSummary> summaries =
            FXCollections.observableArrayList();
    private final ObservableList<ProductTotal> products =
            FXCollections.observableArrayList();
    private final FilteredList<ProductTotal> visibleProducts =
            new FilteredList<>(products);
    private final ReadOnlyBooleanWrapper loading =
            new ReadOnlyBooleanWrapper(this, "loading", false);
    private final ReadOnlyDoubleWrapper progress =
            new ReadOnlyDoubleWrapper(this, "progress", 0);
    private final ReadOnlyStringWrapper status =
            new ReadOnlyStringWrapper(this, "status", "Choose a month.");
    private final ReadOnlyStringWrapper loadError =
            new ReadOnlyStringWrapper(this, "loadError", "");

    
    private final StringBinding monthError;
    private final StringBinding branchError;
    private final StringBinding validationMessage;
    private final BooleanBinding valid;
    private final BooleanBinding canGenerate;
    private final ObjectBinding<BigDecimal> totalRevenue;
    private final StringBinding totalRevenueText;

    private final Command generateCommand;
    private final Command cancelCommand;
    private BackgroundJobs.JobHandle running;

    public ReportViewModel(SummarySource source, BackgroundJobs jobs,
                           Clock clock) {
        this.source = Objects.requireNonNull(source);
        this.jobs = Objects.requireNonNull(jobs);
        this.clock = Objects.requireNonNull(clock);
        YearMonth now = YearMonth.now(clock);
        for (int i = 0; i < 12; i++) {
            monthChoices.add(now.minusMonths(i));
        }
        for (String b : BRANCHES) {
            branchSelected.put(b, new SimpleBooleanProperty(this, b, true));
        }

        
        Observable[] flags =
                branchSelected.values().toArray(Observable[]::new);
        monthError = Bindings.createStringBinding(
                () -> checkMonth(month.get()), month);
        branchError = Bindings.createStringBinding(
                () -> selectedBranches().isEmpty()
                        ? "Select at least one branch." : "",
                flags);
        validationMessage = Bindings.createStringBinding(
                () -> Stream.of(monthError.get(), branchError.get())
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining(" ")),
                monthError, branchError);
        valid = monthError.isEmpty().and(branchError.isEmpty());
        canGenerate = valid.and(loading.not());

        
        totalRevenue = Bindings.createObjectBinding(
                () -> summaries.stream()
                        .map(BranchSummary::revenue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                summaries);
        totalRevenueText = Bindings.createStringBinding(
                () -> formatUsd(totalRevenue.get()), totalRevenue);
        visibleProducts.predicateProperty().bind(
                Bindings.createObjectBinding(
                        () -> matcher(productFilter.get()), productFilter));

        generateCommand = new DelegateCommand(this::generate, canGenerate);
        cancelCommand = new DelegateCommand(this::cancel,
                loading.getReadOnlyProperty());
    }

    public List<String> selectedBranches() {
        return branchSelected.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .toList();
    }

    private String checkMonth(YearMonth m) {
        if (m == null) {
            return "Choose a month.";
        }
        if (m.isAfter(YearMonth.now(clock))) {
            return "Month " + m + " is in the future.";
        }
        return "";
    }

    private static String formatUsd(BigDecimal value) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.US);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return "USD " + f.format(value);
    }

    private static Predicate<ProductTotal> matcher(String text) {
        String needle = text == null ? "" : text.strip().toLowerCase();
        if (needle.isEmpty()) {
            return p -> true;
        }
        return p -> p.productName().toLowerCase().contains(needle)
                || p.sku().toLowerCase().contains(needle);
    }

    private void generate() {
        
        final YearMonth m = month.get();
        final List<String> branches = selectedBranches();

        loading.set(true);
        progress.set(0);
        loadError.set("");
        status.set("Loading " + m + "...");

        BackgroundJobs.JobHandle handle = jobs.submit(
                sink -> loadAll(m, branches, sink),
                new BackgroundJobs.JobListener<List<BranchSummary>>() {
                    @Override
                    public void progress(double fraction) {
                        ReportViewModel.this.progress.set(fraction);
                    }

                    @Override
                    public void succeeded(List<BranchSummary> result) {
                        summaries.setAll(result);
                        products.setAll(mergeTopProducts(result));
                        ReportViewModel.this.progress.set(1);
                        status.set("Loaded " + result.size()
                                + " branch(es) for " + m + ".");
                        finish();
                    }

                    @Override
                    public void failed(Throwable error) {
                        String msg = error.getMessage();
                        loadError.set(msg == null || msg.isBlank()
                                ? error.getClass().getSimpleName() : msg);
                        status.set("Load failed, previous data kept.");
                        ReportViewModel.this.progress.set(0);
                        finish();
                    }

                    @Override
                    public void cancelled() {
                        status.set("Cancelled, previous data kept.");
                        ReportViewModel.this.progress.set(0);
                        finish();
                    }
                });
        
        running = loading.get() ? handle : null;
    }

    private void finish() {
        running = null;
        loading.set(false);
    }

   
    private List<BranchSummary> loadAll(YearMonth m, List<String> branches,
            BackgroundJobs.ProgressSink sink) throws Exception {
        List<BranchSummary> result = new ArrayList<>();
        int n = branches.size();
        for (int i = 0; i < n; i++) {
            if (sink.isCancelled()) {
                throw new CancellationException();
            }
            result.add(source.load(m, branches.get(i)));
            sink.update(i + 1, n);
        }
        return result;
    }

    private static List<ProductTotal> mergeTopProducts(
            List<BranchSummary> list) {
        Map<String, ProductTotal> bySku = new LinkedHashMap<>();
        for (BranchSummary s : list) {
            for (ProductTotal p : s.topProducts()) {
                bySku.merge(p.sku(), p, (a, b) -> new ProductTotal(
                        a.sku(), a.productName(),
                        a.revenue().add(b.revenue())));
            }
        }
        return bySku.values().stream()
                .sorted(Comparator.comparing(ProductTotal::revenue)
                        .reversed())
                .limit(10)
                .toList();
    }

    private void cancel() {
        if (running != null) {
            running.cancel();
        }
    }

    
    public ObjectProperty<YearMonth> monthProperty() { return month; }
    public BooleanProperty branchSelectedProperty(String branch) {
        return Objects.requireNonNull(branchSelected.get(branch), branch);
    }
    public StringProperty productFilterProperty() { return productFilter; }
    public ObservableList<YearMonth> monthChoices() {
        return FXCollections.unmodifiableObservableList(monthChoices);
    }
    public ObservableList<BranchSummary> summaries() {
        return FXCollections.unmodifiableObservableList(summaries);
    }
    public ObservableList<ProductTotal> visibleProducts() {
        return visibleProducts;
    }
    public ReadOnlyBooleanProperty loadingProperty() {
        return loading.getReadOnlyProperty();
    }
    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty loadErrorProperty() {
        return loadError.getReadOnlyProperty();
    }
    public StringExpression monthErrorProperty() { return monthError; }
    public StringExpression branchErrorProperty() { return branchError; }
    public StringExpression validationMessageProperty() {
        return validationMessage;
    }
    public ObservableBooleanValue canGenerateProperty() { return canGenerate; }
    public ObservableObjectValue<BigDecimal> totalRevenueProperty() {
        return totalRevenue;
    }
    public StringExpression totalRevenueTextProperty() {
        return totalRevenueText;
    }
    public Command generateCommand() { return generateCommand; }
    public Command cancelCommand() { return cancelCommand; }
}