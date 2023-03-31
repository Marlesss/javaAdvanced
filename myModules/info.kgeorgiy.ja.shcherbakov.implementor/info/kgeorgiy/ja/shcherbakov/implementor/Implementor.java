package info.kgeorgiy.ja.shcherbakov.implementor;

import info.kgeorgiy.java.advanced.implementor.BaseImplementorTest;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
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

/**
 * Implementation class for {@link JarImpler} interface
 *
 * @author SHCHerbakov_Aleksei
 * @see ClassGenerator
 */
public class Implementor implements JarImpler {
    /**
     * Creates new instance of {@link Implementor}
     */
    public Implementor() {
    }

    /**
     * Main function which called to implement an abstract class or interface.
     * <ul>
     * <li> With 2 arguments {@code className rootPath} executes {@link #implement(Class, Path)}</li>
     * <li> With 3 arguments {@code -jar className jarPath} executes {@link #implementJar(Class, Path)}</li>
     * </ul>
     *
     * @param args arguments for running an application
     */

    public static void main(String[] args) {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null ||
                args.length == 3 && args[2] == null) {
            System.err.println("Wrong arguments");
            System.err.println("Pass 2 args to generate implemented java file: className rootPath");
            System.err.println("Pass 3 args to generate implemented jar file: -jar className filePath.jar");
            return;
        }
        if (args.length == 3) {
            if (!args[0].equals("-jar")) {
                System.err.println("With 3 arguments passed first argument must be \"-jar\"");
                System.err.println("Pass 3 args to generate implemented jar file: -jar className filePath.jar");
                return;
            }
            if (!args[2].endsWith(".jar")) {
                System.err.println("With 3 arguments passed third argument must be with .jar extension");
                return;
            }
        }

        try {
            Class<?> token = Class.forName(args[args.length == 2 ? 0 : 1]);
            Path path = Paths.get(args[args.length == 2 ? 1 : 2]);
            try {
                Implementor implementor = new Implementor();
                if (args.length == 2) {
                    implementor.implement(token, path);
                } else {
                    implementor.implementJar(token, path);
                }
            } catch (ImplerException e) {
                System.err.println("Error occurred while implementing:" + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class " + args[0] + " not found: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Wrong path: " + e.getMessage());
        }
    }

    /**
     * Creates directories on way to {@code path}
     *
     * @param toPath the path for which you want to create directories
     * @return {@code true} if successfully created directories on the path to {@code toPath}; {@code false} otherwise
     */
    private static boolean createDirectoriesOnWay(Path toPath) {
        try {
            Path toPathParent = toPath.getParent();
            if (toPathParent != null) {
                Files.createDirectories(toPathParent);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Can't create directories on way to " + toPath);
        }
        return false;
    }

    /**
     * Produces code implementing class or interface specified by provided {@code token}.
     * Generated class' name is the same as the class name of the type token with {@code Impl} suffix.
     * Generated source code is placed in the subdirectory of the specified {@code root} directory.
     * For example, the implementation of the interface {@link java.util.List} would
     * go to {@code $root/java/util/ListImpl.java}
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException when implementation cannot be
     *                                                                 generated for one of these reasons:
     *                                                                 <ul>
     *                                                                 <li> Given {@code class} is primitive or array. </li>
     *                                                                 <li> Given {@code class} is private. </li>
     *                                                                 <li> Given {@code class} is final class. </li>
     *                                                                 <li> Given {@code class} is {@link Enum}. </li>
     *                                                                 <li> {@link ClassGenerator#generate()} failed to generate source </li>
     *                                                                 </ul>
     */
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

    /**
     * Compiles {@code .class} files for {@code .java} files of given {@link Class} {@code token}.
     *
     * @param token   type token to compile.
     * @param tempDir directory with related {@code .java} files.
     * @throws ImplerException when the given {@link Class} {@code token} can not be compiled for one of these reasons:
     *                         <ul>
     *                         <li> Could not find java compiler. </li>
     *                         <li> {@link #getClassPath(Class)} failed to getting classPath. </li>
     *                         <li> Compiler exit code is not {@code 0}. </li>
     *                         </ul>
     */
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

    /**
     * Returns a path to implementation file of given {@link Class} {@code token}.
     *
     * @param token {@link Class} to get path.
     * @param root  root directory.
     * @param tail  file extension.
     * @return {@link Path} to implementation file.
     */
    private static Path getFilePath(Class<?> token, Path root, String tail) {
        return root.resolve(token.getPackageName().replace('.', File.separatorChar))
                .resolve(ClassGenerator.getClassName(token) + tail);
    }


    /**
     * Getting the class path of given {@link Class} {@code token}.
     *
     * @param token {@link Class} to get class path
     * @return class path of given {@link Class} {@code token}
     * @throws ImplerException if failed getting the class path
     */
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

    /**
     * Produces {@code jar} file implementing class or interface specified by provided {@code token}.
     * <p>
     * During implementation creates temporary folder with generated {@code .java} and {@code .class} files.
     * Informs user if fails to delete temporary folder.
     *
     * @param token   type token to create implementation for.
     * @param jarFile generated {@code jar} file path.
     * @throws ImplerException when implementation cannot be
     *                         generated for one of these reasons:
     *                         <ul>
     *                         <li> Failed to create temporary directory. </li>
     *                         <li> Failed to implement given {@link Class} {@code token} in {@link #implement(Class, Path)} </li>
     *                         <li> Failed to compile generated sources in {@link #compile(Class, Path)} </li>
     *                         <li> The problems with I/O occurred during implementation. </li>
     *                         </ul>
     */
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
