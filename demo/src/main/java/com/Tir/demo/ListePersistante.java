package com.Tir.demo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

// Une liste gardée en mémoire et recopiée dans sa table à chaque changement.
// L'application ne change ses listes que par add, removeIf et replaceAll (voir ClubDonnees) :
// ce sont les seules écritures permises, les autres refusent pour ne rien oublier d'enregistrer.
public class ListePersistante<T extends Identifiable> extends CopyOnWriteArrayList<T> {

    private final transient Table<T> table;

    public ListePersistante(Table<T> table) {
        super(table.toutLire());
        this.table = table;
    }

    public int plusGrandId() {
        return table.plusGrandId();
    }

    @Override
    public synchronized boolean add(T element) {
        table.inserer(element);
        return super.add(element);
    }

    @Override
    public synchronized boolean removeIf(Predicate<? super T> filtre) {
        List<T> aEffacer = stream().filter(filtre).toList();
        for (T element : aEffacer) {
            table.supprimer(element.getId());
        }
        return super.removeIf(x -> aEffacer.contains(x));
    }

    @Override
    public synchronized void replaceAll(UnaryOperator<T> operation) {
        for (int i = 0; i < size(); i++) {
            T ancien = get(i);
            T nouveau = operation.apply(ancien);
            if (nouveau != ancien) {
                table.modifier(nouveau);
                super.set(i, nouveau);
            }
        }
    }

    @Override public T set(int index, T element) { throw interdit(); }
    @Override public void add(int index, T element) { throw interdit(); }
    @Override public T remove(int index) { throw interdit(); }
    @Override public boolean remove(Object o) { throw interdit(); }
    @Override public boolean addIfAbsent(T element) { throw interdit(); }
    @Override public boolean addAll(Collection<? extends T> c) { throw interdit(); }
    @Override public boolean addAll(int index, Collection<? extends T> c) { throw interdit(); }
    @Override public int addAllAbsent(Collection<? extends T> c) { throw interdit(); }
    @Override public boolean removeAll(Collection<?> c) { throw interdit(); }
    @Override public boolean retainAll(Collection<?> c) { throw interdit(); }
    @Override public void clear() { throw interdit(); }
    @Override public void sort(Comparator<? super T> c) { throw interdit(); }

    private static UnsupportedOperationException interdit() {
        return new UnsupportedOperationException("Utiliser add, removeIf ou replaceAll (enregistrés dans la base)");
    }
}
