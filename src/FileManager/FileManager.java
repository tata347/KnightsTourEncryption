package FileManager;

import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.nio.charset.StandardCharsets;

/**
 * Handles all file operations for the vault - creating, reading, deleting,
 * and importing files. Each FileManager instance manages one vault directory.
 */
public class FileManager {

    private Path basePath;
    private static final String DEFAULT_VAULT_NAME = "MyVault";

    /** Uses the default vault at {user.home}/MyVault. */
    public FileManager() {
        this(System.getProperty("user.home") + "\\" + DEFAULT_VAULT_NAME);
    }

    /** Uses a custom vault path. Creates the directory if it doesn't exist. */
    public FileManager(String basePath) {
        this.basePath = Paths.get(basePath);
        ensureDirectoryExists(this.basePath);
    }

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

    // metadata (per-file salt + original extension)

    /** Saves or updates the salt and original extension for a file. */
    public boolean saveMD(String fileName, String saltHex, String originalExtension) {
        Path mdPath = basePath.resolve("md");
        try {
            List<String> lines = new ArrayList<>();
            if (Files.exists(mdPath)) {
                lines = new ArrayList<>(Files.readAllLines(mdPath, StandardCharsets.UTF_8));
            }

            String entry = fileName + ":" + saltHex + ":" + originalExtension;

            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith(fileName + ":")) {
                    lines.set(i, entry);
                    found = true;
                    break;
                }
            }
            if (!found) lines.add(entry);

            Files.write(mdPath, lines, StandardCharsets.UTF_8);
            return true;

        } catch (IOException e) {
            System.err.println("Failed to save salt for: " + fileName);
            e.printStackTrace();
            return false;
        }
    }

    /** Returns the salt for the given file, or null if not found. */
    public String readSalt(String fileName) {
        Path mdPath = basePath.resolve("md");
        if (!Files.exists(mdPath)) return null;
        try {
            List<String> lines = Files.readAllLines(mdPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.startsWith(fileName + ":")) {
                    return line.split(":", 3)[1];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Returns the original extension for the given file, or empty string if not found. */
    public String readOriginalExtension(String fileName) {
        Path mdPath = basePath.resolve("md");
        if (!Files.exists(mdPath)) return "";
        try {
            List<String> lines = Files.readAllLines(mdPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.startsWith(fileName + ":")) {
                    String[] parts = line.split(":", 3);
                    return parts.length == 3 ? parts[2] : "";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    // getters & setters

    public Path getBasePath() {
        return basePath;
    }

    public void setBasePath(Path basePath) {
        this.basePath = basePath;
        ensureDirectoryExists(basePath);
    }

    public String getVaultPath() {
        return basePath.toAbsolutePath().toString();
    }

    // create

    /** Writes text content to a new file in the vault. */
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

    /** Writes raw bytes to a new file - used for encrypted data. */
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
     * Copies a file from anywhere on disk into the vault.
     * If a file with the same name exists, adds (1), (2), etc.
     * Returns the path inside the vault, or null on failure.
     */
    public Path importFileToVault(Path sourcePath) {
        if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
            System.err.println("Source file not found or is not a regular file: " + sourcePath);
            return null;
        }

        String fileName = sourcePath.getFileName().toString();
        Path destination = resolveNameConflict(basePath.resolve(fileName));

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

    // read

    /** Reads the file's raw bytes. Returns null on failure. */
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

    /** Lists everything (files and subfolders) directly inside the given folder. */
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

    /** Lists only the files (no subfolders) at the top level of the vault. */
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

    // delete

    /** Deletes a file from the vault. Returns true if it was actually deleted. */
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
}