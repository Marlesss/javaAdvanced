package info.kgeorgiy.ja.shcherbakov.arrayset;

import java.nio.file.Path;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        ArraySet<Integer> arraySet = new ArraySet<>(List.of(1, 2, 3));
        System.out.println(UnmodifiableArrayList.cntOfGetOps);
        arraySet.descendingSet().descendingSet().descendingSet().descendingSet().descendingSet().first();
        System.out.println(UnmodifiableArrayList.cntOfGetOps);
    }
}
