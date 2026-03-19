package controllers;

import Encryption.KnightsTourEncryption;
import FileManager.FileManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import Encryption.KDF;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class VaultDialogController {

    @FXML private VBox dropZone;
    @FXML private ListView<String> fileListView;
    @FXML private Button exportButton;
    @FXML private Button deleteButton;
    @FXML private Button settingsButton;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordErrorLabel;

    private final FileManager fileManager = new FileManager();
    private final KnightsTourEncryption kte = new KnightsTourEncryption();

    @FXML
    public void initialize() {
        refreshFileList();
    }

    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
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
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (password.isEmpty()) {
            passwordErrorLabel.setText("Please enter a password.");
            event.setDropCompleted(false);
            event.consume();
            return;
        }

        if (!password.equals(confirm)) {
            passwordErrorLabel.setText("Passwords do not match.");
            event.setDropCompleted(false);
            event.consume();
            return;
        }

        passwordErrorLabel.setText("");

        List<File> files = event.getDragboard().getFiles();
        int imported = 0;

        for (File file : files) {
            if (file.isFile()) {
                try {
                    String originalExtension = file.getName().contains(".")
                            ? file.getName().substring(file.getName().lastIndexOf('.'))
                            : "";

                    // build clean enc filename — strip extension and add .enc
                    String encFileName = file.getName().replaceAll("\\.[^.]+$", "") + ".enc";

                    byte[] raw = Files.readAllBytes(file.toPath());

                    // generate key with fresh salt
                    KDF kdf = new KDF();
                    KDF.KDFResult kdfResult = kdf.computeKDF(password);
                    byte[] encrypted = kte.encryptBytes(raw, kdfResult.hash);

                    // write to temp file with correct name, not random
                    Path temp = Paths.get(System.getProperty("java.io.tmpdir")).resolve(encFileName);
                    Files.write(temp, encrypted);

                    // import into vault — returns actual path
                    Path result = fileManager.importFileToVault(temp);
                    Files.delete(temp);

                    if (result != null) {
                        fileManager.saveMD(encFileName, toHex(kdfResult.salt), originalExtension);
                        imported++;
                    }

                } catch (Exception e) {
                    passwordErrorLabel.setText("Encryption failed: " + e.getMessage());
                }
            }
        }

        if (imported > 0) {
            refreshFileList();
            passwordField.clear();
            confirmPasswordField.clear();
        }

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
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Export");
        dialog.setHeaderText("Enter password to decrypt file");
        dialog.setContentText("Password:");
        PasswordField pf = new PasswordField();
        dialog.getEditor().setVisible(false);
        dialog.getEditor().setManaged(false);
        dialog.getDialogPane().setContent(pf);

        dialog.showAndWait().ifPresent(unused -> {
            String password = pf.getText();
            if (password.isEmpty()) return;

            String fileName = selected.replace("🔒 ", "");

            String originalExt = fileManager.readOriginalExtension(fileName);

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export File");
            // restore original extension here, before dialog opens
            chooser.setInitialFileName(fileName.replace(".enc", originalExt));
            File destination = chooser.showSaveDialog(fileListView.getScene().getWindow());

            if (destination != null) {
                try {
                    Path vaultFile = Paths.get(fileManager.getVaultPath()).resolve(fileName);
                    byte[] encrypted = Files.readAllBytes(vaultFile);

                    // look up salt from md file and recompute key
                    byte[] key = deriveKeyForFile(fileName, password);
                    byte[] decrypted = kte.decryptBytes(encrypted, key);
                    Files.write(destination.toPath(), decrypted);

                } catch (IllegalStateException e) {
                    new Alert(Alert.AlertType.ERROR, "No salt found for this file. Was it encrypted with this vault?").show();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Decryption failed: " + e.getMessage()).show();
                }
            }
        });
    }

    @FXML
    private void handleDelete() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String fileName = selected.replace("🔒 ", "");
        if (fileManager.deleteFile(fileName)) refreshFileList();
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
        }
    }

    private void refreshFileList() {
        fileListView.getItems().clear();
        for (Path p : fileManager.listFiles()) {
            String name = p.getFileName().toString();
            // hide the metadata file from the UI
            if (name.equals("md")) continue;
            fileListView.getItems().add("🔒 " + name);
        }
    }

    private byte[] deriveKeyForFile(String fileName, String password) {
        String saltHex = fileManager.readSalt(fileName);
        if (saltHex == null) throw new IllegalStateException("No salt found for: " + fileName);
        byte[] salt = fromHex(saltHex);
        KDF kdf = new KDF();
        return kdf.recomputeKDF(password, salt);
    }

    private byte[] fromHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return bytes;
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}