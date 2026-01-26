package FileManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.util.stream.Stream;


public class FileManager {

    private Path basePath;

    public FileManager(String basePath) {
        this.basePath = Paths.get(basePath);     }

    public Path getBasePath() {
        return basePath;
    }
    public void setBasePath(Path basePath) {
        this.basePath = basePath;
    }

    public List<Path> getItemsInFolder(Path folder) {
        List<Path> items = new ArrayList<>();
        if (Files.isDirectory(folder)) {
            try (Stream<Path> stream = Files.list(folder)) {
                stream.forEach(file -> items.add(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return items;
    }

}
