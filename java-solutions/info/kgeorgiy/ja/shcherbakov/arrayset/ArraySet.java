package info.kgeorgiy.ja.shcherbakov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E>, NavigableSet<E> {
    private final List<E> elements;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        this((Collection<? extends E>) null, null);
    }

    public ArraySet(Collection<? extends E> c) {
        this(c, null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this((Collection<? extends E>) null, comparator);
    }

    @SuppressWarnings("unchecked")
    public ArraySet(Collection<? extends E> c, Comparator<? super E> comparator) {
        if (c == null) {
            elements = new UnmodifiableArrayList<>();
        } else {
            if (c instanceof SortedSet && ((SortedSet<? extends E>) c).comparator() == comparator) {
                elements = new UnmodifiableArrayList<>(c);
            } else {
                TreeSet<E> treeSet = new TreeSet<>(comparator);
                treeSet.addAll(c);
                elements = new UnmodifiableArrayList<>(treeSet);
            }
        }
        this.comparator = comparator;
    }

    private ArraySet(List<E> elements, Comparator<? super E> comparator) {
        this.elements = elements;
        this.comparator = comparator;
    }

    private ArraySet<E> getEmpty() {
        return new ArraySet<>(new UnmodifiableArrayList<>(), comparator);
    }

    @SuppressWarnings("unchecked")
    private int compare(E a, E b) {
        if (comparator == null) {
            return ((Comparable<E>) a).compareTo(b);
        }
        return comparator.compare(a, b);
    }

    private int indexOf(E e, int ifNotFound, int ifFound) {
        int index = Collections.binarySearch(elements, e, comparator);
        if (index < 0) {
            return -index - 1 + ifNotFound;
        }
        return index + ifFound;
    }

    private E getOrNull(int index) {
        if (index == -1 || index == size()) {
            return null;
        }
        return elements.get(index);
    }

    @Override
    public E lower(E e) {
        return getOrNull(indexOf(e, -1, -1));
    }

    @Override
    public E floor(E e) {
        return getOrNull(indexOf(e, -1, 0));
    }

    @Override
    public E ceiling(E e) {
        return getOrNull(indexOf(e, 0, 0));
    }

    @Override
    public E higher(E e) {
        return getOrNull(indexOf(e, 0, 1));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> iterator() {
        return elements.iterator();
    }

    // :NOTE: a.descendingSet().descendingSet().descendingSet().descendingSet().descendingSet().descendingSet().....
    // насоздается куча объектов + get(index) будет занимать O(k), где k - количество .descendingSet()

    // :: Не согласен, прошу обратить внимание на UnmodifiableArrayList.descendingList().
    // elements не копируется, а хранится один и тот же c разным reversed
    // get(index) занимает O(1), пример в Main
    @Override
    public ArraySet<E> descendingSet() {
        return new ArraySet<>(((UnmodifiableArrayList<E>) elements).descendingList(), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return ((UnmodifiableArrayList<E>) elements).descendingIterator();
    }

    @Override
    public ArraySet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        return getSubSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    private ArraySet<E> getSubSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int fromIndex = indexOf(fromElement, 0, fromInclusive ? 0 : 1);
        int toIndex = indexOf(toElement, 0, toInclusive ? 1 : 0);
        if (fromIndex > toIndex) {
            if (compare(fromElement, toElement) > 0) {
                throw new IllegalArgumentException();
            }
            return getEmpty();
        }
        return new ArraySet<>(elements.subList(fromIndex, toIndex), comparator);
    }

    @Override
    public ArraySet<E> headSet(E toElement, boolean inclusive) {
        if (isEmpty()) {
            return getEmpty();
        }
        return getSubSet(first(), true, toElement, inclusive);
    }

    @Override
    public ArraySet<E> tailSet(E fromElement, boolean inclusive) {
        if (isEmpty()) {
            return getEmpty();
        }
        return getSubSet(fromElement, inclusive, last(), true);
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public ArraySet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public ArraySet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public ArraySet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return elements.get(0);
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return elements.get(size() - 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(elements, (E) o, comparator) >= 0;
    }
}
