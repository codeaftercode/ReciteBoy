package com.codeaftercode.reciteboy.bean;

import android.databinding.BaseObservable;


import java.io.Serializable;

/**
 * Created by codeaftercode on 2016/12/19.
 * 数据库questionbank表的bean类
 */

public class QuestionBank extends BaseObservable implements Serializable {
    private int id;
    //题库名称
    private String title;
    //添加日期
    private String addDate;
    //题目数量
    private int count;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        notifyChange();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        notifyChange();
    }

    public String getAddDate() {
        return addDate;
    }

    public void setAddDate(String addDate) {
        this.addDate = addDate;
        notifyChange();
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
        notifyChange();
    }

    public String toString() {
        return title;
    }
}
