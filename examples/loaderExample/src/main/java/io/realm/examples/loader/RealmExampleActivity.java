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

package io.realm.examples.loader;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.widget.ListView;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.examples.loader.model.Coffee;

public class RealmExampleActivity extends Activity implements
        LoaderManager.LoaderCallbacks<RealmResults<Coffee>> {

    @SuppressWarnings("UnusedDeclaration")
    public static final String TAG = RealmExampleActivity.class.getName();

    private Realm realm = null;

    private CoffeeAdapter mAdapter = null;
    private ListView mListView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realm_example);

        mListView = (ListView)findViewById(R.id.items_list);

        // Acquire a realm object
        realm = Realm.getInstance(this);

        // Query for items (will be empty first time)
        RealmResults<Coffee> rList = realm.where(Coffee.class).findAll();

        //Initialize adapter even if empty
        mAdapter = new CoffeeAdapter(this, R.layout.simplelistitem, rList);
        mListView.setAdapter(mAdapter);

        // Set up notify for responses on change
        realm.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Launch the loader
        getLoaderManager().restartLoader(0, null, this);
    }

    // Loader callbacks
    @Override
    public Loader<RealmResults<Coffee>> onCreateLoader(int i, Bundle bundle) {
        return new CoffeeLoader(this, null);
    }

    @Override
    public void onLoadFinished(Loader<RealmResults<Coffee>> listLoader, RealmResults<Coffee> coffees) {
        mAdapter.updateRealmResults(coffees);
    }

    @Override
    public void onLoaderReset(Loader<RealmResults<Coffee>> listLoader) {

    }
}
