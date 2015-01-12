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
import android.util.Log;

import java.util.ArrayList;

import io.realm.entities.ExecutePerformance;
import io.realm.entities.StringOnly;
import io.realm.entities.TimeMeasurement;

public class RealmPerformanceTest extends AndroidTestCase {

    private ArrayList<String> total_time = new ArrayList<String>();

    protected Realm testRealm;

    RealmResults<StringOnly> realmResults;

    private int TEST_DATA_SIZE = 10000;
    private int execute_times = 1;

    TimeMeasurement timeMeasurement = new TimeMeasurement();

    @Override
    protected void setUp() throws Exception {
        Realm.deleteRealmFile(getContext());
        testRealm = Realm.getInstance(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        if (testRealm != null)
            testRealm.close();
    }

    //Creates data for realm with argument size
    private void addObjectToTestRealm(int objects) {
        testRealm.beginTransaction();
        testRealm.allObjects(StringOnly.class).clear();

        for (int i = 0; i < objects; ++i) {
            StringOnly stringOnly = testRealm.createObject(StringOnly.class);
            stringOnly.setChars("test data " + i);
        }
        testRealm.commitTransaction();
    }

    //Creates data for realm with size of TEST_DATA_SIZE
    private void addObjectToTestRealm() {
        addObjectToTestRealm(TEST_DATA_SIZE);
    }

    //Test for insert timing.
    public void testInsertMultiple() {
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                for (int i = 0; i < TEST_DATA_SIZE; i++) {
                    StringOnly stringOnly = testRealm.createObject(StringOnly.class);
                    stringOnly.setChars("a");
                }
                testRealm.commitTransaction();
            }
        });
        Log.i("Time for insert multiple:", String.valueOf(total_time));
    }

    //Remove disabled from method name to run this test
    public void disabledtestInsertSingle() {
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < TEST_DATA_SIZE; i++) {
                    testRealm.beginTransaction();
                    StringOnly stringOnly = testRealm.createObject(StringOnly.class);
                    stringOnly.setChars("a");
                    testRealm.commitTransaction();
                }
            }
        });
        Log.i("Time for insert single:", String.valueOf(total_time));
    }

    //Test for query timing
    public void testQuery() {
        addObjectToTestRealm();
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                RealmResults<StringOnly> realmResults = testRealm.where(StringOnly.class).contains("chars", "200").findAll();
            }
        });
        Log.i("Time for query:", String.valueOf(total_time));
    }

    //Test for size timing
    public void testSize() {
        addObjectToTestRealm();
        realmResults = testRealm.allObjects(StringOnly.class);
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults.size();
            }
        });
        Log.i("Time for size:", String.valueOf(total_time));
    }

    //Test for sorting timing
    public void testSorting() {
        addObjectToTestRealm();
        realmResults = testRealm.where(StringOnly.class).contains("chars", "test").findAll();
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults.sort("chars");
            }
        });
        Log.i("Time for sorting:", String.valueOf(total_time));
    }

    //Test for clearing timing
    public void testClear() {
        addObjectToTestRealm();
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                testRealm.clear(StringOnly.class);
                testRealm.commitTransaction();
            }
        });
        Log.i("Time for clearing:", String.valueOf(total_time));
    }

    //Test for closing timing
    public void testClose() {
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.close();
            }
        });
        Log.i("Time for closing:", String.valueOf(total_time));
    }

    //Test for enumerate and access query timing
    public void testEnumerateAndAccessQueryWithForLoop() {
        addObjectToTestRealm();
        realmResults = testRealm.where(StringOnly.class).contains("chars", "test").findAll();
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < realmResults.size(); i++) {
                    realmResults.get(i).getChars();
                }
            }
        });
        Log.i("Time for enumerate and access with for loop:", String.valueOf(total_time));
    }

    //Test for enumerate and access query timing
    public void testEnumerateAndAccessQueryWithIterator() {
        addObjectToTestRealm();
        realmResults = testRealm.where(StringOnly.class).contains("chars", "test").findAll();
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                for (StringOnly stringOnly : realmResults) {
                    stringOnly.getChars();
                }
            }
        });
        Log.i("Time for enumerate and access with iterator:", String.valueOf(total_time));
    }

    public void testEnumerateAndMutateWithForLoop() {
        addObjectToTestRealm();
        realmResults = testRealm.allObjects(StringOnly.class);
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                for (int i = 0; i < realmResults.size(); i++) {
                    realmResults.get(i).setChars("c");
                }
                testRealm.commitTransaction();
            }
        });
        Log.i("Time for enumerate and mutate with for loop:", String.valueOf(total_time));
    }

/*    //Not working right now
    public void testEnumerateAndMutateWithIterator() {
        addObjectToTestRealm();
        realmResults = testRealm.allObjects(StringOnly.class);
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                for (StringOnly stringOnly : realmResults) {
                    stringOnly.setChars("c");
                }
                testRealm.commitTransaction();
            }
        });
        Log.i("Time for enumerate and mutate with iterator:", String.valueOf(total_time));
    }*/

/*    //Test for insert multiple with index
    public void testInsertMultipleWithIndex() {
        start = System.nanoTime();
        testRealm.beginTransaction();
        for (int i = 0; i < TEST_DATA_SIZE; i++) {
            StringOnly stringOnly = testRealm.createObject(StringOnly.class);
            stringOnly.setChars_index("a");
        }
        testRealm.commitTransaction();
        stop = System.nanoTime();
        Log.i("Time for insert multiple with index:", timeMeasurement(start, stop));
    }*/


/*    //Test for query with index timing
    public void testQueryWithIndex() {
        for (int i = 0; i < TEST_DATA_SIZE; i++) {
            testRealm.beginTransaction();
            StringOnly stringOnly = testRealm.createObject(StringOnly.class);
            stringOnly.setChars_index("a");
            testRealm.commitTransaction();
        }
        start = System.nanoTime();
        RealmResults<StringOnly> realmResults = testRealm.allObjects(StringOnly.class);
        stop = System.nanoTime();
        Log.i("Time for query with index:", total_time);
    }*/

    //Test for creating realm timing.
    //This is not completely accurate since close is called here also.
    public void testCreateRealm() {
        total_time = timeMeasurement.timer(execute_times, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < 50; i++) {
                    Realm realm = Realm.getInstance(getContext());
                    realm.close();
                }
            }
        });
        Log.i("Time for creating realm:", String.valueOf(total_time));
    }
}
