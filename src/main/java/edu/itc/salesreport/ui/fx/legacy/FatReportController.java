package edu.itc.salesreport.ui.fx.legacy;

import edu.itc.salesreport.ui.ReportController;
import javafx.scene.control.TextField;

/** After refactoring: a handler that only forwards raw text to the controller. */
public class FatReportController {
    private final TextField monthField;
    private final ReportController controller;

    public FatReportController(TextField monthField, ReportController controller) {
        this.monthField = monthField;
        this.controller = controller;
    }

    public void onLoad() {
        controller.monthEntered(monthField.getText());
    }
}