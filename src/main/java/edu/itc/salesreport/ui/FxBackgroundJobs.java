package edu.itc.salesreport.ui;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import javafx.concurrent.Task;

/** Runs each background job as a JavaFX Task. */
public final class FxBackgroundJobs implements BackgroundJobs {

    private final Executor executor;

    public FxBackgroundJobs(Executor executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public <T> JobHandle submit(Work<T> work, JobListener<T> listener) {
        Objects.requireNonNull(work);
        Objects.requireNonNull(listener);

        FxTask<T> task = new FxTask<>(work);
        task.progressProperty().addListener((observable, oldValue, newValue) -> {
            double fraction = newValue.doubleValue();
            if (fraction >= 0) {
                listener.progress(fraction);
            }
        });
        task.setOnSucceeded(event -> listener.succeeded(task.getValue()));
        task.setOnFailed(event -> listener.failed(task.getException()));
        task.setOnCancelled(event -> listener.cancelled());

        try {
            executor.execute(task);
        } catch (RejectedExecutionException error) {
            listener.failed(error);
            return () -> { };
        }
        return () -> task.cancel(true);
    }

    private static final class FxTask<T> extends Task<T>
            implements ProgressSink {
        private final Work<T> work;

        FxTask(Work<T> work) {
            this.work = work;
        }

        @Override
        protected T call() throws Exception {
            return work.run(this);
        }

        @Override
        public void update(long done, long total) {
            updateProgress(done, total);
        }
    }
}
