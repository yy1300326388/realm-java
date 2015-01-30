package io.realm.entities;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "test";

    private static final String DATABASE_NAME = "testing.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String CREATE_TABLE_TEST = "CREATE TABLE "
            + TABLE_NAME + "(" + "string"
            + " text not null, " + "integer"
            + " int null" + ")";

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TEST);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void dropTable(SQLiteDatabase db) {db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);}


}
