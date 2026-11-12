package edu.itc.salesreport.ui;

import java.util.Objects;
import javafx.beans.value.ObservableBooleanValue;

/** Command that runs its action only while its availability condition is true. */
public final class DelegateCommand implements Command {
    private final Runnable action;
    private final ObservableBooleanValue executable;

    public DelegateCommand(Runnable action, ObservableBooleanValue executable) {
        this.action = Objects.requireNonNull(action);
        this.executable = Objects.requireNonNull(executable);
    }

    @Override public ObservableBooleanValue executableProperty() { return executable; }
    @Override public void execute() { if (executable.get()) action.run(); }
}
