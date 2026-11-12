package edu.itc.salesreport.ui;

import javafx.beans.value.ObservableBooleanValue;

/** An action the view can trigger, with an observable availability flag. */
public interface Command {
    ObservableBooleanValue executableProperty();
    void execute();
    default boolean isExecutable() { return executableProperty().get(); }
}
