package FileManager;

import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.nio.charset.StandardCharsets;

/**
 * FileManager - A comprehensive CRUD library for file operations
 * Designed for cryptography projects with secure file handling.
 * All files are stored inside a "vault" directory on the user's system.
 */
public class FileManager {

    private Path basePath;

    // ============================================================
    // DEFAULT VAULT LOCATION
    // ============================================================

    /**
     * Default vault folder name, placed in the user's home directory.
     * e.g. /Users/yourname/MyVault  or  C:\Users\yourname\MyVault
     */
    private static final String DEFAULT_VAULT_NAME = "MyVault";

    // ============================================================
    // CONSTRUCTORS & INITIALIZATION
    // ============================================================

    /**
     * Creates a FileManager using the default vault location:
     * {user.home}/MyVault
     * The vault directory is created automatically if it doesn't exist.
     */
    public FileManager() {
        this(System.getProperty("user.home") + System.getProperty("file.separator") + DEFAULT_VAULT_NAME);
    }

    /**
     * Creates a FileManager with a custom vault/base directory path.
     * The directory is created automatically if it doesn't exist.
     *
     * @param basePath The root directory that acts as the vault
     */
    public FileManager(String basePath) {
        this.basePath = Paths.get(basePath);
        ensureDirectoryExists(this.basePath);
    }

    /**
     * Ensures a directory exists, creates it (and any parents) if it doesn't.
     */
    private void ensureDirectoryExists(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Vault created at: " + path.toAbsolutePath());
            } else {
                System.out.println("Vault found at: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to create vault directory: " + path);
            e.printStackTrace();
        }
    }

    // ============================================================
    // GETTERS & SETTERS
    // ============================================================

    public Path getBasePath() {
        return basePath;
    }

    public void setBasePath(Path basePath) {
        this.basePath = basePath;
        ensureDirectoryExists(basePath);
    }

    /**
     * Returns the absolute path of the vault directory as a String.
     */
    public String getVaultPath() {
        return basePath.toAbsolutePath().toString();
    }

    // ============================================================
    // CREATE OPERATIONS
    // ============================================================

    /**
     * Creates a new file with text content inside the vault.
     */
    public boolean createFile(String fileName, String content) {
        Path filePath = basePath.resolve(fileName);
        try {
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            System.out.println("File created in vault: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to create file: " + filePath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates a new file with binary content inside the vault.
     * Useful for storing encrypted byte data.
     */
    public boolean createFile(String fileName, byte[] content) {
        Path filePath = basePath.resolve(fileName);
        try {
            Files.write(filePath, content);
            System.out.println("Binary file created in vault: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to create binary file: " + filePath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Copies an external file (from anywhere on disk) into the vault.
     * This is the primary method used when a user drops a file onto the UI.
     *
     * @param sourcePath Full path of the file to import
     * @return The path of the file inside the vault, or null if failed
     */
    public Path importFileToVault(Path sourcePath) {
        if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
            System.err.println("Source file not found or is not a regular file: " + sourcePath);
            return null;
        }

        String fileName = sourcePath.getFileName().toString();
        Path destination = basePath.resolve(fileName);

        // If a file with the same name already exists, add a numeric suffix
        destination = resolveNameConflict(destination);

        try {
            Files.copy(sourcePath, destination, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Imported into vault: " + destination.toAbsolutePath());
            return destination;
        } catch (IOException e) {
            System.err.println("Failed to import file to vault: " + sourcePath);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * If the target path already exists, appends (1), (2), … until a free name is found.
     */
    private Path resolveNameConflict(Path target) {
        if (!Files.exists(target)) return target;

        String name = target.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        String ext  = dot >= 0 ? name.substring(dot)    : "";

        int counter = 1;
        Path candidate;
        do {
            candidate = target.getParent().resolve(base + " (" + counter++ + ")" + ext);
        } while (Files.exists(candidate));

        return candidate;
    }

    /**
     * Creates a new sub-directory inside the vault.
     */
    public boolean createDirectory(String dirName) {
        Path dirPath = basePath.resolve(dirName);
        try {
            Files.createDirectories(dirPath);
            System.out.println("Directory created in vault: " + dirPath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to create directory: " + dirPath);
            e.printStackTrace();
            return false;
        }
    }

    // ============================================================
    // READ OPERATIONS
    // ============================================================

    public String readFileAsText(String fileName) {
        Path filePath = basePath.resolve(fileName);
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to read file: " + filePath);
            e.printStackTrace();
            return null;
        }
    }

    public byte[] readFileAsBytes(String fileName) {
        Path filePath = basePath.resolve(fileName);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            System.err.println("Failed to read binary file: " + filePath);
            e.printStackTrace();
            return null;
        }
    }

    public List<String> readFileAsLines(String fileName) {
        Path filePath = basePath.resolve(fileName);
        try {
            return Files.readAllLines(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to read file lines: " + filePath);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Path> getItemsInFolder(Path folder) {
        List<Path> items = new ArrayList<>();
        if (Files.isDirectory(folder)) {
            try (Stream<Path> stream = Files.list(folder)) {
                stream.forEach(items::add);
            } catch (IOException e) {
                System.err.println("Failed to list folder: " + folder);
                e.printStackTrace();
            }
        }
        return items;
    }

    public List<Path> listAllItems() {
        return getItemsInFolder(basePath);
    }

    public List<Path> listFiles() {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(basePath)) {
            stream.filter(Files::isRegularFile).forEach(files::add);
        } catch (IOException e) {
            System.err.println("Failed to list files");
            e.printStackTrace();
        }
        return files;
    }

    public List<Path> listDirectories() {
        List<Path> dirs = new ArrayList<>();
        try (Stream<Path> stream = Files.list(basePath)) {
            stream.filter(Files::isDirectory).forEach(dirs::add);
        } catch (IOException e) {
            System.err.println("Failed to list directories");
            e.printStackTrace();
        }
        return dirs;
    }

    public boolean fileExists(String fileName) {
        Path filePath = basePath.resolve(fileName);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    public long getFileSize(String fileName) {
        Path filePath = basePath.resolve(fileName);
        try {
            return Files.size(filePath);
        } catch (IOException e) {
            System.err.println("Failed to get file size: " + filePath);
            e.printStackTrace();
            return -1;
        }
    }

    // ============================================================
    // UPDATE OPERATIONS
    // ============================================================

    public boolean updateFile(String fileName, String content) {
        Path filePath = basePath.resolve(fileName);
        if (!Files.exists(filePath)) {
            System.err.println("File does not exist: " + filePath);
            return false;
        }
        try {
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            System.out.println("File updated: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to update file: " + filePath);
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateFile(String fileName, byte[] content) {
        Path filePath = basePath.resolve(fileName);
        if (!Files.exists(filePath)) {
            System.err.println("File does not exist: " + filePath);
            return false;
        }
        try {
            Files.write(filePath, content);
            System.out.println("Binary file updated: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to update binary file: " + filePath);
            e.printStackTrace();
            return false;
        }
    }

    public boolean appendToFile(String fileName, String content) {
        Path filePath = basePath.resolve(fileName);
        try {
            Files.writeString(filePath, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("Content appended to: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to append to file: " + filePath);
            e.printStackTrace();
            return false;
        }
    }

    public boolean renameFile(String oldName, String newName) {
        Path oldPath = basePath.resolve(oldName);
        Path newPath = basePath.resolve(newName);
        try {
            Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
            System.out.println("Renamed: " + oldName + " -> " + newName);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to rename: " + oldName);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Exports a file from the vault to an external destination path.
     *
     * @param fileName        Name of the file inside the vault
     * @param destinationPath Full path where the file should be exported
     * @return true if successful, false otherwise
     */
    public boolean exportFile(String fileName, Path destinationPath) {
        Path source = basePath.resolve(fileName);
        try {
            Files.copy(source, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Exported: " + fileName + " -> " + destinationPath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to export file: " + fileName);
            e.printStackTrace();
            return false;
        }
    }

    public boolean copyFile(String fileName, String destinationName) {
        Path source = basePath.resolve(fileName);
        Path destination = basePath.resolve(destinationName);
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied: " + fileName + " -> " + destinationName);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to copy file: " + fileName);
            e.printStackTrace();
            return false;
        }
    }

    // ============================================================
    // DELETE OPERATIONS
    // ============================================================

    public boolean deleteFile(String fileName) {
        Path filePath = basePath.resolve(fileName);
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                System.out.println("File deleted from vault: " + filePath);
            } else {
                System.out.println("File not found in vault: " + filePath);
            }
            return deleted;
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + filePath);
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteDirectory(String dirName) {
        Path dirPath = basePath.resolve(dirName);
        try {
            boolean deleted = Files.deleteIfExists(dirPath);
            if (deleted) System.out.println("Directory deleted: " + dirPath);
            else         System.out.println("Directory not found: " + dirPath);
            return deleted;
        } catch (IOException e) {
            System.err.println("Failed to delete directory (may not be empty): " + dirPath);
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteDirectoryRecursively(String dirName) {
        Path dirPath = basePath.resolve(dirName);
        try {
            if (Files.exists(dirPath)) {
                deleteRecursive(dirPath);
                System.out.println("Directory recursively deleted: " + dirPath);
                return true;
            } else {
                System.out.println("Directory not found: " + dirPath);
                return false;
            }
        } catch (IOException e) {
            System.err.println("Failed to recursively delete directory: " + dirPath);
            e.printStackTrace();
            return false;
        }
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                entries.forEach(entry -> {
                    try { deleteRecursive(entry); }
                    catch (IOException e) { e.printStackTrace(); }
                });
            }
        }
        Files.delete(path);
    }

    // ============================================================
    // UTILITY / CRYPTOGRAPHY HELPERS
    // ============================================================

    /**
     * Securely overwrites a file with random data before deletion
     * to help prevent data recovery.
     *
     * @param fileName File to securely delete
     * @param passes   Number of overwrite passes (recommended: 3–7)
     */
    public boolean secureDelete(String fileName, int passes) {
        Path filePath = basePath.resolve(fileName);
        try {
            long fileSize = Files.size(filePath);
            byte[] randomData = new byte[(int) fileSize];
            java.security.SecureRandom rng = new java.security.SecureRandom();

            for (int i = 0; i < passes; i++) {
                rng.nextBytes(randomData);
                Files.write(filePath, randomData);
            }

            Files.delete(filePath);
            System.out.println("File securely deleted: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to securely delete file: " + filePath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates a .backup copy of a file inside the vault.
     */
    public Path createBackup(String fileName) {
        Path original = basePath.resolve(fileName);
        Path backup   = basePath.resolve(fileName + ".backup");
        try {
            Files.copy(original, backup, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backup created: " + backup);
            return backup;
        } catch (IOException e) {
            System.err.println("Failed to create backup: " + fileName);
            e.printStackTrace();
            return null;
        }
    }

    public Path getAbsolutePath(String fileName) {
        return basePath.resolve(fileName).toAbsolutePath();
    }
}