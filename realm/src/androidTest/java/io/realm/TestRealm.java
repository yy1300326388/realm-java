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

import android.test.AndroidTestCase;

import io.realm.entities.StringOnly;

public class TestRealm extends AndroidTestCase {

    private Realm testRealm;

    public static final int DATA_SIZE = 100000;

    public void getRealm() {
        Realm.deleteRealmFile(getContext());
        testRealm = Realm.getInstance(getContext());
    }


    public void close() {
        if (testRealm != null)
            testRealm.close();
    }

    public void clearRealm() {
        testRealm.beginTransaction();
        testRealm.clear(StringOnly.class);
        testRealm.commitTransaction();
    }

    //Creates data for realm with argument size
    public void addObjectToTestRealm(int objects) {
        testRealm.beginTransaction();
        for (int i = 0; i < objects; ++i) {
            StringOnly stringOnly = testRealm.createObject(StringOnly.class);
            stringOnly.setChars("test data " + i);
        }
        testRealm.commitTransaction();
    }

    public void testAdd() {
        for (int i = 0; i < 10; i++) {
            getRealm();
            addObjectToTestRealm(DATA_SIZE);
            close();
            //clearRealm();
        }
    }
}
