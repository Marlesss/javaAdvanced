package info.kgeorgiy.ja.shcherbakov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Implementor implements Impler {

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Wrong arguments");
            System.err.println("java Implementor class_name root_path");
            return;
        }


        try {
            new Implementor().implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (ClassNotFoundException e) {
            System.err.println("Class " + args[0] + " not found: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Wrong path: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Error occurred while implementing:" + e.getMessage());
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        int mods = token.getModifiers();
        if (token.isPrimitive()) {
            throw new ImplerException("Interface or Class token expected");
        }

        if (Modifier.isPrivate(mods)) {
            throw new ImplerException("Can't implement private token");
        }

        // :NOTE: интерфейсы бывают final?
        if (!token.isInterface() && Modifier.isFinal(mods)) {
            throw new ImplerException("Couldn't extend final class");
        }

        if (token == Enum.class) {
            throw new ImplerException("Couldn't extend enum");
        }

        Path classPath = root.resolve(token.getPackageName().replace('.', File.separatorChar))
                .resolve(ClassGenerator.getClassName(token) + ".java");
        try {
            Path classPathParent = classPath.getParent();
            if (classPathParent != null) {
                Files.createDirectories(classPathParent);
            }
        } catch (IOException e) {
            System.err.println("Couldn't create directories on way to the generated class");
            return;
        }

        try (
                BufferedWriter writer = Files.newBufferedWriter(classPath)
        ) {
            ClassGenerator classGenerator = new ClassGenerator(token);
            writer.write(classGenerator.generate());
        } catch (IOException e) {
            System.err.println("Error while writing generated class to path " + classPath);
            System.err.println(e);
        }
    }
}
