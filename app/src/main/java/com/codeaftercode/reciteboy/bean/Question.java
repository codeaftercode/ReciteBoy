package com.codeaftercode.reciteboy.bean;

import java.io.Serializable;

/**
 * Created by codeaftercode on 2016/12/19.
 * 数据库question表的bean类
 */

public class Question implements Serializable {
    // 题型
    public static final String[] TYPES = {"单选题", "多选题", "填空题", "判断题"};
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    //题库号
    private int qbid;

    public int getQbid() {
        return qbid;
    }

    public void setQbid(int qbid) {
        this.qbid = qbid;
    }

    //题型
    private int type;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    //题目内容
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    /*
    * 正确答案
    * 单项选择题为ABCD中的一个
    * 多项选择题为ABCD中的多个
    * 判断题为1或0,代表正确或错误
    * 填空题为填入的内容,一题多空以";"分隔
    * */
    private String answer;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }


    //答对的次数
    private int correct;

    public int getCorrect() {
        return correct;
    }

    public void setCorrect(int correct) {
        this.correct = correct;
    }

    //答错的次数
    private int wrong;

    public int getWrong() {
        return wrong;
    }

    public void setWrong(int wrong) {
        this.wrong = wrong;
    }

    //收藏标记
    private boolean collect;

    public void setCollect(boolean flag) {
        collect = flag;
    }

    public boolean isCollect() {
        return collect;
    }

    //选项
    private String[] options = new String[4];

    public String getOptionAtIndex(int index) {
        if (index < 0 || index >= options.length || null == options[index]) {
            return "";
        }
        return options[index];
    }

    public void setOptionAtIndex(String optionString, int index) {
        if (index < 0 || index >= options.length || null == optionString) {
            return;
        }
        options[index] = optionString;
    }

    public Question() {
        this.correct = 0;
        this.wrong = 0;
    }

    public Question(int id, int qbid, int type, String title, String answer, int correct, int wrong, boolean collect, String... options) {
        this.id = id;
        this.qbid = qbid;
        this.type = type;
        this.title = title;
        this.answer = answer;
        this.correct = correct;
        this.wrong = wrong;
        this.collect = collect;
        this.options = options;
    }

    public String toString() {
        return title;
    }
}
