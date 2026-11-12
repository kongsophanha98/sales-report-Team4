package edu.itc.salesreport.ui;

/** Executes work away from the UI; all listener callbacks return on the UI thread. */
public interface BackgroundJobs {
    <T> JobHandle submit(Work<T> work, JobListener<T> listener);

    @FunctionalInterface interface Work<T> { T run(ProgressSink progress) throws Exception; }
    interface ProgressSink {
        void update(long done, long total);
        boolean isCancelled();
    }
    interface JobListener<T> {
        void progress(double fraction);
        void succeeded(T value);
        void failed(Throwable error);
        void cancelled();
    }
    @FunctionalInterface interface JobHandle { void cancel(); }
}
