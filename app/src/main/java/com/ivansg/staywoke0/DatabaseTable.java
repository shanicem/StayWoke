package com.ivansg.staywoke0;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Ivan on 2017-10-07.
 */

public class DatabaseTable {

    private static final String TAG = "DictionaryDatabase";

    //The columns we'll include in the dictionary table (we must have one called '_id')
    public static final String STOP_NAME = "_id";
    public static final String STOP_LAT = "STOP_LAT";
    public static final String STOP_LON = "STOP_LON";

    private static final String DATABASE_NAME = "DICTIONARY";
    private static final String FTS_VIRTUAL_TABLE = "FTS";
    // increase this number by one whenever the database must be updated
    private static final int DATABASE_VERSION = 13;

    private final DatabaseOpenHelper mDatabaseOpenHelper;

    public DatabaseTable(Context context) {
        mDatabaseOpenHelper = new DatabaseOpenHelper(context);
    }

    public Cursor getStopMatches(String query, String[] columns) {
        String selection = STOP_NAME + " MATCH ?";
        String[] selectionArgs = new String[] {query+"*"};

        return query(selection, selectionArgs, columns);
    }

    private Cursor query(String selection, String[] selectionArgs, String[] columns) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);

        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                columns, selection, selectionArgs, null, null, null);

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    private static class DatabaseOpenHelper extends SQLiteOpenHelper {

        private final Context mHelperContext;
        private SQLiteDatabase mDatabase;

        private static final String FTS_TABLE_CREATE =
                "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
                        " USING fts3 (" +
                        STOP_NAME + ", " +
                        STOP_LAT + ", " +
                        STOP_LON + ")";

        DatabaseOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mHelperContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            mDatabase = db;
            mDatabase.execSQL(FTS_TABLE_CREATE);
            loadDictionary();
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }

        private void loadDictionary() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        loadStops();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }

        private void loadStops() throws IOException {
            final Resources resources = mHelperContext.getResources();
            InputStream inputStream = resources.openRawResource(R.raw.stops);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] strings = TextUtils.split(line, ",");
                    if (strings.length < 3) continue;
                    long id = addStop(strings[0].trim(), strings[1].trim(),
                            strings[2].trim());
                    if (id < 0) {
                        Log.e(TAG, "unable to add stop: " + strings[0].trim());
                    }
                }
            } finally {
                reader.close();
            }
        }

        public long addStop(String stop_name, String stop_lat, String stop_lon) {
            ContentValues initialValues = new ContentValues();
            initialValues.put(STOP_NAME, stop_name);
            initialValues.put(STOP_LAT, stop_lat);
            initialValues.put(STOP_LON, stop_lon);

            return mDatabase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
        }
    }
}
