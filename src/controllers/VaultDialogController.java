package controllers;

import FileManager.FileManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class VaultDialogController {

    @FXML private VBox dropZone;
    @FXML private TextField searchField;
    @FXML private ListView<String> fileListView;
    @FXML private Button exportButton;
    @FXML private Button deleteButton;
    @FXML private Button settingsButton;

    // Vault lives at {user.home}/MyVault — created automatically if absent
    private final FileManager fileManager = new FileManager();

    @FXML
    public void initialize() {
        System.out.println("Vault loaded at: " + fileManager.getVaultPath());
        refreshFileList();
    }

    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
            // Highlight the drop zone
            dropZone.setStyle(
                    "-fx-border-color: #4a90d9; -fx-border-style: dashed; " +
                            "-fx-border-width: 2; -fx-border-radius: 10; " +
                            "-fx-background-color: #eaf4ff; -fx-background-radius: 10;"
            );
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        List<File> files = event.getDragboard().getFiles();
        int imported = 0;

        for (File file : files) {
            if (file.isFile()) {
                Path result = fileManager.importFileToVault(file.toPath());
                if (result != null) {
                    System.out.println("Added to vault: " + result);
                    imported++;
                }
            }
        }

        if (imported > 0) {
            refreshFileList();
        }

        // Reset drop zone style
        dropZone.setStyle(
                "-fx-border-color: #cccccc; -fx-border-style: dashed; " +
                        "-fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;"
        );

        event.setDropCompleted(imported > 0);
        event.consume();
    }

    @FXML
    private void handleExport() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No file selected for export");
            return;
        }

        // Strip the lock emoji prefix added in the list
        String fileName = selected.replace("🔒 ", "");

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export File");
        chooser.setInitialFileName(fileName);
        File destination = chooser.showSaveDialog(fileListView.getScene().getWindow());

        if (destination != null) {
            boolean ok = fileManager.exportFile(fileName, destination.toPath());
            System.out.println(ok ? "Exported: " + destination.getAbsolutePath() : "Export failed");
        }
    }

    @FXML
    private void handleDelete() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No file selected for deletion");
            return;
        }

        String fileName = selected.replace("🔒 ", "");
        boolean ok = fileManager.deleteFile(fileName);

        if (ok) {
            refreshFileList();
            System.out.println("Deleted from vault: " + fileName);
        } else {
            System.out.println("Delete failed: " + fileName);
        }
    }

    @FXML
    private void handleSettings() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Vault Location");
        chooser.setInitialDirectory(fileManager.getBasePath().toFile());

        File newVaultDir = chooser.showDialog(settingsButton.getScene().getWindow());
        if (newVaultDir != null) {
            fileManager.setBasePath(Paths.get(newVaultDir.getAbsolutePath()));
            refreshFileList();
            System.out.println("Vault moved to: " + fileManager.getVaultPath());
        }
    }

    // ── helpers ──────────────────────────────────────────────────

    /** Reloads the ListView from whatever is currently in the vault. */
    private void refreshFileList() {
        fileListView.getItems().clear();
        for (Path p : fileManager.listFiles()) {
            fileListView.getItems().add("🔒 " + p.getFileName().toString());
        }
    }
}