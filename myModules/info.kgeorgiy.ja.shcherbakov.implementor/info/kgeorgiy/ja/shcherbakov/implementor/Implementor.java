package info.kgeorgiy.ja.shcherbakov.implementor;

import info.kgeorgiy.java.advanced.implementor.BaseImplementorTest;
import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.swing.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class Implementor implements JarImpler {
    public static void main(String[] args) {
        if (args == null || args.length < 2 || 3 < args.length || args[0] == null || args[1] == null) {
            System.err.println("Wrong arguments");
            System.err.println("2 args to generate implemented java file: className rootPath");
            System.err.println("3 args to generate implemented jar file: -jar className filePath.jar");
            return;
        }

        // TODO: add 3 args main
        try {
            Class<?> token = Class.forName(args[0]);
            new Implementor().implement(token, Paths.get(args[1]));
            compile(token, Paths.get(args[1]));
        } catch (ClassNotFoundException e) {
            System.err.println("Class " + args[0] + " not found: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Wrong path: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Error occurred while implementing:" + e.getMessage());
        }
    }

    private static boolean createDirectoriesOnWay(Path toPath) {
        try {
            Path toPathParent = toPath.getParent();
            if (toPathParent != null) {
                Files.createDirectories(toPathParent);
                return true;
            }
        } catch (IOException e) {
            System.err.println("Can't create directories on way to " + toPath);
        }
        return false;
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        int mods = token.getModifiers();
        if (token.isPrimitive()) {
            throw new ImplerException("Interface or Class token expected");
        }

        if (Modifier.isPrivate(mods)) {
            throw new ImplerException("Can't implement the private token");
        }

        if (Modifier.isFinal(mods)) {
            throw new ImplerException("Can't extend the final class");
        }

        if (token == Enum.class) {
            throw new ImplerException("Can't extend the Enum");
        }

        Path classPath = root.resolve(token.getPackageName().replace('.', File.separatorChar))
                .resolve(ClassGenerator.getClassName(token) + ".java");

        if (!createDirectoriesOnWay(classPath)) {
            return;
        }

        try (
                BufferedWriter writer = Files.newBufferedWriter(classPath)
        ) {
            ClassGenerator classGenerator = new ClassGenerator(token);
            writer.write(classGenerator.generate());
        } catch (IOException e) {
            System.err.println("Error while writing generated class to path " + classPath);
            System.err.println(e.getMessage());
        }
    }

    private static void compile(Class<?> token, Path tempDir) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }

        List<String> args = new ArrayList<>();
        args.add(getFilePath(token, tempDir, ".java").toString());
        String classPath = getClassPath(token);
        if (classPath != null) {
            args.add("-cp");
            args.add(classPath);
        }

        int exitCode = compiler.run(null, null, null, args.toArray(String[]::new));
        if (exitCode != 0) {
            throw new ImplerException("Compiler exit code: " + exitCode);
        }
    }

    private static Path getFilePath(Class<?> token, Path root, String tail) {
        return root.resolve(token.getPackageName().replace('.', File.separatorChar))
                .resolve(ClassGenerator.getClassName(token) + tail);
    }


    private static String getClassPath(Class<?> token) throws ImplerException {
        try {
            CodeSource codeSource = token.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }
            return Path.of(codeSource.getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            throw new ImplerException("Can't convert URL to URI", e);
        }
    }

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (!createDirectoriesOnWay(jarFile)) {
            return;
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "temp");
        } catch (IOException e) {
            throw new ImplerException("Can't create temporary directory", e);
        }

        try {
            implement(token, tempDir);
            compile(token, tempDir);

            Manifest manifest = new Manifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                writer.putNextEntry(
                        new ZipEntry((token.getPackageName() + "." + token.getSimpleName())
                                .replace('.', '/') + "Impl" + ".class"));
                Files.copy(getFilePath(token, tempDir, ".class"), writer);
            } catch (IOException e) {
                throw new ImplerException("Can't write to jar file " + jarFile, e);
            }
        } finally {
            try {
                BaseImplementorTest.clean(tempDir);
            } catch (IOException e) {
                System.err.println("Can't delete temporary directory: " + e.getMessage());
            }
        }
    }
}
