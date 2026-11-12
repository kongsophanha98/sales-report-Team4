package edu.itc.salesreport.ui;

import java.nio.file.Path;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/** Compares load time and UI frame gaps for caller-thread and JavaFX Task loads. */
public final class LoadBenchmarkApp extends Application {

    private record Result(String mode, double loadMs, double maxGapMs) { }

    private final FrameGapMonitor monitor = new FrameGapMonitor();
    private final List<Result> results = new ArrayList<>();
    private final Deque<Runnable> plan = new ArrayDeque<>();
    private ExecutorService workers;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Map<String, String> args = getParameters().getNamed();
        Path dataDir = Path.of(args.getOrDefault("data", "data"));
        YearMonth month = YearMonth.parse(
                args.getOrDefault("month", "2026-08"));
        int warmup = positiveOrZero(args.getOrDefault("warmup", "2"),
                "warmup");
        int runs = positive(args.getOrDefault("runs", "5"), "runs");
        SummarySource source = new CsvSummarySource(dataDir);
        workers = Executors.newVirtualThreadPerTaskExecutor();

        for (String mode : List.of("caller-thread", "fx-task")) {
            for (int i = 0; i < warmup + runs; i++) {
                boolean measured = i >= warmup;
                plan.add(() -> runOnce(mode, measured, source, month));
            }
        }
        plan.add(this::printAndExit);

        stage.setTitle("Lab 03 · Background load benchmark");
        stage.setScene(new Scene(new StackPane(), 960, 600));
        stage.show();
        monitor.start();

        PauseTransition initialPulse = new PauseTransition(Duration.millis(250));
        initialPulse.setOnFinished(event -> plan.poll().run());
        initialPulse.play();
    }

    private static int positiveOrZero(String value, String name) {
        int parsed = Integer.parseInt(value);
        if (parsed < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
        return parsed;
    }

    private static int positive(String value, String name) {
        int parsed = Integer.parseInt(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return parsed;
    }

    private void runOnce(String mode, boolean measured, SummarySource source,
            YearMonth month) {
        BackgroundJobs jobs = mode.equals("caller-thread")
                ? new CallerThreadJobs() : new FxBackgroundJobs(workers);
        ReportViewModel vm = new ReportViewModel(
                source, jobs, Clock.systemDefaultZone());
        vm.monthProperty().set(month);
        stage.getScene().setRoot(new ReportView(vm).root());

        long startedAt = System.nanoTime();
        monitor.reset();
        vm.loadingProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<?
                    extends Boolean> observable, Boolean wasLoading,
                    Boolean isLoading) {
                if (isLoading) {
                    return;
                }
                observable.removeListener(this);
                String error = vm.loadErrorProperty().get();
                if (!error.isBlank()) {
                    System.err.println("Benchmark load failed (" + mode
                            + "): " + error);
                    shutdownAndExit();
                    return;
                }
                double loadMs = (System.nanoTime() - startedAt) / 1e6;
                PauseTransition settle =
                        new PauseTransition(Duration.millis(250));
                settle.setOnFinished(event -> {
                    if (measured) {
                        results.add(new Result(
                                mode, loadMs, monitor.maxGapMillis()));
                    }
                    plan.poll().run();
                });
                settle.play();
            }
        });
        vm.generateCommand().execute();
    }

    private void printAndExit() {
        System.out.println("mode           run  load ms  max frame gap ms");
        int runNumber = 0;
        String previousMode = "";
        for (Result result : results) {
            runNumber = result.mode().equals(previousMode) ? runNumber + 1 : 1;
            previousMode = result.mode();
            System.out.printf(Locale.ROOT, "%-13s %4d %8.0f %17.0f%n",
                    result.mode(), runNumber, result.loadMs(),
                    result.maxGapMs());
        }
        for (String mode : List.of("caller-thread", "fx-task")) {
            List<Result> modeResults = results.stream()
                    .filter(result -> result.mode().equals(mode))
                    .toList();
            System.out.printf(Locale.ROOT,
                    "%-13s mean load %6.0f ms, worst frame gap %6.0f ms%n",
                    mode,
                    modeResults.stream().mapToDouble(Result::loadMs)
                            .average().orElseThrow(),
                    modeResults.stream().mapToDouble(Result::maxGapMs)
                            .max().orElseThrow());
        }
        shutdownAndExit();
    }

    private void shutdownAndExit() {
        monitor.stop();
        if (workers != null) {
            workers.shutdownNow();
        }
        Platform.exit();
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
