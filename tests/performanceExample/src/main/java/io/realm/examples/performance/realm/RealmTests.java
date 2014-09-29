package io.realm.examples.performance.realm;

import java.io.File;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.examples.performance.PerformanceTest;
import io.realm.examples.performance.PerformanceTestException;

public class RealmTests extends PerformanceTest {

    private Realm realm = null;

    public RealmTests() {
        testName = "Realm";
    }

    public void clearDatabase() throws PerformanceTestException {
        realm = Realm.getInstance(getActivity());

        //Clear the Realm...
        File file = new File("files/default.realm");
        file.delete();
        file = new File("files/default.realm.lock");
        file.delete();
    }

    public void testBootstrap() throws PerformanceTestException {
        realm = Realm.getInstance(getActivity());
        realm.beginTransaction();
        realm.createObject(RealmEmployee.class);
        realm.commitTransaction();
        //Throw away first query
        List<RealmEmployee> results
                = realm.where(RealmEmployee.class)
                .equalTo("hired", 0)
                .between("age", 20, 50)
                .equalTo("name", "Foo0").findAll();
    }

    public void testInsertPerTransaction() throws PerformanceTestException {
        for (int row = 0; row < getNumInserts(); row++) {
            realm.beginTransaction();
            RealmEmployee employee = realm.createObject(RealmEmployee.class);
            employee.setName(getEmployeeName(row));
            employee.setAge(getEmployeeAge(row));
            employee.setHired(getEmployeeHiredStatus(row));
            realm.commitTransaction();
        }
    }

    public void verifyInserts() throws PerformanceTestException {
        //Verify writes were successful
        RealmResults<RealmEmployee> results = realm.where(RealmEmployee.class).findAll();

        if(results.size() < getNumInserts()) {
            throw new PerformanceTestException("Realm failed to insert all of the records");
        }
    }

    public void testBatchInserts() throws PerformanceTestException {
        realm.beginTransaction();
        for (int row = 0; row < getNumInserts(); row++) {
            RealmEmployee employee = realm.createObject(RealmEmployee.class);
            employee.setName(getEmployeeName(row));
            employee.setAge(getEmployeeAge(row));
            employee.setHired(getEmployeeHiredStatus(row));
        }
        realm.commitTransaction();
    }

    public void testQueries() throws PerformanceTestException {
        List<RealmEmployee> results
                = realm.where(RealmEmployee.class)
                .equalTo("hired", 1)
                .between("age", 20, 50)
                .equalTo("name", "Foo1").findAll();
        loopResults(results);

        results = realm.where(RealmEmployee.class)
                .equalTo("hired", 1)
                .between("age", 20, 50)
                .equalTo("name", "Foo3").findAll();
        loopResults(results);

        results = realm.where(RealmEmployee.class)
                .equalTo("hired", 0)
                .between("age", 20, 50)
                .equalTo("name", "Foo2").findAll();
        loopResults(results);

        results = realm.where(RealmEmployee.class)
                .equalTo("hired", 0)
                .between("age", 20, 50)
                .equalTo("name", "Foo330").findAll();
        loopResults(results);
    }

    private void loopResults(List<RealmEmployee> results) throws PerformanceTestException{
        int iterations = 0;
        for (RealmEmployee e : results) {
            e.getHired();
            iterations++;
        }
        if(iterations < getNumInserts()) {
            throw new PerformanceTestException("Realm does not complete the iterations over the queried results");
        }
    }

    public void testCounts() throws PerformanceTestException {
        List<RealmEmployee> results
                = realm.where(RealmEmployee.class)
                .equalTo("hired", 1)
                .between("age", 20, 50)
                .equalTo("name", "Foo1").findAll();
        results.size();
        results = realm.where(RealmEmployee.class)
                .equalTo("hired", 1)
                .between("age", 20, 50)
                .equalTo("name", "Foo3").findAll();
        results.size();
        results = realm.where(RealmEmployee.class)
                .equalTo("hired", 0)
                .between("age", 20, 50)
                .equalTo("name", "Foo2").findAll();
        results.size();
        results = realm.where(RealmEmployee.class)
                .equalTo("hired", 0)
                .between("age", 20, 50)
                .equalTo("name", "Foo330").findAll();
        results.size();
    }

}
