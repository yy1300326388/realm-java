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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import io.realm.entities.AllTypes;
import io.realm.entities.MySQLiteHelper;

public class IOTest extends AndroidTestCase {

    private DecimalFormat decimalFormat = new DecimalFormat("#.##");

    //Decides where to save the file. true if internal, false if external.
    //To copy the files open the terminal and type adb pull /where/the/file/is/saved /where/you/want/to/copy/to
    //For sd card do adb pull /storage/emulated/legacy/the_file_name where_to_copy/it
    //For internal memory adb pull /data/data/io.realm.test/files/the_file_name where_to_copy/it
    private final Boolean isInternal = false;

    //set your file type
    private String file_type = "txt";

    @Override
    protected void setUp() throws Exception {
        Realm.deleteRealmFile(getContext());
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

    public void testSystem_ns() {
        long old_stop = 0;
        long frozen = 0;
        File file = createFile("write_test", isInternal);
        for (int i = 0; i < 5000; i++) {
            long start = System.nanoTime();
            long stop = System.nanoTime();
            if (old_stop != start && start != stop && frozen != start) {
                double ms = ((double) (stop - start) / 1000000.0);
                write(file, i + " : " + decimalFormat.format(ms));
                old_stop = stop;
            } else {
                frozen = stop;
                i--;
            }
        }
    }

    public void testIO() {

        final Thread thread = new Thread() {
            public void run() {
                long old_stop = 0;
                long frozen = 0;
                Realm realm = Realm.getInstance(getContext());
                addObjectsToRealm(5000, realm);
                File file = createFile("realm_multi_field_updates", isInternal);
                for (int i = 0; i < 5000; i++) {
                    long start = System.nanoTime();
                    realm.beginTransaction();
                    realm.allObjects(AllTypes.class).get(i).setColumnBoolean(true);
                    realm.allObjects(AllTypes.class).get(i).setColumnDouble(1.222 + i);
                    realm.allObjects(AllTypes.class).get(i).setColumnFloat(1.256f + i);
                    realm.allObjects(AllTypes.class).get(i).setColumnString("new data " + i);
                    realm.allObjects(AllTypes.class).get(i).setColumnLong(i + i);
                    realm.commitTransaction();
                    long stop = System.nanoTime();
                    if (old_stop != start && start != stop && frozen != start) {
                        double ms = ((double) (stop - start) / 1000000.0);
                        write(file, decimalFormat.format(ms));
                        old_stop = stop;
                    } else {
                        frozen = stop;
                        i--;
                    }
                }
                realm.close();
            }
        };

        final Thread thread1 = new Thread() {
            public void run() {
                long old_stop = 0;
                long frozen = 0;
                Realm realm2 = Realm.getInstance(getContext());
                addObjectsToRealm(5000, realm2);
                File file = createFile("realm_single_field_update", isInternal);
                for (int i = 0; i < 5000; i++) {
                    long start = System.nanoTime();
                    realm2.beginTransaction();
                    realm2.allObjects(AllTypes.class).get(i).setColumnString("new data " + i);
                    realm2.commitTransaction();
                    long stop = System.nanoTime();
                    if (old_stop != start && start != stop && frozen != start) {
                        double ms = ((double) (stop - start) / 1000000.0);
                        write(file, decimalFormat.format(ms));
                        old_stop = stop;
                    } else {
                        frozen = stop;
                        i--;
                    }
                }
                realm2.close();
            }
        };

        Thread thread2 = new Thread() {
            public void run() {
                long old_stop = 0;
                long frozen = 0;
                MySQLiteHelper mySQLiteHelper;
                mySQLiteHelper = new MySQLiteHelper(getContext());
                SQLiteDatabase db = mySQLiteHelper.getWritableDatabase();
                mySQLiteHelper.dropTable(db);
                mySQLiteHelper.onCreate(db);
                insertSQLite(5000, db);
                File file = createFile("SQLite_update", isInternal);
                for (int i = 0; i < 5000; i++) {
                    long start = System.nanoTime();
                    db.execSQL("UPDATE test SET string = 'new data " + i + "' WHERE string = '" + i + "';");
                    long stop = System.nanoTime();
                    if (old_stop != start && start != stop && frozen != start) {
                        double ms = ((double) (stop - start) / 1000000.0);
                        write(file, decimalFormat.format(ms));
                        old_stop = stop;
                    } else {
                        frozen = stop;
                        i--;
                    }
                }
                db.close();
            }
        };

        Thread thread3 = new Thread() {
            public void run() {
                long old_stop = 0;
                long frozen = 0;
                ArrayList<Double> times = new ArrayList<Double>();
                File dummy_file = createFile("dummy", true);
                File file = createFile("normal_write_no_close", isInternal);
                for (int i = 0; i < 5000; i++) {
                    long start = System.nanoTime();
                    writeNoClose(dummy_file, String.valueOf(i));
                    long stop = System.nanoTime();
                    if (old_stop != start && start != stop && frozen != start) {
                        double ms = ((double) (stop - start) / 1000000.0);
                        times.add(i, ms);
                        old_stop = stop;
                    } else {
                        frozen = stop;
                        i--;
                    }
                }
                for (int j = 0; j < times.size(); j++) {
                    write(file, decimalFormat.format(times.get(j)));
                }
            }
        };

        Thread thread4 = new Thread() {
            public void run() {
                long old_stop = 0;
                long frozen = 0;
                File dummy_file = createFile("dummy2", true);
                File file = createFile("normal_write", isInternal);
                //used for testing this alone
                //for (int i = 0; i < 5000; i++) {
                //running while another thread is running
                int i = 0;
                while (thread.isAlive()) {
                    long start = System.nanoTime();
                    write(dummy_file, String.valueOf(i));
                    long stop = System.nanoTime();
                    if (old_stop != start && start != stop && frozen != start) {
                        double ms = ((double) (stop - start) / 1000000.0);
                        write(file, decimalFormat.format(ms));
                        old_stop = stop;
                    } else {
                        frozen = stop;
                        i--;
                    }
                    i++;
                }
            }
        };

        Thread thread5 = new Thread() {
            public void run() {
                long old_stop = 0;
                long frozen = 0;
                Realm realm3 = Realm.getInstance(getContext(), "default2.realm");
                File file = createFile("realm_insert", isInternal);
                for (int i = 0; i < 5000; i++) {
                    long start = System.nanoTime();
                    realm3.beginTransaction();
                    AllTypes allTypes = realm3.createObject(AllTypes.class);
                    allTypes.setColumnString(String.valueOf(i));
                    realm3.commitTransaction();
                    long stop = System.nanoTime();
                    if (old_stop != start && start != stop && frozen != start) {
                        double ms = ((double) (stop - start) / 1000000.0);
                        write(file, decimalFormat.format(ms));
                        old_stop = stop;
                    } else {
                        frozen = stop;
                        i--;
                    }
                }
                realm3.close();
            }
        };

        Thread thread6 = new Thread() {
            public void run() {
                long old_stop = 0;
                long frozen = 0;
                File file = createFile("SQLite_insert", isInternal);
                MySQLiteHelper mySQLiteHelper;
                mySQLiteHelper = new MySQLiteHelper(getContext());
                SQLiteDatabase db = mySQLiteHelper.getWritableDatabase();
                mySQLiteHelper.dropTable(db);
                mySQLiteHelper.onCreate(db);
                for (int i = 0; i < 5000; i++) {
                    long start = System.nanoTime();
                    db.execSQL("INSERT INTO "
                            + "test"
                            + " (string)"
                            + " VALUES ('" + i + "')");
                    long stop = System.nanoTime();
                    if (old_stop != start && start != stop && frozen != start) {
                        double ms = ((double) (stop - start) / 1000000.0);
                        write(file, decimalFormat.format(ms));
                        old_stop = stop;
                    } else {
                        frozen = stop;
                        i--;
                    }
                }
                db.close();
            }
        };

        //uncomment the thread/threads you wanna run in this test
        //thread.start();
        //thread1.start();
        //thread2.start();
        //thread3.start();
        //thread4.start();
        //thread5.start();
        //thread6.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    }

    public File createFile(String fileName, Boolean isInternal) {
        deleteFile(fileName, isInternal);
        try {
            String file_path = "";
            if (isInternal == true) {
                file_path = getContext().getFilesDir().getAbsolutePath() + "/" + fileName + "." + file_type;
            } else {
                file_path = Environment.getExternalStorageDirectory() + "/" + fileName + "." + file_type;
            }
            File file = new File(file_path);
            file.createNewFile();
            if (file_type.equals("dat")) {
                FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write("##;##\n" +
                        "@LiveGraph demo file.\n" +
                        "Time");
                bw.newLine();
                bw.close();
            }
            return file;
        } catch (IOException e) {
            e.printStackTrace();

        }
        return null;
    }

    public void deleteFile(String fileName, Boolean isInternal) {
        String file_path = "";
        if (isInternal == true) {
            file_path = getContext().getFilesDir().getAbsolutePath() + "/" + fileName + "." + file_type;
        } else {
            file_path = Environment.getExternalStorageDirectory() + "/" + fileName + "." + file_type;
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

