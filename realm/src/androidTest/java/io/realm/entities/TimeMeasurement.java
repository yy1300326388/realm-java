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

package io.realm.entities;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;

public class TimeMeasurement {

    private ArrayList<ArrayList<Long>> times = new ArrayList<ArrayList<Long>>();
    private ArrayList<Double> statistics_times = new ArrayList<Double>();

    private String time_Unit = "ns";

    private int DATA_SIZE = 10000;

    public void clearRealm(Realm testRealm) {
        testRealm.beginTransaction();
        testRealm.clear(StringOnly.class);
        testRealm.commitTransaction();
    }

    //Creates data for realm with argument size
    public void addObjectToTestRealm(int objects, Realm testRealm) {
        testRealm.beginTransaction();
        testRealm.allObjects(StringOnly.class).clear();

        for (int i = 0; i < objects; ++i) {
            StringOnly stringOnly = testRealm.createObject(StringOnly.class);
            stringOnly.setChars("test data " + i);
        }
        testRealm.commitTransaction();
    }

    public ArrayList<ArrayList<Long>> timer(Realm testRealm, int times_to_warm_up, int times_to_execute, TimeUnit timeUnit, ExecutePerformance executePerformance) {
        ArrayList<Long> warm_up_times = new ArrayList<Long>();
        ArrayList<Long> test_times = new ArrayList<Long>();
        int invalid = 0;

        for (int i = 0; i < times_to_execute + times_to_warm_up; i++) {
            addObjectToTestRealm(DATA_SIZE, testRealm);
            long start = System.nanoTime();
            executePerformance.execute();
            long stop = System.nanoTime();
            long time_nano = stop - start;
            if (time_nano != 0) {
                if (i < times_to_warm_up) {
                    warm_up_times.add(i - invalid, time_nano);
                } else {
                    test_times.add(i - warm_up_times.size() - invalid, time_nano);
                }
            } else {
                invalid++;
            }
            clearRealm(testRealm);
        }
        times.add(0, warm_up_times);
        times.add(1, test_times);
        if (timeUnit != TimeUnit.NANOSECONDS) {
            times = timeConverting(times, timeUnit);
        }
        setStatistics(times.get(1));
        return times;
    }

    public ArrayList<ArrayList<Long>> timeConverting(ArrayList<ArrayList<Long>> times, TimeUnit timeUnit) {
        setTimeUnit(timeUnit);
        for (int i = 0; i < times.size(); i++) {
            for (int j = 0; j < times.get(i).size(); j++) {

                switch (timeUnit) {
                    case MICROSECONDS: {
                        long time_micro = TimeUnit.MICROSECONDS.convert(times.get(i).get(j), TimeUnit.NANOSECONDS);
                        times.get(i).set(j, time_micro);
                    }break;
                    case MILLISECONDS: {
                        long time_milli = TimeUnit.MILLISECONDS.convert(times.get(i).get(j), TimeUnit.NANOSECONDS);
                        times.get(i).set(j, time_milli);
                    }break;
                    case SECONDS: {
                        long time_sec = TimeUnit.SECONDS.convert(times.get(i).get(j), TimeUnit.NANOSECONDS);
                        times.get(i).set(j, time_sec);
                    }break;
                    default:
                        break;
                }

            }
        }
        return times;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case MICROSECONDS: {
                time_Unit = "μs";
            }break;
            case MILLISECONDS: {
                time_Unit = "ms";
            }break;
            case SECONDS: {
                time_Unit = "s";
            }break;
            default:
                break;
        }

    }

    public ArrayList<ArrayList<String>> timeAsString(ArrayList<ArrayList<Long>> times, TimeUnit timeUnit) {
        ArrayList<ArrayList<String>> times_string = new ArrayList<ArrayList<String>>();
        ArrayList<String> warm_up_times_string = new ArrayList<String>();
        ArrayList<String> test_times_string = new ArrayList<String>();
        for (int i = 0; i < times.size(); i++) {
            for (int j = 0; j < times.get(i).size(); j++) {
                if (i == 0) {
                    warm_up_times_string.add(j, String.valueOf(times.get(0).get(j)) + time_Unit);
                } else {
                    test_times_string.add(j, String.valueOf(times.get(1).get(j)) + time_Unit);
                }
            }
        }
        times_string.add(0, warm_up_times_string);
        times_string.add(1, test_times_string);
        return times_string;
    }

    public double minimum(ArrayList<Long> times) {
        double min = times.get(0);
        for (int i = 1; i < times.size(); i++) {
            if (times.get(i) < min) {
                min = times.get(i);
            }
        }
        return min;
    }

    public double maximum(ArrayList<Long> times) {
        double max = times.get(0);
        for (int i = 1; i < times.size(); i++) {
            if (times.get(i) > max) {
                max = times.get(i);
            }
        }
        return max;
    }
    public double average(ArrayList<Long> times) {
        double sum = 0;
        for (int i = 0; i < times.size(); i++) {
            sum += times.get(i);
        }
        return sum/times.size();
    }

    public double variance(ArrayList<Long> times)
    {
        double avg = average(times);
        double temp = 0;
        for(int i = 0; i < times.size(); i++) {
            temp += (avg - (double) times.get(i)) * (avg - (double) times.get(i));
        }
        return (temp/times.size());
    }

    public double stdDev(ArrayList<Long> times) {
        return Math.sqrt(variance(times));
    }

    public void setStatistics(ArrayList<Long> times){
        //statistics_times.clear();
        statistics_times.add(minimum(times));
        statistics_times.add(maximum(times));
        statistics_times.add(average(times));
        statistics_times.add(variance(times));
        statistics_times.add(stdDev(times));
    }

    public ArrayList<Double> getStatistics() {
        return statistics_times;
    }

    public ArrayList<String> getStatisticsString() {
        ArrayList<String> statistics_times_string = new ArrayList<String>();
        for (int i = 0; i < statistics_times.size(); i++) {
            statistics_times_string.add(i, String.valueOf(statistics_times.get(i)) + time_Unit);
        }
        return statistics_times_string;
    }
}
