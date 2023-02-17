package info.kgeorgiy.ja.shcherbakov.walk;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

public class RecursiveWalk extends Walk {
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("java RecursiveWalk <inputFile> <outputFile>");
            return;
        }
        String inputFile = args[0], outputFile = args[1];
        new RecursiveWalk(inputFile, outputFile);
    }

    RecursiveWalk(String inputFile, String outputFile) {
        super(inputFile, outputFile);
    }

    protected void handleFilePath(Path path) {
        if (Files.isDirectory(path)) {
            try (Stream<Path> paths = Files.walk(path)) {
                paths
                        .filter(Files::isRegularFile)
                        .forEach(super::handleFilePath);
            } catch (IOException e) {
                System.err.println("Error occurred while getting files in directory: " + e.getMessage());
            }
            return;
        }
        super.handleFilePath(path);
    }
}
