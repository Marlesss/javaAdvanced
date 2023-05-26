package info.kgeorgiy.ja.shcherbakov.rmi;

import org.junit.runner.JUnitCore;

public class BankTests {
    public static void main(String[] args) {
        System.exit(new JUnitCore().run(BankTest.class).wasSuccessful() ? 0 : 1);
    }
}
