package com.codeaftercode.reciteboy.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * Created by codeaftercode on 2016/12/20.
 */

class MySqliteOpenHelper extends SQLiteOpenHelper {
    MySqliteOpenHelper(Context context) {
        //数据库版本号:1
        super(context, "questionbank.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //首次运行,建立两表
        String createQuestionbank = "create table questionbank ("
                +" _id integer primary key autoincrement,"
                +" title text,"
                +" adddate text, "
                +" count integer"
                +" )";
        String createQuestion = "create table question ( "
                +" _id integer primary key autoincrement,"
                +" qbid integer,"
                +" type integer,"
                +" title text,"
                +" optionA text,"
                +" optionB text,"
                +" optionC text,"
                +" optionD text,"
                +" answer text, "
                +" correct integer, "
                +" wrong integer, "
                +" collect integer "
                +" )";
        db.execSQL(createQuestionbank);
        db.execSQL(createQuestion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //数据库升级
    }
}
