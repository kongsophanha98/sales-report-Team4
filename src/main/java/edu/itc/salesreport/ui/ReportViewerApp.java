package edu.itc.salesreport.ui;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.engine.SalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ingest.MonthData;
import java.nio.file.Path;
import java.time.Clock;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ReportViewerApp extends Application {

    @Override
    public void start(Stage stage) {
        Path dataDir = Path.of(getParameters().getNamed()
                .getOrDefault("data", "data"));
        CsvMonthReader reader = new CsvMonthReader(new CsvTransactionParser());
        SalesAggregator aggregator = new DefaultSalesAggregator();

        SummarySource source = (month, branch) -> {
            MonthData data = reader.read(dataDir, month);
            var rows = data.rows().stream()
                    .filter(r -> r.branch().equalsIgnoreCase(branch))
                    .toList();
            return aggregator.summarize(branch, rows);
        };
        var vm = new ReportViewModel(source, new CallerThreadJobs(),
                Clock.systemDefaultZone());
        stage.setScene(new Scene(new ReportView(vm).root(), 960, 600));
        stage.setTitle("Angkor Mart · Monthly report (MVVM)");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}