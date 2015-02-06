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

import android.test.AndroidTestCase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;

public class TimeMeasurement extends AndroidTestCase {

    private String time_Unit = "ns";

    //Size of data for testing.
    public static final int DATA_SIZE = 10000;

    //sets file type
    private String file_type = "dat";

    private String internal_path = "/data/data/io.realm.test/files/";
    private String external_path = "/storage/emulated/legacy/";

    private DecimalFormat decimalFormat = new DecimalFormat("##.###");

    //Decides where to save the file. true if internal, false if external.
    //To copy the files open the terminal and type adb pull /where/the/directory/is/saved /where/you/want/to/copy/to
    //For sd card do adb pull /storage/emulated/legacy/the_directory_name where_to_copy/it
    //For internal memory adb pull /data/data/io.realm.test/files/the_directory_name where_to_copy/it
    private boolean isInternal = false;

    //Clears data from realm.
    public void clearRealm(Realm testRealm) {
        testRealm.beginTransaction();
        testRealm.clear(Performance.class);
        testRealm.commitTransaction();
    }

    //Creates performance objects data.
    public void addPerformanceObjects(int objects, Realm testRealm) {
        testRealm.beginTransaction();
        for (int i = 0; i < objects; i++) {
            Performance performance = testRealm.createObject(Performance.class);
            performance.setString("test data " + i);
            performance.setString_index("index data " + i);
            performance.setInteger(i);
        }
        testRealm.commitTransaction();
    }

    //Creates performance objects data.
    public void addAllTypesObjects(int objects, Realm testRealm) {
        testRealm.beginTransaction();
        for (int i = 0; i < objects; i++) {
            AllTypes allTypes = testRealm.createObject(AllTypes.class);
            allTypes.setColumnBoolean((i % 2) == 0);
            allTypes.setColumnDouble(3.1415 + i);
            allTypes.setColumnFloat(1.234567f + i);
            allTypes.setColumnString("test data " + i);
            allTypes.setColumnLong(i);
        }
        testRealm.commitTransaction();
    }

    //Sets up data. Measures timing for method. Clears Data.
    public void timer(String name, Realm testRealm, int times_to_warm_up, int times_to_execute,
                      TimeUnit timeUnit, ExecutePerformance executePerformance) {
        setTimeUnit(timeUnit);
        long old_stop = 0;
        long frozen = 0;
        String fileName_test = name + "_in_" + time_Unit;
        String fileName_warm_up = "warm_up_" + name + "_in_" + time_Unit;
        deleteDir(name);
        for (int i = 0; i < times_to_execute + times_to_warm_up; i++) {
            if (!name.equals("testQueryConstruction")) {
                addPerformanceObjects(DATA_SIZE, testRealm);
            } else {
                addAllTypesObjects(DATA_SIZE, testRealm);
            }
            long start = System.nanoTime();
            executePerformance.execute();
            long stop = System.nanoTime();
            if (old_stop != start && stop != start && frozen != start) {
                double time = ((double) (stop - start) / 1.0);
                if (timeUnit != TimeUnit.NANOSECONDS) {
                    time = timeConverting(stop, start, timeUnit);
                }
                if (i < times_to_warm_up) {
                    write(name, fileName_warm_up, decimalFormat.format(time));
                } else {
                    write(name, fileName_test, decimalFormat.format(time));
                }
                old_stop = stop;
            } else {
                frozen = stop;
                i--;
            }
            clearRealm(testRealm);
        }
        setStatistics(getFile(name), name);
    }

    //Creates and writes to file
    public void write(String dirName, String fileName, String content) {
        try {
            String dir_path = "";
            String file_path = "";
            if (isInternal == true) {
                dir_path = internal_path + dirName;
                file_path = dir_path + "/" + fileName + "." + file_type;
            } else {
                dir_path = external_path + dirName;
                file_path = dir_path + "/" + fileName + "." + file_type;
            }
            File dir = new File(dir_path);
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(file_path);

            if (!file.exists()) {
                file.createNewFile();
                if (file_type.equals("dat")) {
                    FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write("##;###\n" +
                            "@LiveGraph demo file.\n" +
                            "Time");
                    bw.newLine();
                    bw.close();
                }
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Deletes file.
    public void deleteDir(String name) {
        String dir_path = "";
        if (isInternal == true) {
            dir_path = internal_path + name;
            } else {
            dir_path = external_path + name;
            }

        File dir = new File(dir_path);
        if (dir.isDirectory()) {
            String[] files = dir.list();
            for (int i = 0; i < files.length; i++) {
                new File(dir, files[i]).delete();
            }
        }
    }

    //Fetches file.
    public File getFile(String name) {
        String file_path = "";
        if (isInternal == true) {
            file_path = internal_path + name + "/" + name + "_in_" + time_Unit + "." + file_type;
        } else {
            file_path = external_path + name + "/" + name + "_in_" + time_Unit + "." + file_type;
        }
        File file = new File(file_path);
        return file;
    }

    //Converts tests time to other time unit.
    public double timeConverting(long stop, long start, TimeUnit timeUnit) {
        double time = 0;
        switch (timeUnit) {
            case MICROSECONDS: {
                time = ((double) stop / 1000.0) - ((double) start / 1000.0);
            }
            break;
            case MILLISECONDS: {
                time = ((double) stop / 1000000.0) - ((double) start / 1000000.0);
            }
            break;
            case SECONDS: {
                time = ((double) stop / 1000000000.0) - ((double) start / 1000000000.0);
            }
            break;

            default:
                break;
        }
        return time;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case MICROSECONDS: {
                time_Unit = "μs";
            }
            break;
            case MILLISECONDS: {
                time_Unit = "ms";
            }
            break;
            case SECONDS: {
                time_Unit = "s";
            }
            break;
            default:
                break;
        }

    }

    //Finds fastest time.
    public double minimum(File file) {
        double current = 0;
        double min = 0;
        try {
            Scanner scanner = new Scanner(file);
            try {
                for (int i = 0; i < 3; i++) {
                    scanner.nextLine();
                }
                min = Double.valueOf(scanner.nextLine());
                while (scanner.hasNextLine()) {
                    current = Double.valueOf(scanner.nextLine());
                    if (current < min) {
                        min = current;
                    }
                }
            } catch (Exception e) {
            }

        } catch (FileNotFoundException e) {
        }

        return min;
    }

    //Finds longest time.
    public double maximum(File file) {
        double current = 0;
        double max = 0;
        try {
            Scanner scanner = new Scanner(file);
            for (int i = 0; i < 3; i++) {
                scanner.nextLine();
            }
            max = Double.valueOf(scanner.nextLine());
            try {
                max = Double.valueOf(scanner.nextLine());
                while (scanner.hasNextLine()) {
                    current = Double.valueOf(scanner.nextLine());
                    if (current > max) {
                        max = current;
                    }
                }
            } catch (Exception e) {
            }
        } catch (FileNotFoundException e) {

        }
        return max;
    }

    //Calculates average time.
    public double average(File file) {
        double sum = 0;
        int count = 0;
        try {
            Scanner scanner = new Scanner(file);
            for (int i = 0; i < 3; i++) {
                scanner.nextLine();
            }
            try {
                while (scanner.hasNextLine()) {
                    sum += Double.valueOf(scanner.nextLine());
                    count++;
                }
            } catch (Exception e) {
            }
        } catch (FileNotFoundException e) {

        }
        return sum / count;
    }

    //Calculates variance.
    public double variance(File file) {
        double avg = average(file);
        double temp = 0;
        double current = 0;
        int count = 0;
        try {
            Scanner scanner = new Scanner(file);
            for (int i = 0; i < 3; i++) {
                scanner.nextLine();
            }
            try {
                while (scanner.hasNextLine()) {
                    current = Double.valueOf(scanner.nextLine());
                    temp += ((avg - current) * (avg - current));
                    count++;
                }
            } catch (Exception e) {
            }
        } catch (FileNotFoundException e) {

        }
        return temp / count;
    }

    //Calculates standard deviation.
    public double stdDev(File file) {
        return Math.sqrt(variance(file));
    }

    //Calculates the highest percent difference.
    public double minMaxPercentDifference(File file) {
        double min = minimum(file);
        double max = maximum(file);
        double percent = ((max - min) / min) * 100.0;

        return percent;
    }

    //Write Statistics to file.
    public void setStatistics(File file, String name) {
        String fileName = "Statistics_for_" + name + "_in_" + time_Unit;
        write(name, fileName, "Minimum : " + decimalFormat.format(minimum(file)));
        write(name, fileName, "Maximum : " + decimalFormat.format(maximum(file)));
        write(name, fileName, "Average : " + decimalFormat.format(average(file)));
        write(name, fileName, "Variance : " + decimalFormat.format(variance(file)));
        write(name, fileName, "stdDev : " + decimalFormat.format(stdDev(file)));
        write(name, fileName, "MinMax difference : " + decimalFormat.format(minMaxPercentDifference(file)) + "%");
    }
}
