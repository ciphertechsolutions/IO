package com.ciphertechsolutions.io.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * A class to easily turn an OutputStream into an ObservableList<String>.
 */
public class ObservableOutputStream extends OutputStream implements ObservableList<String> {

    private StringBuilder currentString = new StringBuilder();
    private final ObservableList<String> wrappedObservable = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    @Override
    public int size() {
        return wrappedObservable.size();
    }

    @Override
    public boolean isEmpty() {
        return wrappedObservable.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return wrappedObservable.contains(o);
    }

    @Override
    public Iterator<String> iterator() {
        return wrappedObservable.iterator();
    }

    @Override
    public Object[] toArray() {
        return wrappedObservable.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return wrappedObservable.toArray(a);
    }

    @Override
    public boolean add(String e) {
        return wrappedObservable.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return wrappedObservable.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return wrappedObservable.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        return wrappedObservable.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends String> c) {
        return wrappedObservable.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return wrappedObservable.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return wrappedObservable.retainAll(c);
    }

    @Override
    public void clear() {
        wrappedObservable.clear();
    }

    @Override
    public String get(int index) {
        return wrappedObservable.get(index);
    }

    @Override
    public String set(int index, String element) {
        return wrappedObservable.set(index, element);
    }

    @Override
    public void add(int index, String element) {
        wrappedObservable.add(index, element);
    }

    @Override
    public String remove(int index) {
        return wrappedObservable.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return wrappedObservable.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return wrappedObservable.lastIndexOf(o);
    }

    @Override
    public ListIterator<String> listIterator() {
        return wrappedObservable.listIterator();
    }

    @Override
    public ListIterator<String> listIterator(int index) {
        return wrappedObservable.listIterator(index);
    }

    @Override
    public List<String> subList(int fromIndex, int toIndex) {
        return wrappedObservable.subList(fromIndex, toIndex);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        wrappedObservable.addListener(listener);

    }

    @Override
    public void removeListener(InvalidationListener listener) {
        wrappedObservable.removeListener(listener);
    }

    @Override
    public boolean addAll(String... elements) {
        return wrappedObservable.addAll(elements);
    }

    @Override
    public void addListener(ListChangeListener<? super String> listener) {
        wrappedObservable.addListener(listener);
    }

    @Override
    public void remove(int from, int to) {
        wrappedObservable.remove(from, to);
    }

    @Override
    public boolean removeAll(String... elements) {
        return wrappedObservable.removeAll(elements);
    }

    @Override
    public void removeListener(ListChangeListener<? super String> listener) {
        removeListener(listener);
    }

    @Override
    public boolean retainAll(String... elements) {
        return wrappedObservable.retainAll(elements);
    }

    @Override
    public boolean setAll(String... elements) {
        return wrappedObservable.setAll(elements);
    }

    @Override
    public boolean setAll(Collection<? extends String> col) {
        return setAll(col);
    }

    @Override
    public void write(int b) throws IOException {
        if (b < 0) {
            currentString.append((char) (b & 0x00FF)); // TODO: Not entirely sure why this is needed or if this works in all cases.
        }
        else {
            currentString.append((char) b);
        }
    }

    @Override
    public void flush() {
        synchronized (currentString) {
            if (currentString.length() > 0) {
                wrappedObservable.add(currentString.toString());
                currentString = new StringBuilder();
            }
        }
    }
}
