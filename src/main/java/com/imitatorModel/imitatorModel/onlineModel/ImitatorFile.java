package com.imitatorModel.imitatorModel.onlineModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImitatorFile {

    private final Path path;

    public ImitatorFile(String path) {
        this.path = Path.of(path);
    }

    public void write(ImitatorDocument document)
            throws IOException {

        Files.writeString(
                path,
                document.render());
    }
}