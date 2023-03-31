/**
 * Solution for <a href="https://www.kgeorgiy.info/courses/java-advanced/homeworks.html">Implementor</a> homework
 * of <a href="https://www.kgeorgiy.info/courses/java-advanced/">Java Advanced</a> course.
 *
 * @author SHCHerbakov_Aleksei
 */

module info.kgeorgiy.ja.shcherbakov.implementor {
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.implementor;

    requires java.compiler;
    requires java.desktop;

    exports info.kgeorgiy.ja.shcherbakov.implementor;
    opens info.kgeorgiy.ja.shcherbakov.implementor;
}