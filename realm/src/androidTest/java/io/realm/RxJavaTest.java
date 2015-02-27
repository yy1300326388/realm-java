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

package io.realm;

import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.realm.entities.Dog;
import io.realm.rxjava.RxRealm;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

public class RxJavaTest extends AndroidTestCase {

    private String LOG_TAG = "RXJAVA";
    private Realm realm;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Realm.deleteRealmFile(getContext());
        realm = Realm.getInstance(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        realm.close();
    }

    public void testStandAloneObject() {
        new Dog("Foo").observable().subscribe(new Subscriber<Dog>() {
            @Override
            public void onCompleted() {
                Log.i(LOG_TAG, "Done");
            }

            @Override
            public void onError(Throwable e) {
                Log.i(LOG_TAG, "Error");
            }

            @Override
            public void onNext(Dog dog) {
                Log.i(LOG_TAG, "Type:" + dog.getName());
            }
        });
    }

    public void testSingleObject() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealm(new Dog("Foo"));
            }
        });

        // Same thread
        Observable<Dog> obj = realm.allObjects(Dog.class).first().observable();
        Subscriber<Dog> subscriber = new Subscriber<Dog>() {
            @Override
            public void onCompleted() {
                Log.i(LOG_TAG, "Done");
            }

            @Override
            public void onError(Throwable e) {
                Log.i(LOG_TAG, "Error");
            }

            @Override
            public void onNext(Dog dog) {
                Log.i(LOG_TAG, "Type:" + dog.getName());
            }
        };
        obj.subscribe(subscriber);
    }

    public void testSingleObjectOnAnotherThread() throws InterruptedException {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealm(new Dog("Foo"));
            }
        });

        Subscriber sub = new Subscriber<Dog>() {
            @Override
            public void onCompleted() {
                Log.i(LOG_TAG, "Done");
            }

            @Override
            public void onError(Throwable expected) {
                Log.i(LOG_TAG, expected.toString());
            }

            @Override
            public void onNext(Dog dog) {
                Log.i(LOG_TAG, "Type:" + dog.getName());
            }
        };

        // Try on another thread, doesn't work
        realm.allObjects(Dog.class).first().observable()
                .subscribeOn(Schedulers.newThread())
                .subscribe(sub);

        SystemClock.sleep(100);
    }

    public void testAddonAPI() {

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealm(new Dog("Foo"));
            }
        });

        // Same thread
        Observable<Dog> obj = RxRealm.create(realm.allObjects(Dog.class).first());
        obj.subscribe(new Action1<Dog>() {
            @Override
            public void call(Dog dog) {
                Log.i(LOG_TAG, "Type:" + dog.getName());
            }
        });
    }

    public void testMultipleSubscribers() {

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealm(new Dog("Foo"));
            }
        });


        Action1<Dog> subscriber = new Action1<Dog>() {
            @Override
            public void call(Dog dog) {
                Log.i(LOG_TAG, "Type:" + dog.getName());
            }
        };


        // Same thread
        Observable<Dog> obj = RxRealm.create(realm.allObjects(Dog.class).first());
        ConnectableObservable<Dog> connectable = obj.publish();
        connectable.subscribe(subscriber);
        connectable.subscribe(subscriber);
        connectable.connect();
    }

    // Testing on RealmResults are trivially the same as for objects.
    // But what about Queries? What is the semantics of observing on a query?
    // Most likely it is you want to be notified with the new result each time it changes
    public void testQuerys() {

    }
}
