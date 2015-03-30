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

package io.realm.rxjava;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import rx.Observable;
import rx.Subscriber;

/**
 * Factory class for creating observables from Realm types. Most likely from
 *
 * - RealmObject
 * - RealmList
 * - RealmResults
 * - RealmQuery
 *
 * This is the alternative to baking the support in directly and would allow us to split it into
 * a separate dependency 'io.realm:realm-android-rx' project. Making it optional opt-in.
 *
 * API gets a bit more verbose though
 *
 */
public class RxRealm {

    public static <E extends RealmObject> Observable<E> create(E object) {
        return Observable.just(object);
    }

    public static <E extends RealmObject> Observable<RealmList<E>> create(RealmList<E> list) {
        return Observable.just(list);
    }

    public static <E extends RealmObject> Observable<RealmResults<E>> create(RealmQuery<E> query) {
        // add notification to Realm as we need to know whenever the Realm tables are changed so
        // query can be rerun.

        // RealmQueryObservers should most likely be treated as never ending observables.
        return null;
    }

    static class RxObserverAdaptor<T extends Object> implements Realm.Observer<T> {
        private final Subscriber<? super T> subscriber;

        public RxObserverAdaptor(final Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onChange(T newValue) {
            if (newValue != null)
                subscriber.onNext(newValue);
            else
                subscriber.onCompleted();
        }
    }

    public static <E extends RealmObject> Observable<E> observable(final Realm realm, final E object) {
        return Observable.create(new Observable.OnSubscribe<E>() {
            @Override
            public void call(final Subscriber<? super E> observer) {
                realm.observe(object, new RxObserverAdaptor<>(observer));
            }
        });
    }

    public static <E extends RealmObject> Observable<RealmResults<E>> observe(final Realm realm, final RealmQuery<E> query) {
        return Observable.create(new Observable.OnSubscribe<RealmResults<E>>() {
            @Override
            public void call(Subscriber<? super RealmResults<E>> subscriber) {
                realm.observe(query, new RxObserverAdaptor<RealmResults<E>>(subscriber));
            }
        });
    }
}
