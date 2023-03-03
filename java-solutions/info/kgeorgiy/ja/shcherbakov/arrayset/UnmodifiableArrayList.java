package info.kgeorgiy.ja.shcherbakov.arrayset;

import java.util.*;

public class UnmodifiableArrayList<E> extends AbstractList<E> implements RandomAccess {
    private class UnmodifiableDescendingIterator implements Iterator<E> {
        int cursor = size() - 1;
        int lastRet = -1;
        int expectedModCount = modCount;

        @Override
        public boolean hasNext() {
            return cursor != -1;
        }

        @Override
        public E next() {
            checkForComodification();
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            try {
                int i = cursor;
                E next = get(i);
                lastRet = i;
                cursor = i - 1;
                return next;
            } catch (ArrayIndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException(e);
            }
        }

        void checkForComodification() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }

    private final List<E> list;
    private final boolean reversed;

    public UnmodifiableArrayList() {
        this(null);
    }

    public UnmodifiableArrayList(Collection<? extends E> c) {
        this(c, false);
    }

    public UnmodifiableArrayList(Collection<? extends E> c, boolean reversed) {
        if (c == null) {
            list = new ArrayList<>();
        } else {
            // :NOTE: костыль для копии?
            list = Collections.unmodifiableList(c.stream().toList());
        }
        this.reversed = reversed;
    }

    public UnmodifiableArrayList(List<E> list, boolean reversed) {
        this.list = list;
        this.reversed = reversed;
    }

    @Override
    public E get(int index) {
        return list.get(inParentIndex(index));
    }

    private int inParentIndex(int index) {
        return reversed ? size() - index - 1 : index;
    }

    @Override
    public int size() {
        return list.size();
    }

    public UnmodifiableArrayList<E> descendingList() {
        return new UnmodifiableArrayList<>(list, !reversed);
    }

    public Iterator<E> descendingIterator() {
        return new UnmodifiableDescendingIterator();
    }
}