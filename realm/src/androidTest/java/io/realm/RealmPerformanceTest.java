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
import io.realm.entities.StringOnly;
import io.realm.entities.TimeMeasurement;

public class RealmPerformanceTest extends AndroidTestCase {

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
    public void testAddAndClear() {
        timeMeasurement.testAdd(testRealm);
    }

    //Test for overhead timing
    public void testOverhead() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {

            }
        });
    }

    //Test for insert timing.
    public void testInsert() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                for (int i = 0; i < timeMeasurement.DATA_SIZE; i++) {
                    StringOnly stringOnly = testRealm.createObject(StringOnly.class);
                    stringOnly.setChars("a");
                }
                testRealm.commitTransaction();
            }
        });
    }

    //Remove disabled from method name to run this test
    public void disabledtestInsertMultiCommits() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.MICROSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < timeMeasurement.DATA_SIZE; i++) {
                    testRealm.beginTransaction();
                    StringOnly stringOnly = testRealm.createObject(StringOnly.class);
                    stringOnly.setChars("a");
                    testRealm.commitTransaction();
                }
            }
        });
    }

    //Test for query timing
    public void testQuery() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                RealmResults<StringOnly> realmResults = testRealm.where(StringOnly.class).contains("chars", "200").findAll();
            }
        });
    }

    //Test for size timing
    public void testSize() {
        String name = getName();
        realmResults = testRealm.allObjects(StringOnly.class);
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults.size();
            }
        });
    }

    //Test for sorting timing
    public void testSort() {
        String name = getName();
        realmResults = testRealm.where(StringOnly.class).contains("chars", "test").findAll();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                realmResults.sort("chars");
            }
        });
    }

    //Test for clearing timing
    public void testClear() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                testRealm.clear(StringOnly.class);
                testRealm.commitTransaction();
            }
        });
    }

    //Test for enumerate and access query timing
    public void testEnumerateAndAccessQueryWithForLoop() {
        String name = getName();
        realmResults = testRealm.where(StringOnly.class).contains("chars", "test").findAll();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < realmResults.size(); i++) {
                    realmResults.get(i).getChars();
                }
            }
        });
    }

    //Test for enumerate and access query timing
    public void testEnumerateAndAccessQueryWithIterator() {
        String name = getName();
        realmResults = testRealm.where(StringOnly.class).contains("chars", "test").findAll();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                for (StringOnly stringOnly : realmResults) {
                    stringOnly.getChars();
                }
            }
        });
    }

    // Test for modifying values
/*    public void testEnumerateAndMutateWithForLoop() {
        String name = getName();
        realmResults = testRealm.allObjects(StringOnly.class);
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times,TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                for (int i = 0; i < realmResults.size(); i++) {
                    realmResults.get(i).setChars("c");
                }
                testRealm.commitTransaction();
            }
        });
    }*/

    //Not working right now
/*    public void testEnumerateAndMutateWithIterator() {
        String name = getName();
        realmResults = testRealm.allObjects(StringOnly.class);
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times,TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                testRealm.beginTransaction();
                for (StringOnly stringOnly : realmResults) {
                    stringOnly.setChars("c");
                }
                testRealm.commitTransaction();
            }
        });
    }*/

    //Test for insert multiple with index
/*    public void testInsertWithIndex() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
                    @Override
                    public void execute() {
                        testRealm.beginTransaction();
                        for (int i = 0; i < timeMeasurement.DATA_SIZE; i++) {
                            StringOnly stringOnly = testRealm.createObject(StringOnly.class);
                            stringOnly.setChars_index("a");
                        }
                        testRealm.commitTransaction();
                    }
                });
    }*/


    //Test for query with index timing
/*    public void testQueryWithIndex() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                    RealmResults<StringOnly> realmResults = testRealm.allObjects(StringOnly.class);
            }
        });
    }*/

    //Test for creating realm timing.
    //This is not completely accurate since close is called here also.
    public void testCreateAndClose() {
        String name = getName();
        timeMeasurement.timer(name, testRealm, warm_up_times, execute_times, TimeUnit.NANOSECONDS, new ExecutePerformance() {
            @Override
            public void execute() {
                for (int i = 0; i < 50; i++) {
                    Realm realm = Realm.getInstance(getContext());
                    realm.close();
                }
            }
        });
    }
}
