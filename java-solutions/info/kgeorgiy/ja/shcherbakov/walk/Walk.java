package info.kgeorgiy.ja.shcherbakov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class Walk {
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("java Walk <inputFile> <outputFile>");
            return;
        }
        String inputFile = args[0], outputFile = args[1];
        new Walk(inputFile, outputFile);
    }

    private BufferedReader reader;
    private BufferedWriter writer;
    private MessageDigest digest;

    Walk(String inputFile, String outputFile) {
        Path outputPath;
        try {
            outputPath = Path.of(outputFile);
            Path outputDirectory = outputPath.getParent();
            if (outputDirectory != null && !Files.isDirectory(outputDirectory)) {
                Files.createDirectories(outputDirectory);
            }
        } catch (InvalidPathException | IOException e) {
            System.err.println("Can't make the directories on the way to the output file: " + e.getMessage());
            return;
        } catch (SecurityException e) {
            System.err.println("Access denied while trying to make directories on the way to the output file:" + e.getMessage());
            return;
        }
        try (Reader inputReader = new FileReader(inputFile, StandardCharsets.UTF_8);
             BufferedReader inputBuffered = new BufferedReader(inputReader)) {
            try (BufferedWriter writerBuffered = Files.newBufferedWriter(outputPath)) {
                this.reader = inputBuffered;
                this.writer = writerBuffered;
                this.digest = MessageDigest.getInstance("SHA-256");
                run();
            } catch (FileNotFoundException e) {
                System.err.println("Output file not found: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error occurred while working with output file: " + e.getMessage());
            } catch (SecurityException e) {
                System.err.println("Access denied (" + outputFile + "):" + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Can't work with SHA-256 hashing algorithm: " + e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.err.println("Input file not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error occurred while working with input file: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Access denied (" + inputFile + "):" + e.getMessage());
        }
    }

    protected void run() {
        try {
            String pathString;
            while ((pathString = reader.readLine()) != null) {
                handlePathString(pathString);
            }
        } catch (IOException e) {
            System.err.println("Error occurred while working with files: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Access denied:" + e.getMessage());
        }
    }

    protected void handlePathString(String pathString) {
        try {
            Path filePath = Path.of(pathString);
            handleFilePath(filePath);
        } catch (InvalidPathException e) {
            System.err.println("Got invalid path: " + e.getMessage());
            try {
                writeHash(null, pathString);
            } catch (IOException ignored) {
            }
        }
    }

    protected void handleFilePath(Path filePath) {
        try {
            writeHash(getFileHash(filePath), filePath.toString());
        } catch (IOException e) {
            System.err.println("Error occurred while working with output file: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Access denied:" + e.getMessage());
        }
    }

    byte[] getFileHash(Path path) {
        try (
                InputStream inputStream = Files.newInputStream(path);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)
        ) {
            byte[] buffer = new byte[8192];
            int count;
            digest.reset();
            while ((count = bufferedInputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, count);
            }
            return digest.digest();
        } catch (IOException e) {
            System.err.println("Error occurred while calculating hash of file (" + path + "):" + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Access denied (" + path + "):" + e.getMessage());
        }
        return null;
    }

    private void writeHash(byte[] hash, String pathString) throws IOException {
        if (hash == null) {
            digest.reset();
            hash = digest.digest();
            Arrays.fill(hash, (byte) 0);
        }
        StringBuilder stringHash = new StringBuilder();
        for (byte hashByte : hash) {
            stringHash.append(String.format("%02x", hashByte));
        }
        writer.write(String.format("%s %s", stringHash, pathString));
        writer.newLine();
    }
}
