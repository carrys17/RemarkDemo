package com.example.admin.remarkdemo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.example.admin.remarkdemo.Constant.*;

/**
 * Created by admin on 2017/12/14.
 */

public class MyDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "MyDatabaseHelper";

    public MyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null,DATA_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table if not exists "+TABLE_NAME +" ("+WXID+" varchar(128) not null unique,"+CONREMARK+" varchar(1024),"+TIME+" varchar(128),"+STATUS+" int)";
        Log.i(TAG,sql);
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }
}
