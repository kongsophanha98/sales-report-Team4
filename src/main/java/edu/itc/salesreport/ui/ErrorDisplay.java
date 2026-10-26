package edu.itc.salesreport.ui;

/** Toolkit-free contract for displaying an error in a report view. */
public interface ErrorDisplay {
    void showError(String message);
    void clearError();
}
