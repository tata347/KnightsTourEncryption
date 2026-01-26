package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;

public class VaultDialogController {

    @FXML
    private VBox dropZone;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> fileListView;

    @FXML
    private Button exportButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button settingsButton;

    @FXML
    public void initialize() {
        System.out.println("Vault loaded!");
    }

    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        List<File> files = event.getDragboard().getFiles();

        for (File file : files) {
            System.out.println("File dropped: " + file.getAbsolutePath());
            fileListView.getItems().add("🔒 " + file.getName());
        }

        event.setDropCompleted(true);
        event.consume();
    }

    @FXML
    private void handleExport() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("Exporting: " + selected);
        } else {
            System.out.println("No file selected");
        }
    }

    @FXML
    private void handleDelete() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            fileListView.getItems().remove(selected);
            System.out.println("Deleted: " + selected);
        } else {
            System.out.println("No file selected");
        }
    }

    @FXML
    private void handleSettings() {
        System.out.println("Settings clicked");
    }
}