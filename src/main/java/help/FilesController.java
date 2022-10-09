package help;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FilesController {
    private final File root;

    public FilesController(String pathToRoot) throws IllegalArgumentException {
        this.root = new File("C:\\Users\\vryab\\Main\\IT\\MyProjects\\LexilizeAddWordBot\\src\\main\\data");

        if (!this.root.isDirectory()) {
            throw new IllegalArgumentException("This file: " + pathToRoot + " is not folder.");
        }
    }

    public File openFile(String pathToFile) throws IllegalArgumentException {
        File file = new File(root, pathToFile);
        if (file.exists()) {
            return file;
        }
        throw new IllegalArgumentException(String.format("File %s doesn't exist", pathToFile));
    }

    public void addFolderIfNotExists(String pathToFolder) {
        File folder = new File(this.root, pathToFolder);

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public void createFile(String pathToFile) throws IOException {
        File file = new File(this.root, pathToFile);

        if (file.exists()) {
            throw new IllegalArgumentException("This file: " + file.getPath() + " already exists");
        }
        file.createNewFile();
    }

    public List<String> getAllNamesFileSameType(String pathToFolder, String type) {
        List<String> fileNames = new ArrayList<>();
        for (File file : new File(this.root, pathToFolder).listFiles()) {
            if (file.isFile() && file.getName().endsWith("." + type)) {
                fileNames.add(file.getName());
            }
        }

        return fileNames;
    }
}
