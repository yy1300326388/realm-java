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

package io.realm.examples.realmadapters.providers;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmObject;

public class RealmContentProvider extends ContentProvider {

    private static final String TAG = RealmContentProvider.class.getName();

    private final ThreadLocal<Boolean> mApplyingBatch = new ThreadLocal<Boolean>();

    private Realm mRealm = null;

    @Override
    public boolean onCreate() {
        mRealm = Realm.getInstance(getContext());
        return true;
    }

    //TODO: Huge Issue??  Need a Cursor implementation to return
    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings2, String s2) {
        //TODO:  Uri processing
        RealmCursor rCursor = new RealmCursor(getContext(), Class.class);
        rCursor.query(uri, strings, s, strings2, s2);
        return rCursor;
    }

    @Override
    public String getType(Uri uri) {
        List<String> segments = uri.getPathSegments();
        return segments.get(segments.size()-1);
    }

    // Here I just assume the type is the last field in the Uri;
    // however; in reality this depends on Uri selection
    public Class getTypeFromUri(Uri uri) {
        try {
            //TODO:  Issue?  create object from Uri string?
            //This examples requires fully qualified as in  http://stackoverflow.com/questions/2408789/getting-class-type-from-string
            return (Class<RealmObject>)Class.forName(getType(uri));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean applyingBatch() {
        return mApplyingBatch.get() != null && mApplyingBatch.get();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        //TODO:  Handle failover
        Class<RealmObject> clazz = getTypeFromUri(uri);

        mRealm.beginTransaction();
        mRealm.createObject(clazz);
        //TODO:  Issue?  Converting ContentValues to relevant fields is a pain
        mRealm.commitTransaction();
        //TODO:  Huge Issue?  ContentProvider requires specific row id for insert Uri
        int rowIndex = 1;

        Uri insertUri = new Uri.Builder().appendEncodedPath(rowIndex + "").build();
        return insertUri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        //TODO:  Handle failover
        Class<RealmObject> clazz = getTypeFromUri(uri);

        mRealm.beginTransaction();
        for(int i = 0; i < values.length; i++) {
            mRealm.createObject(clazz);
            //TODO:  Issue?  Converting ContentValues to relevant fields is a pain
        }
        mRealm.commitTransaction();

        //TODO:  Should provide actual insert count completed
        return values.length;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        //TODO:  Huge Issue?  This selection field allows complex SQL queries

        //TODO:  This should support the possibility of multiple items...
        return 1;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        //TODO:  Huge Issue?  This selection field allows complex SQL queries

        //TODO:  This should support the possibility of multiple items...
        return 1;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {

        //TODO:  Annoying Issue?  Have to also comply with the ContentProviderOperation interface...
        //https://developer.android.com/reference/android/content/ContentProviderOperation.html

        return null;
    }
}
