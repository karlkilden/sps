package com.kildeen.sps;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.Map;
import java.util.Objects;

public class LongTuple<T1, T2> implements Pair<T1, T2> {

    private final long id;
    private final Pair<T1, T2> pair;

    private LongTuple(long id, T1 one, T2 two) {
        this.id = id;
        this.pair = Tuples.pair(one, two);
    }

    public long getId() {
        return id;
    }

    @Override
    public T1 getOne() {
        return pair.getOne();
    }

    @Override
    public T2 getTwo() {
        return pair.getTwo();
    }

    @Override
    public void put(Map<? super T1, ? super T2> map) {
        pair.put(map);
    }

    @Override
    public Map.Entry<T1, T2> toEntry() {
        return pair.toEntry();
    }

    @Override
    public Pair<T2, T1> swap() {
        return pair.swap();
    }

    @Override
    public boolean isEqual() {
        return pair.isSame();
    }

    @Override
    public boolean isSame() {
        return pair.isSame();
    }

    @Override
    public int compareTo(Pair<T1, T2> other) {
        return pair.compareTo(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LongTuple<?, ?> longTuple = (LongTuple<?, ?>) o;
        return id == longTuple.id && Objects.equals(pair, longTuple.pair);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pair);
    }

    public static <T1, T2> LongTuple<T1, T2> of(long id, T1 one, T2 two) {
        return new LongTuple<>(id, one, two);
    }
}
