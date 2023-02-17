package info.kgeorgiy.ja.shcherbakov.walk;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


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

    Walk(String inputFile, String outputFile) {
        try {
            Path outputPath = Path.of(outputFile);
            Path outputDirectory = outputPath.getParent();
            if (outputDirectory != null && !Files.isDirectory(outputDirectory)) {
                Files.createDirectories(outputDirectory);
            }
        } catch (InvalidPathException | IOException e) {
            System.err.println("Can't make the directories on the way to the output file: " + e.getMessage());
            return;
        }
        try (Reader inputReader = new FileReader(inputFile);
             BufferedReader inputBuffered = new BufferedReader(inputReader);
             BufferedWriter writerBuffered = Files.newBufferedWriter(Path.of(outputFile))
        ) {
            this.reader = inputBuffered;
            this.writer = writerBuffered;
            run();
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error occurred while opening input/output files: " + e.getMessage());
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
        }
    }

    protected void handlePathString(String pathString) {
        try {
            Path filePath = Path.of(pathString);
            handleFilePath(filePath);
        } catch (InvalidPathException e) {
            System.err.println("Got invalid path: " + e.getMessage());
            try {
                writeHash(BigInteger.ZERO, pathString);
            } catch (IOException ignored1) {
            }
        }
    }

    protected void handleFilePath(Path filePath) {
        try {
            writeHash(getFileHash(filePath), filePath.toString());
        } catch (IOException e) {
            System.err.println("Error occurred while working with output file: " + e.getMessage());
        }
    }

    BigInteger getFileHash(Path path) {
        try (
                InputStream inputStream = Files.newInputStream(path);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)
        ) {
            byte[] buffer = new byte[8192];
            int count;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            while ((count = bufferedInputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, count);
            }
            byte[] hash = digest.digest();
            return new BigInteger(1, hash);
        } catch (NoSuchAlgorithmException e) {
            System.err.println(e.getMessage());
        } catch (IOException ignored) {
        }
        return BigInteger.ZERO;
    }

    private void writeHash(BigInteger hash, String pathString) throws IOException {
        writer.write(String.format("%064x %s", hash, pathString));
        writer.newLine();
    }
}
