package controllers;

import Encryption.KDF;
import Encryption.KnightsTourEncryption;
import FileManager.FileManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Controller for the Vault dialog window.
 */
public class VaultDialogController {


    @FXML private VBox dropZone;
    @FXML private ListView<String> fileListView;
    @FXML private Button exportButton;
    @FXML private Button deleteButton;
    @FXML private Button settingsButton;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordErrorLabel;


    /**
     * Handles filesystem operations for the vault.
     */
    private final FileManager fileManager = new FileManager();

    /**
     * Encryption engine used for encrypt/decrypt.
     */
    private final KnightsTourEncryption kte = new KnightsTourEncryption();

    /**
     * Runs automatically after FXML loads.
     * Refreshes visible vault file list.
     */
    @FXML
    public void initialize() {
        refreshFileList();
    }


    /**
     * Triggered while user drags files over the drop zone.
     *
     * If files are detected:
     * - allows copy mode
     * - changes UI styling for feedback
     */
    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);

            // Highlight drop area
            dropZone.setStyle(
                    "-fx-border-color: #4a90d9; -fx-border-style: dashed; " +
                            "-fx-border-width: 2; -fx-border-radius: 10; " +
                            "-fx-background-color: #eaf4ff; -fx-background-radius: 10;"
            );
        }

        event.consume();
    }

    /**
     * Triggered when files are dropped into vault area.
     *
     * Flow:
     * 1. Validate password fields
     * 2. Read dropped files
     * 3. Generate salted key using KDF
     * 4. Encrypt bytes
     * 5. Store encrypted file in vault
     * 6. Save metadata (salt + original extension)
     */
    @FXML
    private void handleDragDropped(DragEvent event) {

        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();


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

            if (!file.isFile()) continue;

            try {
                // Keep original extension for export later
                String originalExtension = file.getName().contains(".")
                        ? file.getName().substring(file.getName().lastIndexOf('.'))
                        : "";

                // Example:
                // photo.jpg -> photo.enc
                String encFileName =
                        file.getName().replaceAll("\\.[^.]+$", "") + ".enc";

                // Read raw bytes
                byte[] raw = Files.readAllBytes(file.toPath());

                // Derive encryption key using password + random salt
                KDF kdf = new KDF();
                KDF.KDFResult kdfResult = kdf.computeKDF(password);

                // Encrypt bytes
                byte[] encrypted =
                        kte.encryptBytes(raw, kdfResult.hash);

                // Temporary file before moving into vault
                Path temp =
                        Paths.get(System.getProperty("java.io.tmpdir"))
                                .resolve(encFileName);

                Files.write(temp, encrypted);

                // Import to actual vault folder
                Path result = fileManager.importFileToVault(temp);

                // Remove temp file
                Files.delete(temp);

                // Save metadata if import succeeded
                if (result != null) {
                    fileManager.saveMD(
                            encFileName,
                            toHex(kdfResult.salt),
                            originalExtension
                    );
                    imported++;
                }

            } catch (Exception e) {
                passwordErrorLabel.setText(
                        "Encryption failed: " + e.getMessage()
                );
            }
        }

        // Success cleanup
        if (imported > 0) {
            refreshFileList();
            passwordField.clear();
            confirmPasswordField.clear();
        }

        // Reset UI style
        dropZone.setStyle(
                "-fx-border-color: #cccccc; -fx-border-style: dashed; " +
                        "-fx-border-width: 2; -fx-border-radius: 10; " +
                        "-fx-background-radius: 10;"
        );

        event.setDropCompleted(imported > 0);
        event.consume();
    }


    /**
     * Exports selected vault file.
     *
     * Flow:
     * 1. Ask for password
     * 2. Load file metadata salt
     * 3. Recompute same key
     * 4. Decrypt bytes
     * 5. Save user-selected output file
     */
    @FXML
    private void handleExport() {

        String selected =
                fileListView.getSelectionModel().getSelectedItem();

        if (selected == null) return;

        // Password popup
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Export");
        dialog.setHeaderText("Enter password to decrypt file");
        dialog.setContentText("Password:");

        PasswordField pf = new PasswordField();

        // Replace default text box with password field
        dialog.getEditor().setVisible(false);
        dialog.getEditor().setManaged(false);
        dialog.getDialogPane().setContent(pf);

        dialog.showAndWait().ifPresent(unused -> {

            String password = pf.getText();

            if (password.isEmpty()) return;

            String fileName = selected.replace("🔒 ", "");

            String originalExt =
                    fileManager.readOriginalExtension(fileName);

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export File");

            // Restore original extension
            chooser.setInitialFileName(
                    fileName.replace(".enc", originalExt)
            );

            File destination =
                    chooser.showSaveDialog(
                            fileListView.getScene().getWindow()
                    );

            if (destination == null) return;

            try {
                // Read encrypted vault file
                Path vaultFile =
                        Paths.get(fileManager.getVaultPath())
                                .resolve(fileName);

                byte[] encrypted =
                        Files.readAllBytes(vaultFile);

                // Recreate same key from password + stored salt
                byte[] key =
                        deriveKeyForFile(fileName, password);

                // Decrypt
                byte[] decrypted =
                        kte.decryptBytes(encrypted, key);

                // Save output
                Files.write(destination.toPath(), decrypted);

            } catch (IllegalStateException e) {

                new Alert(
                        Alert.AlertType.ERROR,
                        "No salt found for this file."
                ).show();

            } catch (Exception e) {

                new Alert(
                        Alert.AlertType.ERROR,
                        "Decryption failed: " + e.getMessage()
                ).show();
            }
        });
    }


    /**
     * Deletes selected file from vault.
     */
    @FXML
    private void handleDelete() {

        String selected =
                fileListView.getSelectionModel().getSelectedItem();

        if (selected == null) return;

        String fileName = selected.replace("🔒 ", "");

        if (fileManager.deleteFile(fileName)) {
            refreshFileList();
        }
    }



    /**
     * Lets user choose a different vault folder.
     */
    @FXML
    private void handleSettings() {

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Vault Location");

        chooser.setInitialDirectory(
                fileManager.getBasePath().toFile()
        );

        File newVaultDir =
                chooser.showDialog(
                        settingsButton.getScene().getWindow()
                );

        if (newVaultDir != null) {
            fileManager.setBasePath(
                    Paths.get(newVaultDir.getAbsolutePath())
            );

            refreshFileList();
        }
    }


    /**
     * Reloads vault file names into UI list.
     * Metadata file "md" is hidden from user.
     */
    private void refreshFileList() {

        fileListView.getItems().clear();

        for (Path p : fileManager.listFiles()) {

            String name = p.getFileName().toString();

            if (name.equals("md")) continue;

            fileListView.getItems().add("🔒 " + name);
        }
    }

    /**
     * Reads stored salt for file
     * and recreates original encryption key.
     */
    private byte[] deriveKeyForFile(
            String fileName,
            String password
    ) {
        String saltHex = fileManager.readSalt(fileName);

        if (saltHex == null) {
            throw new IllegalStateException(
                    "No salt found for: " + fileName
            );
        }

        byte[] salt = fromHex(saltHex);

        KDF kdf = new KDF();

        return kdf.recomputeKDF(password, salt);
    }

    /**
     * Converts hex string to byte[].
     */
    private byte[] fromHex(String hex) {

        byte[] bytes = new byte[hex.length() / 2];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)
                    Integer.parseInt(
                            hex.substring(i * 2, i * 2 + 2),
                            16
                    );
        }

        return bytes;
    }

    /**
     * Converts byte[] to lowercase hex string.
     */
    private String toHex(byte[] bytes) {

        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }
}