package edu.itc.salesreport.ui;

import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ReportViewerApp extends Application {

    private ExecutorService workers;

    @Override
    public void start(Stage stage) {
        workers = Executors.newVirtualThreadPerTaskExecutor();
        Path dataDir = Path.of(getParameters().getNamed()
                .getOrDefault("data", "data"));
        var vm = new ReportViewModel(new CsvSummarySource(dataDir),
                new FxBackgroundJobs(workers),
                Clock.systemDefaultZone());
        stage.setScene(new Scene(new ReportView(vm).root(), 960, 600));
        stage.setTitle("Angkor Mart · Monthly report (MVVM)");
        stage.show();
    }

    @Override
    public void stop() {
        if (workers != null) {
            workers.shutdownNow();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}