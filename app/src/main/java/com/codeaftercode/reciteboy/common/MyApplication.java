package com.codeaftercode.reciteboy.common;

import android.app.Application;
import android.content.Context;

import com.codeaftercode.reciteboy.bean.QuestionBank;
import com.codeaftercode.reciteboy.db.Dao;

import java.util.ArrayList;

/**
 * Created by codeaftercode on 2017/2/11.
 */

public class MyApplication extends Application {

    private static ArrayList<QuestionBank> mQuestionBankArrayList;

    public static ArrayList<QuestionBank> getQuestionBankArrayList() {
        if (null == mQuestionBankArrayList) {
            mQuestionBankArrayList =new  ArrayList<>();
        }
        return mQuestionBankArrayList;
    }

    public static void initQuestionBankArrayList(Context context) {
        //创建DAO对象做增删改查
        Dao dao = new Dao(context);
        //查询数据
        mQuestionBankArrayList = dao.queryQuestionbankList();
    }

    @Override
    public void onCreate() {

    }
}
