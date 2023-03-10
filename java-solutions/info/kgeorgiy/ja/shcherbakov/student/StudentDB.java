package info.kgeorgiy.ja.shcherbakov.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements GroupQuery {

    private static final Comparator<Student> NAME_ORDER = Comparator.comparing(Student::getLastName).reversed()
            .thenComparing(Comparator.comparing(Student::getFirstName).reversed())
            .thenComparing(Student::getId);

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return students.stream().map(Student::getFirstName).toList();
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return students.stream().map(Student::getLastName).toList();
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return students.stream().map(Student::getGroup).toList();
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return students.stream().map((s) -> String.format("%s %s", s.getFirstName(), s.getLastName())).toList();
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream().map(Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Comparator.comparingInt(Student::getId)).map(Student::getFirstName).orElse("");
    }


    private <T, U extends Comparable<U>> List<T> sortedList(Stream<T> stream, Function<? super T, ? extends U> keyExtractor) {
        return stream.sorted(Comparator.comparing(keyExtractor)).toList();
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortedList(students.stream(), Student::getId);
    }

    private List<Student> sortedByName(Stream<Student> stream) {
        return stream.sorted(NAME_ORDER).toList();
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortedByName(students.stream());
    }

    private <T> Stream<Student> filteredByFieldEquals(Stream<Student> stream, Function<Student, T> getField, T element) {
        return stream.filter((s) -> getField.apply(s).equals(element));
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return sortedByName(filteredByFieldEquals(students.stream(), Student::getFirstName, name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return sortedByName(filteredByFieldEquals(students.stream(), Student::getLastName, name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return sortedByName(filteredByFieldEquals(students.stream(), Student::getGroup, group));
    }

    private static <T extends Comparable<? super T>> T min(T e1, T e2) {
        return e1.compareTo(e2) <= 0 ? e1 : e2;
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return filteredByFieldEquals(students.stream(), Student::getGroup, group)
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        StudentDB::min));
    }

    private static <R> Collector<Student, ?, HashMap<GroupName, R>> groupCollector(Collector<Student, ?, R> inGroupCollector) {
        return Collectors.groupingBy(Student::getGroup, HashMap::new, inGroupCollector);
    }

    private static Collector<Student, ?, List<Student>> sortedStudentsCollectorBy(Comparator<Student> comparator) {
        return Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(comparator)), ArrayList::new);
    }

    private static Stream<Group> makeGroups(Map<GroupName, List<Student>> groups) {
        return groups.entrySet().stream().map((e) -> new Group(e.getKey(), e.getValue()));
    }

    private static Stream<Group> collectGroups(Collection<Student> students, Collector<Student, ?, List<Student>> inGroupCollector) {
        return students.stream().collect(
                Collectors.collectingAndThen(
                        groupCollector(inGroupCollector),
                        StudentDB::makeGroups));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return sortedList(collectGroups(students, sortedStudentsCollectorBy(NAME_ORDER)), Group::getName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return sortedList(collectGroups(students, sortedStudentsCollectorBy(Comparator.comparing(Student::getId))), Group::getName);
    }

    private static GroupName largestGroupNameBy(Stream<Group> groupStream, Comparator<Group> comparator) {
        return groupStream.max(comparator).map(Group::getName).orElse(null);
    }

    private static Integer countOfStudents(Group group) {
        return group.getStudents().size();
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return largestGroupNameBy(collectGroups(students, Collectors.toList()),
                Comparator.comparing(StudentDB::countOfStudents)
                        .thenComparing(Group::getName));
    }

    private static Long countOfDistinctFirstNames(Group group) {
        return group.getStudents().stream().map(Student::getFirstName).distinct().count();
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return largestGroupNameBy(collectGroups(students, Collectors.toList()),
                Comparator.comparing(StudentDB::countOfDistinctFirstNames)
                        .thenComparing(Comparator.comparing(Group::getName).reversed()));
    }
}
