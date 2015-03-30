/*
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class ImplicitTransaction extends Group {
    private class ObjectObserver<E extends RealmObject> {
        private final E object;
        private final Realm.Observer<E> observer;

        public ObjectObserver(final E object, Realm.Observer<E> observer) {
            this.object = object;
            this.observer = observer;
        }

        public Row getRow() {
            return object.getRow();
        }

        public void onChange() {
            observer.onChange(object);
        }

        public void onDelete() {
            observer.onChange(null);
        }
    }

    private class QueryObserver<E extends RealmObject> {
        private final RealmQuery<E> query;
        private final Realm.Observer<RealmResults<E>> observer;

        public QueryObserver(final RealmQuery<E> query, Realm.Observer<RealmResults<E>> observer) {
            this.query = query;
            this.observer = observer;
        }

        public Set<Table> getTables() {
            return query.getTables();
        }

        public void onChange() {
            observer.onChange(query.findAll());
        }

        public void onDelete() {
            observer.onChange(null);
        }
    }

    private final SharedGroup parent;

    private final ArrayList<ObjectObserver> observers = new ArrayList<ObjectObserver>();
    private final ArrayList<QueryObserver> tableObservers = new ArrayList<QueryObserver>();

    public ImplicitTransaction(Context context, SharedGroup sharedGroup, long nativePtr) {
        super(context, nativePtr, true);
        parent = sharedGroup;
    }

    public void advanceRead() {
        assertNotClosed();

        int[] tables = new int[5];
        for (QueryObserver observer : tableObservers) {
            for (Table table : observer.getTables()) {
                tables[table.getIndexInGroup()] = 1;
            }
        }

        long[] rows = new long[observers.size()];
        int i = 0;
        for (ObjectObserver observer : observers)
            rows[i++] = observer.getRow().nativePtr;
        long[] modified = parent.advanceRead(rows, tables);
        for (long modifiedIndex : modified)
            observers.get(i).onChange();

        for (i = observers.size() - 1; i >= 0; --i) {
            if (!observers.get(i).getRow().isAttached()) {
                observers.get(i).onDelete();
                observers.remove(i);
            }
        }
    }

    public void promoteToWrite() {
        assertNotClosed();
        if (immutable) {
            immutable = false;
            parent.promoteToWrite();
        } else {
            throw new IllegalStateException("Nested transactions are not allowed. Use commitTransaction() after each beginTransaction().");
        }
    }

    public void commitAndContinueAsRead() {
        assertNotClosed();
        parent.commitAndContinueAsRead();
        immutable = true;
    }

    public void endRead() {
        assertNotClosed();
        parent.endRead();
    }

    public void rollbackAndContinueAsRead() {
        assertNotClosed();
        if (!immutable) {
            parent.rollbackAndContinueAsRead();
            immutable = true;
        } else {
            throw new IllegalStateException("Cannot cancel a non-write transaction.");
        }
    }

    public <E extends RealmObject> void addObserver(E object, Realm.Observer<? super E> observer) {
        observers.add(new ObjectObserver(object, observer));
    }

    public <E extends RealmObject> void addObserver(RealmQuery query, Realm.Observer<RealmResults<E>> observer) {
        tableObservers.add(new QueryObserver(query, observer));
    }

    private void assertNotClosed() {
        if (isClosed() || parent.isClosed()) {
            throw new IllegalStateException("Cannot use ImplicitTransaction after it or its parent has been closed.");
        }
    }

    public boolean contains(final long[] array, final long v) {
        for (final long e : array)
            if (e == v)
                return true;
        return false;
    }

    protected void finalize() {} // Nullify the actions of Group.finalize()
}
