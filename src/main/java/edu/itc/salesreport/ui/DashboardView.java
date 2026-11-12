package edu.itc.salesreport.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
public final class DashboardView {

    private final VBox root = new VBox(8);

    public DashboardView(ReportViewModel vm) {
        Label month = new Label();
        month.textProperty().bind(Bindings.createStringBinding(
                () -> vm.monthProperty().get() == null
                        ? "Month: -" : "Month: " + vm.monthProperty().get(),
                vm.monthProperty()));

        Label total = new Label();
        total.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        total.textProperty().bind(vm.totalRevenueTextProperty());

        Label branches = new Label();
        branches.textProperty().bind(
                Bindings.size(vm.summaries()).asString("%d branch(es) loaded"));

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(32, 32);
        spinner.visibleProperty().bind(vm.loadingProperty());
        spinner.managedProperty().bind(vm.loadingProperty());
        spinner.progressProperty().bind(Bindings
                .when(vm.progressProperty().greaterThan(0))
                .then(vm.progressProperty())
                .otherwise(-1.0));

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> vm.generateCommand().execute());
        refresh.disableProperty().bind(
                Bindings.not(vm.generateCommand().executableProperty()));

        root.getChildren().addAll(month, total, branches, spinner, refresh);
        root.setPadding(new Insets(12));
    }

    public Parent root() {
        return root;
    }
}
