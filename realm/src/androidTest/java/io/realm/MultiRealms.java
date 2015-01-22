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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import io.realm.entities.AllTypes;

public class MultiRealms extends AndroidTestCase {

    private DecimalFormat decimalFormat = new DecimalFormat("#.##");

    @Override
    protected void setUp() throws Exception {
        Realm.deleteRealmFile(getContext());
        Realm.deleteRealmFile(getContext(), "default1.realm");
        Realm.deleteRealmFile(getContext(), "default2.realm");
        Realm.deleteRealmFile(getContext(), "default3.realm");
    }

    public void addObjectToRealm(int objects, Realm realm) {
        realm.beginTransaction();
        for (int i = 0; i < objects; ++i) {
            AllTypes allTypes = realm.createObject(AllTypes.class);
            allTypes.setColumnBoolean((i % 2) == 0);
            allTypes.setColumnDouble(1.337);
            allTypes.setColumnFloat(0.1337f + i);
            allTypes.setColumnString("dummy data " + i);
            allTypes.setColumnLong(i);
        }
        realm.commitTransaction();
    }

    public void testRealms() {

        Thread thread1 = new Thread() {
            public void run() {
                Realm realm1 = Realm.getInstance(getContext(), "default1.realm");
                addObjectToRealm(50, realm1);
                File file = createFile("realm1");
                for (int i = 0; i < 250; i++) {
                    for (int j = 0; j < 50; j++) {
                        long start = System.nanoTime();
                        realm1.beginTransaction();
                        realm1.allObjects(AllTypes.class).get(j).setColumnBoolean(false);
                        realm1.allObjects(AllTypes.class).get(j).setColumnDouble(23551.222 + i);
                        realm1.allObjects(AllTypes.class).get(j).setColumnFloat(10000.256f + i);
                        realm1.allObjects(AllTypes.class).get(j).setColumnString("new dummy data " + i);
                        realm1.allObjects(AllTypes.class).get(j).setColumnLong(i + i);
                        realm1.commitTransaction();
                        long stop = System.nanoTime();
                        double mSec = ((double) (stop - start) / 1000000.0);
                        write(file, decimalFormat.format(mSec));
                    }
                }
                realm1.close();
            }
        };

        Thread thread2 = new Thread() {
            public void run() {
                Realm realm2 = Realm.getInstance(getContext(), "default2.realm");
                addObjectToRealm(50, realm2);
                File file = createFile("realm2");
                for (int i = 0; i < 250; i++) {
                    for (int j = 0; j < 50; j++) {
                        long start = System.nanoTime();
                        realm2.beginTransaction();
                        realm2.allObjects(AllTypes.class).get(j).setColumnBoolean(true);
                        realm2.allObjects(AllTypes.class).get(j).setColumnDouble(1.222 + i + j);
                        realm2.allObjects(AllTypes.class).get(j).setColumnFloat(1.256f + i + j);
                        realm2.allObjects(AllTypes.class).get(j).setColumnString("new dummy data " + i + 10000);
                        realm2.allObjects(AllTypes.class).get(j).setColumnLong(j + i);
                        realm2.commitTransaction();
                        long stop = System.nanoTime();
                        double mSec = ((double) (stop - start) / 1000000.0);
                        write(file, decimalFormat.format(mSec));
                    }
                }
                realm2.close();
            }
        };

        Thread thread3 = new Thread() {
            public void run() {
                Realm realm2 = Realm.getInstance(getContext(), "default3.realm");
                addObjectToRealm(50, realm2);
                File file = createFile("realm3");
                for (int i = 0; i < 500; i++) {
                    long start = System.nanoTime();
                    realm2.beginTransaction();
                    realm2.allObjects(AllTypes.class).get(1).setColumnString("new dummy data " + i);
                    realm2.commitTransaction();
                    long stop = System.nanoTime();
                    double mSec = ((double) (stop - start) / 1000000.0);
                    write(file, decimalFormat.format(mSec));
                }
                realm2.close();
            }
        };

        Thread thread4 = new Thread() {
            public void run() {
                ArrayList<Double> times = new ArrayList<Double>();
                File dummy_file = createFile("dummy");
                File file = createFile("normal_write");
                for (int i = 0; i < 12500; i++) {
                    long start = System.nanoTime();
                    writeNoClose(dummy_file, String.valueOf(i));
                    long stop = System.nanoTime();
                    double mSec = ((double) (stop - start) / 1000000.0);
                    times.add(i, mSec);
                }
                for (int j = 0; j < times.size(); j++) {
                    write(file, decimalFormat.format(times.get(j)));
                }
            }
        };

        Thread thread5 = new Thread() {
            public void run() {
                ArrayList<Double> times = new ArrayList<Double>();
                File dummy_file = createFile("dummy2");
                File file = createFile("normal_write_close");
                for (int i = 0; i < 12500; i++) {
                    long start = System.nanoTime();
                    write(dummy_file, String.valueOf(i));
                    long stop = System.nanoTime();
                    double mSec = ((double) (stop - start) / 1000000.0);
                    write(file, decimalFormat.format(mSec));
                }
            }
        };

        //thread1.start();
        //thread2.start();
        //thread3.start();
        //thread4.start();
        thread5.start();

        try {
            thread1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            thread4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            thread5.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public File createFile(String fileName) {
        deleteFile(fileName);
        try {
            String file_path = getContext().getFilesDir().getAbsolutePath() + "/" + fileName + ".dat";

            File file = new File(file_path);
            file.createNewFile();
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("##;##\n" +
                    "@LiveGraph demo file.\n" +
                    "Time");
            bw.newLine();
            bw.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();

        }
        return null;
    }

    public void deleteFile(String fileName) {
        String file_path = getContext().getFilesDir().getAbsolutePath() + "/" + fileName + ".dat";
        File file = new File(file_path);
        file.delete();
    }

    public void writeNoClose(File file, String content) {
        try {
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw, 1024 * 1024);
            bw.write(content);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(File file, String content) {
        try {
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

