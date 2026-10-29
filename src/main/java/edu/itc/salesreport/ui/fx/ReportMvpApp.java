package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.mvp.ReportPresenter;

import java.nio.file.Path;
import java.time.Clock;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ReportMvpApp extends Application {
    @Override
    public void start(Stage stage) {
        var model = new SalesModel(
                new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        var screen = new FxReportScreen();
        var presenter = new ReportPresenter(model, screen,
                Path.of("data"), Clock.systemDefaultZone());
        screen.attach(presenter);
        stage.setTitle("Angkor Mart · Monthly sales (MVP)");
        stage.setScene(new Scene(screen.root(), 1100, 650));
        stage.show();
        presenter.monthEntered("2026-09");
    }

    public static void main(String[] args) { launch(args); }
}
