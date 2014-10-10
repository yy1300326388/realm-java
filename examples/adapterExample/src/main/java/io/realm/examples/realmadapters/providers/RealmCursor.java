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

import android.content.ContentResolver;
import android.content.Context;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

import java.lang.reflect.Field;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class RealmCursor implements Cursor {

    // This implementation attempts to assign indexes for items in the Realm
    // This implementation will not tolerate View changes

    private Context context = null;
    private Class typeClazz = null;

    private Realm mRealm = null;

    private RealmQuery mRealmQuery = null;

    private RealmResults mResults = null;

    //TODO:  Annoying Issue?  Requires maintainance of a current index in the view.
    //TODO:  Huge Issue?  When a requery() occurs these indexes get screwed; poses issues with changing views
    private int mCurrentPosition = -1;

    //TODO:  Extend for general case of realm names
    public RealmCursor(Context context, Class clazz) {
        this.context = context;
        this.typeClazz = clazz;

        mResults = mRealm.allObjects(typeClazz);
    }

    //    From Google API javadoc:
    //    uri	The URI to query. This will be the full URI sent by the client; if the client is requesting a specific record, the URI will end in a record number that the implementation should parse and add to a WHERE or HAVING clause, specifying that _id value.
    //    projection	The list of columns to put into the cursor. If null all columns are included.
    //    selection	A selection criteria to apply when filtering rows. If null then all rows are included.
    //    selectionArgs	You may include ?s in selection, which will be replaced by the values from selectionArgs, in order that they appear in the selection. The values will be bound as Strings.
    //    sortOrder	How the rows in the cursor should be sorted. If null then the provider is free to define the sort order.

    // This is necessary for the ContentProvider to pair down fields
    public void query (Uri uri, String[] projection,
                       String selection, String[] selectionArgs, String sortOrder) {
        mRealmQuery = mResults.where();
        mResults = mRealmQuery
                //TODO:  Huge Issue?  The selection field allows SQL query statements
                //TODO:     -- this is difficult to transform into Realm query language
                //TODO:  Projection limiting?
                //TODO:  Sort Order?
                .findAll();
        return;
    }

    @Override
    public int getCount() {
        return mResults.size();
    }

    @Override
    public int getPosition() {
        return mCurrentPosition;
    }

    // NOTE:  Cursors allow users to extend past possible index bounds (before/after)
    // This is a relative amount
    @Override
    public boolean move(int i) {
        mCurrentPosition += i;
        if (mResults.size() <= mCurrentPosition) {
            return false;
        }
        return true;
    }

    @Override
    public boolean moveToPosition(int i) {
        mCurrentPosition = i;
        if (mResults.size() <= mCurrentPosition) {
            return false;
        }
        return true;
    }

    @Override
    public boolean moveToFirst() {
        mCurrentPosition = 0;
        if (mResults.size() <= mCurrentPosition) {
            return false;
        }
        return true;
    }

    @Override
    public boolean moveToLast() {
        mCurrentPosition = mResults.size() - 1;
        if (mResults.size() <= mCurrentPosition) {
            return false;
        }
        return true;
    }

    @Override
    public boolean moveToNext() {
        mCurrentPosition++;
        if (mResults.size() <= mCurrentPosition) {
            return false;
        }
        return true;
    }

    @Override
    public boolean moveToPrevious() {
        mCurrentPosition--;
        if (mCurrentPosition < 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isFirst() {
        if (mCurrentPosition == 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isLast() {
        if (mCurrentPosition == (mResults.size() - 1)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isBeforeFirst() {
        if(mCurrentPosition < 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isAfterLast() {
        if (mCurrentPosition > (mResults.size() - 1)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getColumnIndex(String s) {
        return 0;
    }

    @Override
    public int getColumnIndexOrThrow(String s) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public String getColumnName(int i) {
        return null;
    }

    //TODO:  Use reflection or no???
    @Override
    public String[] getColumnNames() {
        int iter = 0;
        String[] strArray = new String[typeClazz.getFields().length];
        for (Field field : typeClazz.getFields()) {
            strArray[iter] = field.getName();
            iter++;
        }
        return strArray;
    }

    @Override
    public int getColumnCount() {
        return typeClazz.getFields().length;
    }

    @Override
    public byte[] getBlob(int i) {
        return new byte[0];
    }

    //TODO:  Perhaps these methods already do bounds checking?
    @Override
    public String getString(int i) {
        return mResults.getTable().getString(mCurrentPosition, i);
    }

    @Override
    public void copyStringToBuffer(int i, CharArrayBuffer charArrayBuffer) {
        String str = mResults.getTable().getString(mCurrentPosition, i);
        char[] buf = new char[str.length()];
        str.getChars(0, str.length(), buf, 0);
        new CharArrayBuffer(buf);
    }

    @Override
    public short getShort(int i) {
        return (short)mResults.getTable().getLong(mCurrentPosition, i);
    }

    @Override
    public int getInt(int i) {
        return (int)mResults.getTable().getLong(mCurrentPosition, i);
    }

    @Override
    public long getLong(int i) {
        return mResults.getTable().getLong(mCurrentPosition, i);
    }

    @Override
    public float getFloat(int i) {
        return mResults.getTable().getFloat(mCurrentPosition, i);
    }

    @Override
    public double getDouble(int i) {
        return mResults.getTable().getDouble(mCurrentPosition, i);
    }

//    FIELD_TYPE_NULL
//    FIELD_TYPE_INTEGER
//    FIELD_TYPE_FLOAT
//    FIELD_TYPE_STRING
//    FIELD_TYPE_BLOB

    @Override
    public int getType(int i) {
        switch(mResults.getTable().getColumnType(i)) {
            case INTEGER:
                return FIELD_TYPE_INTEGER;
            case FLOAT:
                return FIELD_TYPE_FLOAT;
            case STRING:
                return FIELD_TYPE_STRING;
            case BINARY:
                return FIELD_TYPE_NULL;
            default:
                //TODO:  Fix if assumption?  Seems odd that there are only 5 types
                return FIELD_TYPE_NULL;
        }
    }

    @Override
    public boolean isNull(int i) {
        return getType(i) == FIELD_TYPE_NULL;
    }

    @Override
    public void deactivate() {
        close();
    }

    //TODO:  Huge Issue?  When a requery() occurs mCurrentIndex gets entirely thrown off.
    //Probably would need to leave this method unsupported
    @Override
    public boolean requery() {
        if(mRealmQuery != null) {
            mResults = mRealmQuery.findAll();
        }
        if(mResults != null) {
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        mResults = null;
        mRealm   = null;
    }

    @Override
    public boolean isClosed() {
        if(mResults == null || mRealm == null) {
            return true;
        }
        return false;
    }

    //TODO:  Implement the below
    @Override
    public void registerContentObserver(ContentObserver contentObserver) {

    }

    @Override
    public void unregisterContentObserver(ContentObserver contentObserver) {

    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public void setNotificationUri(ContentResolver contentResolver, Uri uri) {
        //TODO:  Issue?  Notifications may be entirely different in Cursor speak
    }

    @Override
    public Uri getNotificationUri() {
        //TODO:  Issue?  Notifications may be entirely different in Cursor speak
        return null;
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return false;
    }

    @Override
    public Bundle getExtras() {
        return null;
    }

    @Override
    public Bundle respond(Bundle bundle) {
        return null;
    }

}
