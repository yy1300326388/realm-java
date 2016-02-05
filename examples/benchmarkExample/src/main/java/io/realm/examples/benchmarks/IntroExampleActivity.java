package io.realm.examples.benchmarks;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import dk.ilios.spanner.Spanner;
import dk.ilios.spanner.SpannerCallbackAdapter;
import dk.ilios.spanner.internal.InvalidBenchmarkException;
import dk.ilios.spanner.model.Trial;

public class IntroExampleActivity extends Activity {

    private String TAG = IntroExampleActivity.class.getName();
    private LinearLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realm_basic_example);
        rootLayout = ((LinearLayout) findViewById(R.id.container));
        rootLayout.removeAllViews();
        try {
            startBenchmark();
        } catch (InvalidBenchmarkException e) {
            throw new RuntimeException(e);
        }
    }

    private void startBenchmark() throws InvalidBenchmarkException {
        Spanner.runAllBenchmarks(Benchmarks.class, new SpannerCallbackAdapter() {
            @Override
            public void trialStarted(Trial trial) {
                addStatus("Start: " + getDescription(trial));
            }

            @Override
            public void trialSuccess(Trial trial, Trial.Result result) {
                double baselineFailure = 15; //benchmarkConfiguration.getBaselineFailure()
                if (trial.hasBaseline()) {
                    double absChange = Math.abs(trial.getChangeFromBaseline(50));
                    if (absChange > baselineFailure) {
                        addStatus(String.format("Change from baseline was to big: %.2f%%. Limit is %.2f%%",
                                absChange, baselineFailure));
                    }
                } else {
                    String resultString = String.format(" [%.2f ns.]", trial.getMedian());
                    addStatus(getDescription(trial) + resultString);
                }
            }

            @Override
            public void trialFailure(Trial trial, Throwable error) {
                addStatus(error.getMessage());
            }

            @Override
            public void onComplete() {
                addStatus("Benchmarks completed");
            }

            @Override
            public void onError(Exception error) {
                addStatus(error.getMessage());
            }
        });
    }

    private String getDescription(Trial trial) {
        return trial.experiment().instrumentation().benchmarkMethod().getName();
    }

    private void addStatus(String txt) {
        Log.i(TAG, txt);
        TextView tv = new TextView(this);
        tv.setText(txt);
        rootLayout.addView(tv);
    }
}