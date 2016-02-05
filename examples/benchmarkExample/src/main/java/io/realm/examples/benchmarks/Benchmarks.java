package io.realm.examples.benchmarks;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dk.ilios.spanner.AfterExperiment;
import dk.ilios.spanner.BeforeExperiment;
import dk.ilios.spanner.Benchmark;
import dk.ilios.spanner.BenchmarkConfiguration;
import dk.ilios.spanner.Param;
import dk.ilios.spanner.SpannerConfig;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.examples.benchmarks.model.AllTypes;
import io.realm.examples.benchmarks.model.Dog;

public class Benchmarks {

    private File filesDir = MyApplication.getContext().getFilesDir();
    private File resultsDir = new File(filesDir, "results");
//    private File baseLineFile = Utils.copyFromAssets("baseline.json");

    @BenchmarkConfiguration
    public SpannerConfig configuration = new SpannerConfig.Builder()
            .saveResults(resultsDir, Benchmarks.class.getCanonicalName() + ".json")
//            .useBaseline(baseLineFile)
            .medianFailureLimit(1.0f) // Accept 100% difference, normally should be 10-15%
//            .uploadResults()
            .build();

    // Public test parameters (value chosen and injected by Experiment)
    @Param(value = {"java.util.Date", "java.lang.Object"})
    public String value;

    // Private fields used by benchmark methods
    private Class testClass;
    private List<AllTypes> list;
    private Realm realm;

    @BeforeExperiment
    public void before() {
        RealmConfiguration config = new RealmConfiguration.Builder(MyApplication.getContext()).build();
        Realm.deleteRealm(config);
        realm = Realm.getInstance(config);
        list = new ArrayList();
        for (int i = 0; i < 10; i++) {
            AllTypes allTypes = new AllTypes();
            allTypes.setColumnBoolean((i % 3) == 0);
            allTypes.setColumnBinary(new byte[]{1, 2, 3});
            allTypes.setColumnDate(new Date(1000));
            allTypes.setColumnDouble(3.1415);
            allTypes.setColumnFloat(1.234567f + i);
            allTypes.setColumnString("test data " + i);
            allTypes.setColumnLong(i);

            RealmList<Dog> dogs = new RealmList<>(new Dog("White"), new Dog("Black"));
            allTypes.setColumnRealmObject(dogs.get(i));
            allTypes.setColumnRealmList(dogs);
        }


        try {
            testClass = Class.forName(value);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterExperiment
    public void after() {

    }

    @Benchmark
    public void copyToRealm(int reps) {
        for (int i = 0; i < reps; i++) {
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(list);
            realm.commitTransaction();
        }
    }
}