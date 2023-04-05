module info.kgeorgiy.ja.shcherbakov {
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.implementor;

    requires java.compiler;
    requires java.desktop;

    exports info.kgeorgiy.ja.shcherbakov.implementor;
    opens info.kgeorgiy.ja.shcherbakov.implementor;
}