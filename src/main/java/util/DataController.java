package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataController {
    private final File root;

    public DataController(String relativePathToDataFolder) throws IllegalArgumentException {
        File projectFolder = new File(System.getProperty("user.dir"));
        this.root = new File(projectFolder, relativePathToDataFolder);

        if (!this.root.isDirectory()) {
            throw new IllegalArgumentException("This file: " + this.root.getPath() + " is not folder.");
        }
    }

    public File openFile(String pathToFile) throws IllegalArgumentException {
        File file = new File(root, pathToFile);
        if (file.exists()) {
            return file;
        }
        throw new IllegalArgumentException("File " + pathToFile + " doesn't exist");
    }

    public void addFolderIfNotExists(String pathToFolder) {
        File folder = new File(this.root, pathToFolder);

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public File createFile(String pathToFile) throws IOException {
        File file = new File(this.root, pathToFile);

        if (file.exists()) {
            throw new IllegalArgumentException("File " + file.getPath() + " already exists");
        }
        file.createNewFile();
        return file;
    }

    public List<File> getAllNamesFileSameType(String pathToFolder, String type) {
        List<File> fileNames = new ArrayList<>();
        for (File file : new File(this.root, pathToFolder).listFiles()) {
            if (file.isFile() && file.getName().endsWith("." + type)) {
                fileNames.add(file);
            }
        }

        return fileNames;
    }

    public void removeFile(String pathToFile) throws IllegalArgumentException, IOException {
        File file = new File(root, pathToFile);
        if (!file.exists()) {
            throw new IllegalArgumentException("File " + pathToFile + " doesn't exist");
        } else {
            file.delete();
        }
    }
}
