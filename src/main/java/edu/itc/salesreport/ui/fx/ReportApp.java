package edu.itc.salesreport.ui.fx;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.SalesModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class ReportApp extends Application {
    @Override
    public void start(Stage stage) {
        var model = new SalesModel(
                new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        var view = new ReportView(model);

        stage.setTitle("Angkor Mart · Monthly sales (MVC)");
        stage.setScene(new Scene(view.root(), 1100, 700));
        stage.show();

        try {
            model.load(Path.of("data"), YearMonth.of(2026, 9));
        } catch (IOException e) {
            view.showError("Could not load sales data: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

