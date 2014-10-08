package io.realm.examples.loader;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.examples.loader.model.Coffee;

public class CoffeeLoader extends AsyncTaskLoader<RealmResults<Coffee>> {

    public static final String TAG = CoffeeLoader.class.getName();

    private Context context = null;
    private Bundle args = null;

    //Added this field to make Loaders work under support package.
    private boolean dataIsReady = false;

    public CoffeeLoader(final Context context, Bundle bundle) {
        super(context);

        this.context = context;
        this.args = bundle;
    }

    //Added this method to make loaders from support package work
    @Override
    public void onStartLoading() {
        if (dataIsReady) {
            deliverResult(null);
        } else {
            forceLoad();
        }
    }

    @Override
    public RealmResults<Coffee> loadInBackground() throws RuntimeException {
        //Store the retrieved items to the Realm
        Realm realm = Realm.getInstance(context);
        realm.deleteRealmFile(context); //Delete the Realm (in the event of a Reset)

        InputStream stream = null;
        try {
            stream = context.getAssets().open("coffees.json");
        } catch (IOException e) {
            return null;
        }

        JsonParser parser = new JsonParser();
        JsonArray jsonArray = parser.parse(new InputStreamReader(stream)).getAsJsonArray();

        // Open a transaction to store items into the realm
        realm.beginTransaction();
        for (JsonElement e : jsonArray) {
            // Create a realm capable object
            Coffee coffee = realm.createObject(Coffee.class);
            coffee.setName(e.getAsJsonObject().get("name").getAsString());
            coffee.setOrigin(e.getAsJsonObject().get("origin").getAsString());
        }
        realm.commitTransaction();

        RealmResults<Coffee> results = realm.where(Coffee.class).findAll();

        dataIsReady = true;

        return results;
    }

    public Gson getGson() {
        return new Gson();
    }
}