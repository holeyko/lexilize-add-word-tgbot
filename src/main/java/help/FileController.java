package help;

import java.io.File;
import java.io.IOException;

public class FileController {
    private final File root;

    public FileController(String pathToRoot) throws IllegalArgumentException {
        this.root = new File(pathToRoot);

        if (!this.root.isDirectory()) {
            throw new IllegalArgumentException("This file: " + pathToRoot + " is not folder.");
        }
    }

    public void addFolderIfNotExists(String pathToFolder) {
        File folder = new File(this.root, pathToFolder);

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public void createFileIfNotExists(String pathToFolder, String fileName) throws IOException {
        File folder = new File(this.root, pathToFolder);

        if (!folder.exists()) {
            throw new IllegalArgumentException("This folder: " + pathToFolder + " doesn't exists.");
        }

        File file = new File(folder, fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    public boolean doesFolderExist(String pathToFolder) {
        return new File(this.root, pathToFolder).exists();
    }
}
