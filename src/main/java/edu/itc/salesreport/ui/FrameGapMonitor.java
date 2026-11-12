package edu.itc.salesreport.ui;

import javafx.animation.AnimationTimer;

/** Records the longest gap between pulses on the JavaFX Application Thread. */
public final class FrameGapMonitor extends AnimationTimer {

    private long last;
    private long maxGapNanos;

    @Override
    public void handle(long now) {
        if (last != 0) {
            maxGapNanos = Math.max(maxGapNanos, now - last);
        }
        last = now;
    }

    public void reset() {
        // Keep the prior pulse so the next interval includes any UI stall.
        maxGapNanos = 0;
    }

    public double maxGapMillis() {
        return maxGapNanos / 1_000_000.0;
    }
}
