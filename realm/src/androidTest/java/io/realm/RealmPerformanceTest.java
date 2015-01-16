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

import java.util.concurrent.TimeUnit;

import io.realm.entities.ExecutePerformance;
import io.realm.entities.Performance;
import io.realm.entities.TimeMeasurement;

// After the tests have been ran you can do "adb pull /data/data/io.realm.test/files/ your/directory"
// in your terminal to copy the folder where the text files are.
// The text file name describes which test and what time unit it was ran in.

public class RealmPerformanceTest extends AndroidTestCase {

    private Realm testRealm;

    private RealmResults<Performance> realmResults;

    //Sets numbers of time to execute the test.
    private int execute_times = 10;

    //Sets numbers of time to warm up running the test.
    private int warm_up_times = 10;

    private TimeMeasurement timeMeasurement = new TimeMeasurement();

    //Sets time unit for time in tests.
    private TimeUnit timeUnit = TimeUnit.NANOSECONDS;

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
    public void testOverhead() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {

            }
        });
    }

    //Test for integer insert timing.
    public void testInsertInteger() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                for (int i = 0; i < timeMeasurement.DATA_SIZE; i++) {
                    Performance performance = testRealm.createObject(Performance.class);
                    performance.setInteger(3);
                }
                testRealm.commitTransaction();
            }
        });
    }

    //Test for string insert timing.
    public void testInsertString() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                for (int i = 0; i < timeMeasurement.DATA_SIZE; i++) {
                    Performance performance = testRealm.createObject(Performance.class);
                    performance.setString("a");
                }
                testRealm.commitTransaction();
            }
        });
    }

    //Remove disabled from method name to run this test.
    public void disabledtestInsertIntegerMultiCommits() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < timeMeasurement.DATA_SIZE; i++) {
                    testRealm.beginTransaction();
                    Performance performance = testRealm.createObject(Performance.class);
                    performance.setInteger(3);
                    testRealm.commitTransaction();
                }
            }
        });
    }

    //Remove disabled from method name to run this test.
    public void disabledtestInsertStringMultiCommits() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < timeMeasurement.DATA_SIZE; i++) {
                    testRealm.beginTransaction();
                    Performance performance = testRealm.createObject(Performance.class);
                    performance.setString("a");
                    testRealm.commitTransaction();
                }
            }
        });
    }

    //Test for query timing.
    public void testQuery() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                RealmResults<Performance> realmResults = testRealm.where(Performance.class).contains("string", "200").findAll();
            }
        });
    }

    //Test for size timing.
    public void testSize() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults = testRealm.allObjects(Performance.class);
                realmResults.size();
            }
        });
    }

    //Test for sorting timing.
    public void testSort() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults = testRealm.where(Performance.class).contains("string", "test").findAll();
                realmResults.sort("string");
            }
        });
    }

    //Test for clearing timing.
    public void testClear() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                testRealm.clear(Performance.class);
                testRealm.commitTransaction();
            }
        });
    }

    //Test for enumerating and access query with for loop timing.
    public void testEnumerateAndAccessQueryWithForLoop() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults = testRealm.where(Performance.class).contains("string", "test").findAll();
                for (int i = 0; i < realmResults.size(); i++) {
                    realmResults.get(i).getString();
                }
            }
        });
    }

    //Test for enumerating and access query with iterator timing.
    public void testEnumerateAndAccessQueryWithIterator() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults = testRealm.where(Performance.class).contains("string", "test").findAll();
                for (Performance performance : realmResults) {
                    performance.getString();
                }
            }
        });
    }

    //Test for enumerating modifying values with for loop timing.
    public void testEnumerateAndMutateStringWithForLoop() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults = testRealm.where(Performance.class).contains("string", "test").findAll();
                testRealm.beginTransaction();
                for (int i = 0; i < realmResults.size(); i++) {
                    realmResults.get(i).setString("c");
                }
                testRealm.commitTransaction();
            }
        });
    }

    //Not working right now.
    //Test for enumerating modifying values with iterator timing.
/*    public void testEnumerateAndMutateWithIterator() {
        String name = getName();
        realmResults = testRealm.allObjects(Performance.class);
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                for (Performance Performance : realmResults) {
                    Performance.setString("c");
                }
                testRealm.commitTransaction();
            }
        });
    }*/

    //Test for string insert with index timing.
    public void testInsertStringWithIndex() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                for (int i = 0; i < timeMeasurement.DATA_SIZE; i++) {
                    Performance performance = testRealm.createObject(Performance.class);
                    performance.setString_index("a");
                }
                testRealm.commitTransaction();
            }
        });
    }


    //Test for query with index timing.
    public void testQueryWithIndex() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                RealmResults<Performance> realmResults = testRealm.where(Performance.class).contains("string_index", "200").findAll();
            }
        });
    }

    //Test for enumerating and access query timing.
    public void testEnumerateAndAccessQueryWithForLoopIndex() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults = testRealm.where(Performance.class).contains("string_index", "index").findAll();
                for (int i = 0; i < realmResults.size(); i++) {
                    realmResults.get(i).getString_index();
                }
            }
        });
    }

    //Test for creating and closing realm timing.
    public void testCreateAndClose() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < 50; i++) {
                    Realm realm = Realm.getInstance(getContext());
                    realm.close();
                }
            }
        });
    }
    //Test for minimum timing.
    public void testMinimum() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults = testRealm.where(Performance.class).findAll();
                double min = realmResults.min("integer").doubleValue();
            }
        });
    }

    //Test for maximum timing.
    public void testMaximum() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults = testRealm.where(Performance.class).findAll();
                double max = realmResults.max("integer").doubleValue();
            }
        });
    }

    //Test for summing timing.
    public void testSum() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults = testRealm.where(Performance.class).findAll();
                double sum = realmResults.sum("integer").doubleValue();
            }
        });
    }

    //Test for average timing.
    public void testAverage() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults = testRealm.where(Performance.class).findAll();
                double avg = realmResults.average("integer");
            }
        });
    }

    //Test for transaction timing.
    public void testTransaction() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                testRealm.commitTransaction();
            }
        });
    }

    //Test for transaction block timing.
    public void testTransactionBlock() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {

                    }
                });
            }
        });
    }

    //Test for allObjects timing.
    public void testAllObjects() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, timeUnit, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.allObjects(Performance.class);
            }
        });
    }
}
