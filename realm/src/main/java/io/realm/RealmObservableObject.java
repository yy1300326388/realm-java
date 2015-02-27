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

import io.realm.annotations.RealmClass;
import io.realm.internal.Row;
import rx.Observable;

@RealmClass
public abstract class RealmObservableObject<E> extends Observable<E> {

    protected Row row;
    protected Realm realm;


    /**
     * Creates an Observable with a Function to execute when it is subscribed to.
     * <p/>
     * <em>Note:</em> Use {@link #create(rx.Observable.OnSubscribe)} to create an Observable, instead of this constructor,
     * unless you specifically have a need for inheritance.
     *
     * @param f {@link rx.Observable.OnSubscribe} to be executed when {@link #subscribe(Subscriber)} is called
     */
    protected RealmObservableObject(OnSubscribe<E> f) {
        super(f);
    }
}
