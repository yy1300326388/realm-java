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

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import io.realm.entities.AllTypes;
import io.realm.entities.MySQLiteHelper;
import io.realm.entities.StringOnly;

public class MultiRealms extends AndroidTestCase {

    private DecimalFormat decimalFormat = new DecimalFormat("#.##");
    //decides where to save .dat file. true if emulator or can access data/data, false if sd card.
    private final Boolean isInternal = true;

    @Override
    protected void setUp() throws Exception {
        Realm.deleteRealmFile(getContext());
        Realm.deleteRealmFile(getContext(), "default1.realm");
        Realm.deleteRealmFile(getContext(), "default2.realm");
        Realm.deleteRealmFile(getContext(), "default3.realm");
    }

    public void addObjectsToRealmStringOnly(int objects, Realm realm) {
        realm.beginTransaction();
        for (int i = 0; i < objects; i++) {
            StringOnly stringOnly = realm.createObject(StringOnly.class);
            stringOnly.setChars(String.valueOf(i));
        }
        realm.commitTransaction();
    }

    public void addObjectsToRealm(int objects, Realm realm) {
        realm.beginTransaction();
        for (int i = 0; i < objects; ++i) {
            AllTypes allTypes = realm.createObject(AllTypes.class);
            allTypes.setColumnBoolean((i % 2) == 0);
            allTypes.setColumnDouble(1.337);
            allTypes.setColumnFloat(0.1337f + i);
            allTypes.setColumnString("data " + i);
            allTypes.setColumnLong(i);
        }
        realm.commitTransaction();
    }

    public void insertSQLite(int objects, SQLiteDatabase db) {
        for (int i = 0; i < objects; i++) {
            db.execSQL("INSERT INTO "
                    + "test"
                    + " (string, integer)"
                    + " VALUES ('" + i + "', " + i + ")");
        }
    }

/*    public void testSystem_ns() {

        List<Long> list = new ArrayList<Long>();
        File file = createFile("write", isInternal);
        for (int i = 0; i < 12500; i++) {
            long start = System.nanoTime();
            long stop = System.nanoTime();
            long ns = stop - start;
            list.add(i, ns);
        }
        for (int i = 0; i < list.size(); i++) {
            write(file, decimalFormat.format(list.get(i)));
        }
    }*/

    public void testIO() {
/*
        final Thread thread_a = new Thread() {
            public void run() {
                Realm realm1 = Realm.getInstance(getContext(), "default1.realm");
                addObjectToRealm(50, realm1);
                File file = createFile("realm1", isInternal);
                for (int i = 0; i < 250; i++) {
                    for (int j = 0; j < 50; j++) {
                        long start = System.nanoTime();
                        realm1.beginTransaction();
                        realm1.allObjects(AllTypes.class).get(j).setColumnBoolean(false);
                        realm1.allObjects(AllTypes.class).get(j).setColumnDouble(23551.222 + i);
                        realm1.allObjects(AllTypes.class).get(j).setColumnFloat(10000.256f + i);
                        realm1.allObjects(AllTypes.class).get(j).setColumnString("new data " + i);
                        realm1.allObjects(AllTypes.class).get(j).setColumnLong(i + i);
                        realm1.commitTransaction();
                        long stop = System.nanoTime();
                        double ms = ((double) (stop - start) / 1000000.0);
                        write(file, decimalFormat.format(ms));
                    }
                }
                realm1.close();
            }
        };

        final Thread thread_b = new Thread() {
            public void run() {
                Realm realm2 = Realm.getInstance(getContext(), "default2.realm");
                addObjectToRealm(50, realm2);
                File file = createFile("realm2", isInternal);
                for (int i = 0; i < 250; i++) {
                    for (int j = 0; j < 50; j++) {
                        long start = System.nanoTime();
                        realm2.beginTransaction();
                        realm2.allObjects(AllTypes.class).get(j).setColumnBoolean(true);
                        realm2.allObjects(AllTypes.class).get(j).setColumnDouble(1.222 + i + j);
                        realm2.allObjects(AllTypes.class).get(j).setColumnFloat(1.256f + i + j);
                        realm2.allObjects(AllTypes.class).get(j).setColumnString("new data " + i + 10000);
                        realm2.allObjects(AllTypes.class).get(j).setColumnLong(j + i);
                        realm2.commitTransaction();
                        long stop = System.nanoTime();
                        double ms = ((double) (stop - start) / 1000000.0);
                        write(file, decimalFormat.format(ms));
                    }
                }
                realm2.close();
            }
        };*/

        final Thread thread1 = new Thread() {
            public void run() {
                Realm realm3 = Realm.getInstance(getContext(), "default3.realm");
                addObjectsToRealm(12500, realm3);
                File file = createFile("realm", isInternal);
                for (int i = 0; i < 12500; i++) {
                    long start = System.nanoTime();
                    Log.i("TIMER : ", "START " + i);
                    realm3.beginTransaction();
                    realm3.allObjects(AllTypes.class).get(i).setColumnString("new data " + i);
                    realm3.commitTransaction();
                    long stop = System.nanoTime();
                    Log.i("TIMER : ", "STOP");
                    double ms = ((double) (stop - start) / 1000000.0);
                    write(file, decimalFormat.format(ms));
                }
                realm3.close();
            }
        };


        final Thread thread2 = new Thread() {
            public void run() {
                ArrayList<Double> times = new ArrayList<Double>();
                File dummy_file = createFile("dummy", true);
                File file = createFile("normal_write_no_close", isInternal);
                for (int i = 0; i < 12500; i++) {
                    long start = System.nanoTime();
                    writeNoClose(dummy_file, String.valueOf(i));
                    long stop = System.nanoTime();
                    double ms = ((double) (stop - start) / 1000000.0);
                    times.add(i, ms);
                }
                for (int j = 0; j < times.size(); j++) {
                    write(file, decimalFormat.format(times.get(j)));
                }
            }
        };

        final Thread thread3 = new Thread() {
            public void run() {
                MySQLiteHelper mySQLiteHelper;
                mySQLiteHelper = new MySQLiteHelper(getContext());
                SQLiteDatabase db = mySQLiteHelper.getWritableDatabase();
                mySQLiteHelper.dropTable(db);
                mySQLiteHelper.onCreate(db);
                insertSQLite(12500, db);
                File file = createFile("SQLite", isInternal);
                for (int i = 0; i < 12500; i++) {
                    long start = System.nanoTime();
                    db.execSQL("UPDATE test SET string = 'new data " + i + "' WHERE string = '" + i + "';");
                    long stop = System.nanoTime();
                    double ms = ((double) (stop - start) / 1000000.0);
                    write(file, decimalFormat.format(ms));
                }
                db.close();
            }
        };

        Thread thread4 = new Thread() {
            public void run() {
                File dummy_file = createFile("dummy2", true);
                File file = createFile("normal_write", isInternal);
                //used for testing this alone
                for (int i = 0; i < 10000; i++) {
                    //int i = 0;
                    //running while another thread is running
                    //while (thread3.isAlive() || thread5.isAlive()) {
                    long start = System.nanoTime();
                    Log.i("TIMER : ", "START " + i);
                    write(dummy_file, String.valueOf(i));
                    long stop = System.nanoTime();
                    Log.i("TIMER : ", "STOP");
                    double ms = ((double) (stop - start) / 1000000.0);
                    write(file, decimalFormat.format(ms));
                }
            }
        };

        Thread thread5 = new Thread() {
            public void run() {
                Realm realm4 = Realm.getInstance(getContext(), "default4.realm");
                File file = createFile("realm_insert", isInternal);
                for (int i = 0; i < 12500; i++) {
                    long start = System.nanoTime();
                    realm4.beginTransaction();
                    AllTypes allTypes = realm4.createObject(AllTypes.class);
                    allTypes.setColumnString(String.valueOf(i));
                    realm4.commitTransaction();
                    long stop = System.nanoTime();
                    double ms = ((double) (stop - start) / 1000000.0);
                    write(file, decimalFormat.format(ms));
                }
                realm4.close();
            }
        };

        Thread thread6 = new Thread() {
            public void run() {
                File file = createFile("SQLite_insert", isInternal);
                MySQLiteHelper mySQLiteHelper;
                mySQLiteHelper = new MySQLiteHelper(getContext());
                SQLiteDatabase db = mySQLiteHelper.getWritableDatabase();
                mySQLiteHelper.dropTable(db);
                mySQLiteHelper.onCreate(db);
                for (int i = 0; i < 12500; i++) {
                    long start = System.nanoTime();
                    db.execSQL("INSERT INTO "
                            + "test"
                            + " (string)"
                            + " VALUES ('" + i + "')");

                    long stop = System.nanoTime();
                    double ms = ((double) (stop - start) / 1000000.0);
                    write(file, decimalFormat.format(ms));
                }
                db.close();
            }
        };

        Thread thread7 = new Thread() {
            public void run() {
                List<Double> list = new ArrayList<Double>();
                int a = 5;
                File file = createFile("write", isInternal);
                for (int i = 0; i < 12500; i++) {
                    long start = System.nanoTime();
                    for (int j = 0; j < 500000; j++) {
                        a = a * j;
                    }
                    long stop = System.nanoTime();
                    double ms = ((double) (stop - start) / 1000000.0);
                    list.add(i, ms);

                }
                Log.i("a =", String.valueOf(a));
                for (int i = 0; i < list.size(); i++) {
                    write(file, decimalFormat.format(list.get(i)));
                }
            }
        };

        thread1.start();
        //thread2.start();
        //thread3.start();
        //thread4.start();
        //thread5.start();
        //thread6.start();
        thread7.start();


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
        try {
            thread6.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            thread7.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public File createFile(String fileName, Boolean isInternal) {
        deleteFile(fileName, isInternal);
        try {
            String file_path = "";
            if (isInternal == true) {
                file_path = getContext().getFilesDir().getAbsolutePath() + "/" + fileName + ".dat";
            } else {
                file_path = Environment.getExternalStorageDirectory() + "/" + fileName + ".dat";
            }
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

    public void deleteFile(String fileName, Boolean isInternal) {
        String file_path = "";
        if (isInternal == true) {
            file_path = getContext().getFilesDir().getAbsolutePath() + "/" + fileName + ".dat";
        } else {
            file_path = Environment.getExternalStorageDirectory() + "/" + fileName + ".dat";
        }
        File file = new File(file_path);
        file.delete();
    }

    public void writeNoClose(File file, String content) {
        try {
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw, 256);
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

