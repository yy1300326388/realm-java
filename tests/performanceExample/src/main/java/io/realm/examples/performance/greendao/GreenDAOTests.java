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
    private EmployeeDao employeeDao;

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
        employeeDao = daoSession.getEmployeeDao();
        employeeDao.createTable(db, true);
        Employee e = new Employee();
        employeeDao.insert(e);
        employeeDao.delete(e);

        //Skip the first as a "warmup"
        String query = "WHERE name = 'Foo0' " +
                "AND age >= 20 AND age <= 50 " +
                "AND hired = 0";
        List<Employee> list = employeeDao.queryRaw(query, null);
        list.size();
    }

    public void testInsertPerTransaction() throws PerformanceTestException {
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();

        for (int row = 0; row < getNumInserts(); row++) {
            Employee employee = new Employee();
            employee.setName(getEmployeeName(row));
            employee.setAge(getEmployeeAge(row));
            employee.setHired(getHiredBool(row));
            employeeDao.insert(employee);
        }
    }

    public void verifyInserts() throws PerformanceTestException {
        //Verify writes were successful
        List<Employee> list = employeeDao.loadAll();

        if(list.size() < getNumInserts()) {
            throw new PerformanceTestException("GreenDAO failed to insert all of the records");
        }

        db.close();
    }

    public void testBatchInserts() throws PerformanceTestException {
        //To do batch on GreenDAO it is generally easier to use sqlite batch controls.

        db.beginTransaction();
        try {
            for (int row = 0; row < getNumInserts(); row++) {
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
    }

    public void testQueries() throws PerformanceTestException {
        String query;

        query = "WHERE name = 'Foo1' " +
                "AND age >= 20 AND age <= 50 " +
                "AND hired = 1";
        List<Employee> list = employeeDao.queryRaw(query, null);
        loopResults(list);

        query = "WHERE name = 'Foo3' " +
                "AND age >= 20 AND age <= 50 " +
                "AND hired = 1";
        list = employeeDao.queryRaw(query, null);
        loopResults(list);

        query = "WHERE name = 'Foo2' " +
                "AND age >= 20 AND age <= 50 " +
                "AND hired = 0";
        list = employeeDao.queryRaw(query, null);
        loopResults(list);

        query = "WHERE name = 'Foo330' " +
                "AND age >= 20 AND age <= 50 " +
                "AND hired = 0";
        list = employeeDao.queryRaw(query, null);
        loopResults(list);

        db.close();
    }

    private void loopResults(List<Employee> list) throws PerformanceTestException{
        int iterations = 0;
        for(Employee e : list) {
            e.getHired();
            iterations++;
        }
        if(iterations < getNumInserts()) {
            throw new PerformanceTestException("GreenDAO does not complete the iterations over the queried results");
        }
    }

    public void testCounts() throws PerformanceTestException {
        String query;

        query = "WHERE name = 'Foo1' " +
                "AND age >= 20 AND age <= 50 " +
                "AND hired = 1";
        List<Employee> list = employeeDao.queryRaw(query, null);
        list.size();

        query = "WHERE name = 'Foo3' " +
                "AND age >= 20 AND age <= 50 " +
                "AND hired = 1";
        list = employeeDao.queryRaw(query, null);
        list.size();

        query = "WHERE name = 'Foo2' " +
                "AND age >= 20 AND age <= 50 " +
                "AND hired = 0";
        list = employeeDao.queryRaw(query, null);
        list.size();

        query = "WHERE name = 'Foo330' " +
                "AND age >= 20 AND age <= 50 " +
                "AND hired = 0";
        list = employeeDao.queryRaw(query, null);
        list.size();

        db.close();
    }

}
