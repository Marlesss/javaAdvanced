package info.kgeorgiy.ja.shcherbakov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class ClassGenerator {

    private static final String LINE_SEP = System.lineSeparator();
    private static final String OVERRIDE = "@Override";
    private final Class<?> token;

    ClassGenerator(Class<?> token) {
        this.token = token;
    }

    public static String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    public String generate() throws ImplerException {
        StringBuilder sb = new StringBuilder();
        generatePackage(sb);
        generateHeader(sb);
        generateConstructors(sb);
        generateMethods(sb);
        generateFooter(sb);
        return sb.toString();
    }

    private static String getPadding(int size) {
        return "\t".repeat(size);
    }

    private static String getParameters(Executable executable, boolean withTypes) {
        StringJoiner stringJoiner = new StringJoiner(", ", "(", ")");
        for (Parameter parameter : executable.getParameters()) {
            stringJoiner.add((withTypes ? parameter.getType().getCanonicalName() + " " : "") + parameter.getName());
        }
        return stringJoiner.toString();
    }

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

    private static String generateReturn(Method method) {
        if (method.getReturnType() == void.class) {
            return "";
        }
        return "return " + getDefaultValue(method.getReturnType());
    }

    private static String generateSuper(Constructor<?> constructor) {
        return "super" + getParameters(constructor, false);
    }

    private static String getDefaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return "false";
        }
        if (returnType.isPrimitive()) {
            return "0";
        }
        return "null";
    }

    private void generatePackage(StringBuilder sb) {
        // :NOTE: что если у имплементируемого класса нет пекеджа?
        String packageName = token.getPackage().getName();
        if (!packageName.isEmpty()) {
            sb.append("package ").append(token.getPackage().getName()).append(';').append(LINE_SEP).append(LINE_SEP);
        }
    }

    private void generateHeader(StringBuilder sb) {
        sb.append("public class ").append(getClassName(token)).append(' ');
        if (token.isInterface()) {
            sb.append("implements ");
        } else {
            sb.append("extends ");
        }
        sb.append(token.getCanonicalName()).append(" {").append(LINE_SEP);
    }

    private void generateConstructors(StringBuilder sb) throws ImplerException {
        if (token.isInterface()) {
            return;
        }
        List<Constructor<?>> constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter((constructor -> !Modifier.isPrivate(constructor.getModifiers()))).toList();
        if (constructors.size() == 0) {
            throw new ImplerException("There is no default constructor available");
        }
        // :NOTE: если у имплементируемого класса сотня конструкторов, тебе не нужны они все
//        constructors.forEach(constructor -> generateExecutable(sb, constructor));
        generateExecutable(sb, constructors.get(0));
    }

    private record UniqueMethod(Method method) {

        @Override
        public int hashCode() {
            // :NOTE: пожалуйста не надо придумывать хешкод самостоятельно, возьми стандартную функцию
//            return (method.getName().hashCode() * 31 + method.getReturnType().hashCode()) * 31 +
//                    Arrays.hashCode(method.getParameterTypes());
            return Objects.hash(method.getName(), method.getReturnType(), Arrays.hashCode(method.getParameterTypes()));
        }

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

    private void generateFooter(StringBuilder sb) {
        sb.append('}');
    }
}
