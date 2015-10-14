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

package io.realm.examples.arm64stress;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.examples.arm64stress.model.Place;


public class MainActivity extends Activity implements WatchDog.Watchable {

    private static final long BACKGROUND_SLEEP_MS = 10;

    Random random = new Random();
    private AtomicLong uiProgress = new AtomicLong(0);
    private AtomicLong backgroundProgress = new AtomicLong(0);
    private Realm realm;
    private boolean isStarted = false;
    private TextView statusView;
    private RealmConfiguration config;
    private RealmChangeListener changeListener;

    public String name; // Make sure that value is read.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusView = (TextView) findViewById(R.id.text_status);

        View button = findViewById(R.id.button_start);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTest();
            }
        });

        // Setup Realm
        byte[] encryptionKey = new byte[64];
        random.nextBytes(encryptionKey);
        config = new RealmConfiguration.Builder(this).encryptionKey(encryptionKey).build();
        Realm.deleteRealm(config);
        realm = Realm.getInstance(config);

        // Setup touch area (to flood with manual touch events
        final View touchAreaView = findViewById(R.id.touch_area);
        touchAreaView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                touchAreaView.setBackgroundColor(random.nextInt());
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    private void startTest() {
        WatchDog watchDog = new WatchDog();

        Thread backgroundWriter = new Thread(new Runnable() {
            @Override
            public void run() {
                Realm realm = Realm.getInstance(config);
                while(!Thread.interrupted()) {
                    realm.beginTransaction();
                    Place place1 = realm.createObject(Place.class);
                    place1.setName(null);
                    Place place2 = realm.createObject(Place.class);
                    place2.setName("Place" + backgroundProgress);
                    realm.commitTransaction();
                    backgroundProgress.set(realm.where(Place.class).isNotNull("name").count());
                    SystemClock.sleep(BACKGROUND_SLEEP_MS);
                }
                realm.close();
            }
        });

        // Avoid GC -> I hate weak references :(
        changeListener = new RealmChangeListener() {
            @Override
            public void onChange() {
                if (!isStarted) return;
                RealmResults<Place> result = realm.where(Place.class).isNotNull("name").findAll();
                for (Place place : result) {
                    name = place.getName();
                }
                uiProgress.set(result.size());
            }
        };
        realm.addChangeListener(changeListener);

        watchDog.setWatchable(this);
        backgroundWriter.start();
        isStarted = true;
        watchDog.start();
    };

    private void stopTest() {

    }

    @Override
    public long getUIProgress() {
        return uiProgress.get();
    }

    @Override
    public long getBackgroundProgress() {
        return backgroundProgress.get();
    }

    @Override
    public void notifyUpdate(final long uiProgress, final long backgroundProgress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusView.setText(String.format("UI: %s, Background: %s", uiProgress, backgroundProgress));
            }
        });
    }
}
