package io.realm.examples.performance.sqlite;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import io.realm.examples.performance.PerformanceTest;
import io.realm.examples.performance.PerformanceTestException;
import io.realm.examples.performance.greendao.Employee;

public class SQLiteTests extends PerformanceTest {

    private EmployeeDatabaseHelper databaseHelper = null;
    private android.database.sqlite.SQLiteDatabase db = null;

    public SQLiteTests() {
        testName = "SQLite";
    }

    public void clearDatabase() throws PerformanceTestException {
        databaseHelper.onUpgrade(databaseHelper.getWritableDatabase(), 2, 3);
    }

    public void testBootstrap() throws PerformanceTestException {
        databaseHelper = new EmployeeDatabaseHelper(getActivity());
        db = databaseHelper.getWritableDatabase();
        databaseHelper.onCreate(db);
    }

    public void testInsertPerTransaction() throws PerformanceTestException {
        ContentValues values = new ContentValues();
        for (int row = 0; row < getNumInserts(); row++) {
            db.beginTransaction();
                values.put(databaseHelper.COLUMN_NAME, getEmployeeName(row));
                values.put(databaseHelper.COLUMN_AGE, getEmployeeAge(row));
                values.put(databaseHelper.COLUMN_HIRED, getEmployeeHiredStatus(row));
                db.insert(databaseHelper.TABLE_EMPLOYEES, null, values);
            db.setTransactionSuccessful();
            db.endTransaction();
        }

        //Verify writes were successful
        String query = "SELECT * FROM " + EmployeeDatabaseHelper.TABLE_EMPLOYEES;
        Cursor cursor = db.rawQuery(query, null);

        if(cursor.getCount() < getNumInserts()) {
            throw new PerformanceTestException("SQLite failed to insert all of the records");
        }

        db.close();
    }

    public void testBatchInserts() throws PerformanceTestException {
        SQLiteStatement stmt = db.compileStatement("INSERT INTO "
                + EmployeeDatabaseHelper.TABLE_EMPLOYEES + " VALUES(?1, ?2, ?3, ?4)");

        db.beginTransaction();
        for (int row = 0; row < getNumInserts(); row++) {
            stmt.clearBindings();
            stmt.bindString(2, getEmployeeName(row));
            stmt.bindLong(3, getEmployeeAge(row));
            stmt.bindLong(4, getEmployeeHiredStatus(row));
            stmt.executeInsert();
        }
        db.setTransactionSuccessful();
        db.endTransaction();

//        ContentValues values = new ContentValues();
//        db.beginTransaction();
//        for (int row = 0; row < getNumInserts(); row++) {
//            values.put(databaseHelper.COLUMN_NAME, getEmployeeName(row));
//            values.put(databaseHelper.COLUMN_AGE, getEmployeeAge(row));
//            values.put(databaseHelper.COLUMN_HIRED, getEmployeeHiredStatus(row));
//            db.insert(databaseHelper.TABLE_EMPLOYEES, null, values);
//        }
//        db.setTransactionSuccessful();
//        db.endTransaction();

        //Verify writes were successful
        String query = "SELECT * FROM " + EmployeeDatabaseHelper.TABLE_EMPLOYEES;
        Cursor cursor = db.rawQuery(query, null);

        if(cursor.getCount() < getNumInserts()) {
            throw new PerformanceTestException("SQLite failed to insert all of the records");
        }

        db.close();
    }

    public void testQueries() throws PerformanceTestException {
        android.database.sqlite.SQLiteDatabase db = databaseHelper.getWritableDatabase();

        //Skip the first as a "warmup"
        String query = QUERY1;
        Cursor cursor = db.rawQuery(query, null);
        loopCursor(cursor);
        cursor.close();

        long startTime = System.currentTimeMillis();

        query = QUERY2;
        cursor = db.rawQuery(query, null);
        loopCursor(cursor);
        cursor.close();

        query = QUERY3;
        cursor = db.rawQuery(query, null);
        loopCursor(cursor);
        cursor.close();

        query = QUERY4;
        cursor = db.rawQuery(query, null);
        loopCursor(cursor);
        cursor.close();

        query = QUERY5;
        cursor = db.rawQuery(query, null);
        loopCursor(cursor);
        cursor.close();
        db.close();
    }

    private void loopCursor(Cursor cursor) {
        cursor.moveToFirst();
        while (cursor.isAfterLast() == false) {
            cursor.moveToNext();
        }
    }

    public void testCounts() throws PerformanceTestException {
        android.database.sqlite.SQLiteDatabase db = databaseHelper.getWritableDatabase();

        Cursor cursor = db.rawQuery(COUNT_QUERY1, null);
        cursor.moveToFirst();
        cursor.close();

        cursor = db.rawQuery(COUNT_QUERY2, null);
        cursor.moveToFirst();
        cursor.close();

        cursor = db.rawQuery(COUNT_QUERY3, null);
        cursor.moveToFirst();
        cursor.close();

        cursor = db.rawQuery(COUNT_QUERY4, null);
        cursor.moveToFirst();
        cursor.close();

        cursor = db.rawQuery(COUNT_QUERY5, null);
        cursor.moveToFirst();
        cursor.close();
        db.close();
    }
}
