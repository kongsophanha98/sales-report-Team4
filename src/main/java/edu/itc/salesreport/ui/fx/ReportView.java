package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.ui.ErrorDisplay;
import edu.itc.salesreport.ui.SalesEvent;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.SalesModelListener;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** MVC view: observes SalesModel and renders its current report. */
public final class ReportView implements SalesModelListener, ErrorDisplay {
    private final SalesModel model;
    private final BorderPane root = new BorderPane();
    private final TextField monthField = new TextField("2026-09");
    private final ComboBox<String> branchBox = new ComboBox<>();
    private final TableView<SaleTransaction> table = new TableView<>();
    private final BarChart<String, Number> chart =
            new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final Label status = new Label("No month loaded");
    private final Label error = new Label();

    public ReportView(SalesModel model) {
        this.model = model;

        branchBox.setItems(FXCollections.observableArrayList("ALL", "PNH", "REP", "BTB"));
        branchBox.setValue(SalesModel.ALL);

        table.getColumns().setAll(List.of(
                column("Date", t -> t.date().toString()),
                column("Receipt", SaleTransaction::receiptNo),
                column("Product", SaleTransaction::productName),
                column("Category", SaleTransaction::category),
                column("Qty", t -> Integer.toString(t.quantity())),
                column("Revenue", t -> money(t.revenue()))
        ));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        chart.setTitle("Revenue by category (USD)");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        error.setStyle("-fx-text-fill: firebrick;");

        var filters = new HBox(10,
                new Label("Month"), monthField,
                new Label("Branch"), branchBox);
        filters.setPadding(new Insets(10));
        root.setTop(filters);

        var content = new SplitPane(table, chart);
        content.setDividerPositions(0.62);
        root.setCenter(content);

        var footer = new VBox(5, status, error);
        footer.setPadding(new Insets(10));
        root.setBottom(footer);

        model.addListener(this);
    }

    public Parent root() { return root; }

    @Override
    public void modelChanged(SalesEvent event) {
        branchBox.setValue(model.branch());
        table.getItems().setAll(model.visibleTransactions());

        var series = new XYChart.Series<String, Number>();
        model.summary().revenueByCategory().forEach((category, revenue) ->
                series.getData().add(new XYChart.Data<>(category, revenue)));
        chart.getData().setAll(series);

        status.setText(switch (event) {
            case SalesEvent.MonthLoaded loaded -> statusText(loaded.rows());
            case SalesEvent.BranchSelected ignored ->
                    statusText(model.visibleTransactions().size());
        });
    }

    private String statusText(int rows) {
        var summary = model.summary();
        return "%s: %d rows, %d rejected · revenue USD %s · receipts %d"
                .formatted(model.month().map(Object::toString).orElse("No month loaded"),
                        rows, model.rejectedRows().size(), money(summary.revenue()), summary.receipts());
    }

    @Override public void showError(String message) { error.setText(message); }
    @Override public void clearError() { error.setText(""); }

    private static String money(BigDecimal amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }

    private static TableColumn<SaleTransaction, String> column(
            String title, Function<SaleTransaction, String> value) {
        var col = new TableColumn<SaleTransaction, String>(title);
        col.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return col;
    }
}
