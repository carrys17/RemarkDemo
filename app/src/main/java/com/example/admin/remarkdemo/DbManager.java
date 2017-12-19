package com.example.admin.remarkdemo;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import static com.example.admin.remarkdemo.Constant.*;

/**
 * Created by admin on 2017/12/14.
 */

public class DbManager {
    public static List<Person> cursorToPerson(Cursor cursor){
        List<Person> list = new ArrayList<>();
        while (cursor.moveToNext()){
            // 根据参数指定的字段来读取字段下标
            String wxid = cursor.getString(cursor.getColumnIndex(WXID));
            String nickname = cursor.getString(cursor.getColumnIndex(NICKNAME));
            String time = cursor.getString(cursor.getColumnIndex(TIME));
            int status = cursor.getInt(cursor.getColumnIndex(STATUS));
            Person person = new Person(wxid,nickname,time,status);
            list.add(person);
        }
        return list;
    }
}
