package info.kgeorgiy.ja.shcherbakov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a class which implements provided {@link Class} {@code token}.
 *
 * @author SHCHerbakov_Aleksei
 * @see Implementor
 */
public class ClassGenerator {
    /**
     * String value of system line separator.
     */
    private static final String LINE_SEP = System.lineSeparator();
    /**
     * String value of overriding mark.
     */
    private static final String OVERRIDE = "@Override";
    /**
     * Given {@link Class} {@code token} to implement.
     */
    private final Class<?> token;

    /**
     * Creates new instance of {@link ClassGenerator}
     *
     * @param token {@link Class} {@code token} to implement
     */
    ClassGenerator(Class<?> token) {
        this.token = token;
    }

    /**
     * Return the name of generated class which implements {@link Class} {@code token}.
     *
     * @param token {@link Class} {@code token} to get name of.
     * @return the name of generated class.
     */
    public static String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Generates a class which implements provided {@link Class} {@code token}.
     *
     * @return String with generated class code.
     * @throws ImplerException if implementation was failed.
     */
    public String generate() throws ImplerException {
        StringBuilder sb = new StringBuilder();
        generatePackage(sb);
        generateHeader(sb);
        generateConstructors(sb);
        generateMethods(sb);
        generateFooter(sb);
        return sb.toString();
    }

    /**
     * Returns the padding of the desired size.
     *
     * @param size size of padding.
     * @return string of tabs multiplied by size.
     */
    private static String getPadding(int size) {
        return "\t".repeat(size);
    }

    /**
     * Generates the parameters for the {@link Executable} {@code executable} with the types if needed.
     *
     * @param executable {@link Executable} {@code executable} to generate parameters for
     * @param withTypes  whether to generate types.
     * @return String of generated parameters.
     */
    private static String getParameters(Executable executable, boolean withTypes) {
        StringJoiner stringJoiner = new StringJoiner(", ", "(", ")");
        for (Parameter parameter : executable.getParameters()) {
            stringJoiner.add((withTypes ? parameter.getType().getCanonicalName() + " " : "") + parameter.getName());
        }
        return stringJoiner.toString();
    }

    /**
     * Generates a description of the errors it throws for the {@link Executable} {@code method}.
     *
     * @param method {@link Executable} {@code method} to generate throws for.
     * @return String of generated description.
     */
    private static String generateThrows(Executable method) {
        Class<?>[] exceptions = method.getExceptionTypes();
        if (exceptions.length == 0) {
            return "";
        }

        StringJoiner sj = new StringJoiner(", ", " throws ", "");
        for (Class<?> exception : exceptions) {
            sj.add(exception.getCanonicalName());
        }
        return sj.toString();
    }

    /**
     * Generates a return value for the {@link Executable} {@code method}.
     *
     * @param method {@link Executable} {@code method} to generate return value for.
     * @return String of generated return value.
     */
    private static String generateReturn(Method method) {
        if (method.getReturnType() == void.class) {
            return "";
        }
        return "return " + getDefaultValue(method.getReturnType());
    }

    /**
     * Generates a call of super constructor with the same arguments for {@link Constructor} {@code constructor}.
     *
     * @param constructor {@link Constructor} {@code constructor} to generate for.
     * @return String of the generated call of super constructor.
     */
    private static String generateSuper(Constructor<?> constructor) {
        return "super" + getParameters(constructor, false);
    }

    /**
     * Generates a default value for {@link Class} {@code returnType}.
     * If {@link Class} {@code returnType} is the {@code boolean.class}, default value would be {@code false}.
     * If {@link Class} {@code returnType} is primitive, default value would be {@code 0}.
     * Otherwise, default value would be {@code null}.
     *
     * @param returnType {@link Class} to generate for.
     * @return String of the generated default value.
     */
    private static String getDefaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return "false";
        }
        if (returnType.isPrimitive()) {
            return "0";
        }
        return "null";
    }

    /**
     * Generates package of the provided {@link Class} {@code token}.
     *
     * @param sb {@link StringBuilder} where to save the result.
     */
    private void generatePackage(StringBuilder sb) {
        // :NOTE: что если у имплементируемого класса нет пекеджа?
        String packageName = token.getPackage().getName();
        if (!packageName.isEmpty()) {
            sb.append("package ").append(token.getPackage().getName()).append(';').append(LINE_SEP).append(LINE_SEP);
        }
    }

    /**
     * Generates the header for the implementation class of the provided {@link Class} {@code token}.
     *
     * @param sb {@link StringBuilder} where to save the result.
     */
    private void generateHeader(StringBuilder sb) {
        sb.append("public class ").append(getClassName(token)).append(' ');
        if (token.isInterface()) {
            sb.append("implements ");
        } else {
            sb.append("extends ");
        }
        sb.append(token.getCanonicalName()).append(" {").append(LINE_SEP);
    }

    /**
     * Generates the constructor for the implementation class of the provided {@link Class} {@code token}.
     *
     * @param sb {@link StringBuilder} where to save the result.
     * @throws ImplerException if there is no available default constructor.
     */
    private void generateConstructors(StringBuilder sb) throws ImplerException {
        if (token.isInterface()) {
            return;
        }
        List<Constructor<?>> constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter((constructor -> !Modifier.isPrivate(constructor.getModifiers()))).toList();
        if (constructors.size() == 0) {
            throw new ImplerException("There is no available default constructor");
        }
        // :NOTE: если у имплементируемого класса сотня конструкторов, тебе не нужны они все
//        constructors.forEach(constructor -> generateExecutable(sb, constructor));
        generateExecutable(sb, constructors.get(0));
    }

    /**
     * Static record used for correct comparison for equality of {@link Method}s with the same signature
     * by comparing their names, return types and parameter's types.
     *
     * @param method {@link Method} to wrap.
     */
    private record UniqueMethod(Method method) {

        /**
         * Calculates hashcode for this wrapper using hashes of name, return type and parameter's types
         * of {@link #method}.
         *
         * @return hashcode for this wrapper.
         */
        @Override
        public int hashCode() {
            // :NOTE: пожалуйста не надо придумывать хешкод самостоятельно, возьми стандартную функцию
//            return (method.getName().hashCode() * 31 + method.getReturnType().hashCode()) * 31 +
//                    Arrays.hashCode(method.getParameterTypes());
            return Objects.hash(method.getName(), method.getReturnType(), Arrays.hashCode(method.getParameterTypes()));
        }

        /**
         * Compares the passed {@code obj} for equality with this class.
         *
         * @param obj the reference object with which to compare.
         * @return {@code true} if the passed {@code obj} is instance of {@link UniqueMethod} and has the same name,
         * return type and parameter's types. {@code false} otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj instanceof UniqueMethod uniqueMethod) {
                return method.getName().equals(uniqueMethod.method.getName()) &&
                        method.getReturnType() == uniqueMethod.method.getReturnType() &&
                        Arrays.equals(method.getParameterTypes(), uniqueMethod.method.getParameterTypes());
            }
            return false;
        }
    }

    /**
     * Generates implementation of abstract methods for the provided {@link Class} {@code token}.
     *
     * @param sb {@link StringBuilder} where to save the result.
     */
    private void generateMethods(StringBuilder sb) {
        Set<UniqueMethod> methods = new HashSet<>();
        // :NOTE: почему именно Collectors.toCollection(() -> methods) ? чем не понравился более читаемый стандартный вариант?
        Arrays.stream(token.getMethods()).map(UniqueMethod::new).collect(Collectors.toCollection(() -> methods));
//        for (Method m : token.getMethods()) {
//            methods.add(new UniqueMethod(m));
//        }

        Class<?> tempToken = token;
        while (tempToken != null) {
            Arrays.stream(tempToken.getDeclaredMethods()).map(UniqueMethod::new)
                    .collect(Collectors.toCollection(() -> methods));
            tempToken = tempToken.getSuperclass();
        }
        HashMap<String, Map<List<Class<?>>, List<UniqueMethod>>> collect = methods.stream().collect(
                Collectors.groupingBy((m) -> m.method.getName(), HashMap::new,
                        Collectors.groupingBy((m) -> Arrays.stream(m.method.getParameterTypes()).toList(), Collectors.toList())));
        for (String methodName : collect.keySet()) {
            for (List<UniqueMethod> intersectedMethods : collect.get(methodName).values()) {
                UniqueMethod uniqueMethod = intersectedMethods.get(0);
                for (int i = 1; i < intersectedMethods.size(); i++) {
                    if (uniqueMethod.method.getReturnType().isInstance(intersectedMethods.get(i).method.getReturnType())) {
                        uniqueMethod = intersectedMethods.get(i);
                    }
                }
                if (Modifier.isAbstract(uniqueMethod.method.getModifiers())) {
                    generateExecutable(sb, uniqueMethod.method);
                }
            }
        }
    }

    /**
     * Generates implementation of the provided {@link Executable} {@code executable}.
     * If the provided {@link Executable} {@code executable} is instance of {@link Method}, it would have
     * generated default return value.
     * Otherwise, it would have generated call of super constructor with the same arguments.
     *
     * @param sb         {@link StringBuilder} where to save the result.
     * @param executable {@link Executable} to generate implementation for.
     */
    private void generateExecutable(StringBuilder sb, Executable executable) {
        if (executable instanceof Method) {
            sb.append(getPadding(1)).append(OVERRIDE).append(LINE_SEP);
        }
        sb.append(getPadding(1)).append(Modifier.isPublic(executable.getModifiers()) ? "public " : "protected ");
        if (executable instanceof Method method) {
            sb.append(method.getReturnType().getCanonicalName()).append(' ').append(method.getName());
        } else {
            sb.append(getClassName(token));
        }
        sb.append(getParameters(executable, true))
                .append(generateThrows(executable)).append(" {").append(LINE_SEP);

        sb.append(getPadding(2));
        if (executable instanceof Method method) {
            sb.append(generateReturn(method));
        } else {
            sb.append(generateSuper((Constructor<?>) executable));
        }
        sb.append(';').append(LINE_SEP)
                .append(getPadding(1)).append('}').append(LINE_SEP);
    }

    /**
     * Generates the end of implementation class.
     *
     * @param sb {@link StringBuilder} where to save the result.
     */
    private void generateFooter(StringBuilder sb) {
        sb.append('}');
    }
}
