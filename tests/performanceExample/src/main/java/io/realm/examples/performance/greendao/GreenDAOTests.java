package io.realm.examples.performance.greendao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import io.realm.examples.performance.PerformanceTest;
import io.realm.examples.performance.PerformanceTestException;
import io.realm.examples.performance.sqlite.EmployeeDatabaseHelper;

public class GreenDAOTests extends PerformanceTest {

    private SQLiteDatabase db;
    private DaoMaster daoMaster;
    private DaoSession daoSession;

    public GreenDAOTests() {
        testName = "GreenDAO";
    }

    public void clearDatabase() throws PerformanceTestException {
        DaoMaster.DevOpenHelper helper
                = new DaoMaster.DevOpenHelper(getActivity(), "EmployeeGreenDAO.db", null);
        db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        EmployeeDao employeeDao = daoSession.getEmployeeDao();
        employeeDao.dropTable(db, true);
    }

    public void testBootstrap() throws PerformanceTestException {
        DaoMaster.DevOpenHelper helper
                = new DaoMaster.DevOpenHelper(getActivity(), "EmployeeGreenDAO.db", null);
        db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        EmployeeDao employeeDao = daoSession.getEmployeeDao();
        employeeDao.createTable(db, true);
        Employee e = new Employee();
        employeeDao.insert(e);
        employeeDao.delete(e);

        //Skip the first as a "warmup"
        String query = QUERY1;
        Cursor cursor = db.rawQuery(query, null);
        cursor.getCount();
        cursor.close();
    }

    public void testInsertPerTransaction() throws PerformanceTestException {
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        EmployeeDao employeeDao = daoSession.getEmployeeDao();

        for(int row=0; row < getNumInserts(); row++) {
            Employee employee = new Employee();
            employee.setName(getEmployeeName(row));
            employee.setAge(getEmployeeAge(row));
            employee.setHired(getHiredBool(row));
            employeeDao.insert(employee);
        }

        Cursor cursor = db.query(employeeDao.getTablename(),
                employeeDao.getAllColumns(), null, null, null, null, null);
        cursor.getCount();

        if(cursor.getCount() < getNumInserts()) {
            throw new PerformanceTestException("GreenDAO failed to insert all of the records");
        }

        db.close();
    }

    public void testBatchInserts() throws PerformanceTestException {
        EmployeeDao employeeDao = daoSession.getEmployeeDao();
        //To do batch on GreenDAO it is generally easier to use sqlite batch controls.
        db.beginTransaction();
        try {
            for (int row = 0; row < 100000; row++) {
                Employee employee = new Employee();
                employee.setName(getEmployeeName(row));
                employee.setAge(getEmployeeAge(row));
                employee.setHired(getHiredBool(row));
                employeeDao.insert(employee);
            }
            db.setTransactionSuccessful();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        //Verify writes were successful
        String query = "SELECT * FROM " + EmployeeDatabaseHelper.TABLE_EMPLOYEES;
        Cursor cursor = db.rawQuery(query, null);

        if(cursor.getCount() < getNumInserts()) {
            throw new PerformanceTestException("GreenDAO failed to insert all of the records");
        }

        db.close();
    }

    public void testQueries() throws PerformanceTestException {
        long startTime = System.currentTimeMillis();
        String query;
        Cursor cursor;

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
        String query;
        Cursor cursor;

        query = QUERY2;
        cursor = db.rawQuery(query, null);
        cursor.getCount();
        cursor.close();

        query = QUERY3;
        cursor = db.rawQuery(query, null);
        cursor.getCount();
        cursor.close();

        query = QUERY4;
        cursor = db.rawQuery(query, null);
        cursor.getCount();
        cursor.close();

        query = QUERY5;
        cursor = db.rawQuery(query, null);
        loopCursor(cursor);
        cursor.getCount();
        db.close();
    }

}
