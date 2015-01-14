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
import java.util.concurrent.TimeUnit;

import io.realm.entities.ExecutePerformance;
import io.realm.entities.StringOnly;
import io.realm.entities.TimeMeasurement;

public class RealmPerformanceTest extends AndroidTestCase {

    private ArrayList<ArrayList<Long>> total_time = new ArrayList<ArrayList<Long>>();

    private Realm testRealm;

    private RealmResults<StringOnly> realmResults;

    private int execute_times = 10;
    private int warm_up_times = 10;

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

    //Test for overhead timing
    public void testOverheadTime() {
        total_time = timeMeasurement.timer(testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {

            }
        });
        Log.i("Warm up times for overhead:", String.valueOf(total_time.get(0)));
        Log.i("Test times for overhead:", String.valueOf(total_time.get(1)));
        Log.i("Statistics times for overhead:", String.valueOf(timeMeasurement.getStatisticsString()));
    }

    //Test for insert timing.
    public void testInsertMultiple() {
        total_time = timeMeasurement.timer(testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                for (int i = 0; i < 10000; i++) {
                    StringOnly stringOnly = testRealm.createObject(StringOnly.class);
                    stringOnly.setChars("a");
                }
                testRealm.commitTransaction();
            }
        });
        Log.i("Warm up times for insert:", String.valueOf(total_time.get(0)));
        Log.i("Test times for insert:", String.valueOf(total_time.get(1)));
        Log.i("Statistics times for insert:", String.valueOf(timeMeasurement.getStatisticsString()));
    }

    //Remove disabled from method name to run this test
    public void disabledtestInsertSingle() {
        total_time = timeMeasurement.timer(testRealm, warm_up_times, execute_times, TimeUnit.MICROSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < 10000; i++) {
                    testRealm.beginTransaction();
                    StringOnly stringOnly = testRealm.createObject(StringOnly.class);
                    stringOnly.setChars("a");
                    testRealm.commitTransaction();
                }
            }
        });
        Log.i("Warm up times for insert single:", String.valueOf(total_time.get(0)));
        Log.i("Test times for insert single:", String.valueOf(total_time.get(1)));
        Log.i("Statistics times for insert single:", String.valueOf(timeMeasurement.getStatisticsString()));
    }

    //Test for query timing
    public void testQuery() {
        total_time = timeMeasurement.timer(testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                RealmResults<StringOnly> realmResults = testRealm.where(StringOnly.class).contains("chars", "200").findAll();
            }
        });
        Log.i("Warm up times for query:", String.valueOf(total_time.get(0)));
        Log.i("Test times for query:", String.valueOf(total_time.get(1)));
        Log.i("Statistics times for query:", String.valueOf(timeMeasurement.getStatisticsString()));
    }

    //Test for size timing
    public void testSize() {
        realmResults = testRealm.allObjects(StringOnly.class);
        total_time = timeMeasurement.timer(testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults.size();
            }
        });
        Log.i("Warm up times for size:", String.valueOf(total_time.get(0)));
        Log.i("Test times for size:", String.valueOf(total_time.get(1)));
        Log.i("Statistics times for size:", String.valueOf(timeMeasurement.getStatisticsString()));
    }

    //Test for sorting timing
    public void testSorting() {
        realmResults = testRealm.where(StringOnly.class).contains("chars", "test").findAll();
        total_time = timeMeasurement.timer(testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults.sort("chars");
            }
        });
        Log.i("Warm up times for sorting:", String.valueOf(total_time.get(0)));
        Log.i("Test times for sorting:", String.valueOf(total_time.get(1)));
        Log.i("Statistics times for sorting:", String.valueOf(timeMeasurement.getStatisticsString()));
    }

    //Test for clearing timing
    public void testClear() {
        total_time = timeMeasurement.timer(testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                testRealm.clear(StringOnly.class);
                testRealm.commitTransaction();
            }
        });
        Log.i("Warm up times for clearing:", String.valueOf(total_time.get(0)));
        Log.i("Test times for clearing:", String.valueOf(total_time.get(1)));
        Log.i("Statistics times for clearing:", String.valueOf(timeMeasurement.getStatisticsString()));
    }

    //Test for enumerate and access query timing
    public void testEnumerateAndAccessQueryWithForLoop() {
        realmResults = testRealm.where(StringOnly.class).contains("chars", "test").findAll();
        total_time = timeMeasurement.timer(testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < realmResults.size(); i++) {
                    realmResults.get(i).getChars();
                }
            }
        });
        Log.i("Warm up times for enumerate and access with for loop:", String.valueOf(total_time.get(0)));
        Log.i("Test times for enumerate and access with for loop:", String.valueOf(total_time.get(1)));
        Log.i("Statistics times for enumerate and access with for loop:", String.valueOf(timeMeasurement.getStatisticsString()));
    }

    //Test for enumerate and access query timing
    public void testEnumerateAndAccessQueryWithIterator() {
        realmResults = testRealm.where(StringOnly.class).contains("chars", "test").findAll();
        total_time = timeMeasurement.timer(testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                for (StringOnly stringOnly : realmResults) {
                    stringOnly.getChars();
                }
            }
        });
        Log.i("Warm up times for enumerate and access with iterator:", String.valueOf(total_time.get(0)));
        Log.i("Test times for enumerate and access with iterator:", String.valueOf(total_time.get(1)));
        Log.i("Statistics times for enumerate and access with iterator:", String.valueOf(timeMeasurement.getStatisticsString()));
    }

    // Test for modifying values
    public void testEnumerateAndMutateWithForLoop() {
        realmResults = testRealm.allObjects(StringOnly.class);
        total_time = timeMeasurement.timer(testRealm, warm_up_times, execute_times,TimeUnit.NANOSECONDS, new ExecutePerformance() {
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
        Log.i("Warm up times for enumerate and mutate with for loop:", String.valueOf(total_time.get(0)));
        Log.i("Test times for enumerate and mutate with for loop:", String.valueOf(total_time.get(1)));
        Log.i("Statistics times for enumerate and mutate with for loop:", String.valueOf(timeMeasurement.getStatisticsString()));
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
    public void testCreateRealmAndClose() {
        total_time = timeMeasurement.timer(testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < 50; i++) {
                    Realm realm = Realm.getInstance(getContext());
                    realm.close();
                }
            }
        });
        Log.i("Warm up times for creating realm:", String.valueOf(total_time.get(0)));
        Log.i("Test times for creating realm:", String.valueOf(total_time.get(1)));
        Log.i("Statistics times for creating realm:", String.valueOf(timeMeasurement.getStatisticsString()));
    }
}
