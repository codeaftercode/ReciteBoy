package com.codeaftercode.reciteboy.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.codeaftercode.reciteboy.bean.Question;
import com.codeaftercode.reciteboy.bean.QuestionBank;

import java.util.ArrayList;

/**
 * Created by codeaftercode on 2017/2/5.
 * 功能:
 * 1.导入1个题库
 * 2.删除1个题库
 * 3.修改题库名称
 * 4.查询题库列表
 *
 * 5.查询1个题库内:全部题目列表
 * 6.查询1个题库内:指定题型的题目列表
 */

public class Dao {
    public static final int RANGE_ALL = 0;
    public static final int RANGE_WRONG = 1;
    public static final int RANGE_COLLECT = 2;
    public static final int ORDER_NORMAL = 3;
    public static final int ORDER_REVERSE = 4;
    public static final int ORDER_RANDOM = 5;

    private final MySqliteOpenHelper mySqliteOpenHelper;

    public Dao(Context context) {
        mySqliteOpenHelper = new MySqliteOpenHelper(context);
    }

    /**
     * 导入1个题库:开启事务,更新2个表
     *
     * @param questionbank      题库信息
     * @param questionArrayList 题目列表
     */
    public boolean add(QuestionBank questionbank, ArrayList<Question> questionArrayList) {
        boolean result = false;
        //执行sql语句需要sqliteDatabase对象
        //1.调用getReadableDatabase方法,来初始化数据库的创建
        SQLiteDatabase db = mySqliteOpenHelper.getWritableDatabase();
        //2.开启事务
        db.beginTransaction();
        try {
            //3.向questionbank表插入1条记录(返回值是新记录的_id值)
            ContentValues values = new ContentValues();
            values.put("title", questionbank.getTitle());
            values.put("addDate", questionbank.getAddDate());
            values.put("count", questionbank.getCount());
            int questionbankId = (int)db.insert("questionbank", null, values);
            //4.获取题库号,如果questionbankId<0:出错
            if (questionbankId > -1) {
                questionbank.setId(questionbankId);
                //5.向question表中插入记录(答案项直接存储为大写,collect字段直接存储为0)
                for (Question question : questionArrayList) {
                    db.execSQL("insert into question(qbid,type,title,optionA,optionB,optionC,optionD,answer,correct,wrong,collect) values(?,?,?,?,?,?,?,?,?,?,?);",
                            new Object[]{questionbankId, question.getType(), question.getTitle(),
                                    question.getOptionAtIndex(0), question.getOptionAtIndex(1), question.getOptionAtIndex(2), question.getOptionAtIndex(3),
                                    question.getAnswer().toUpperCase(), question.getCorrect(), question.getWrong(), 0});
                }
                //6.事务执行成功
                db.setTransactionSuccessful();
                result = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //结束事务,关闭数据库
            db.endTransaction();
            db.close();
        }
        return result;
    }

    /**
     * 删除1个题库:题库表和题目表
     *
     * @param questionBank 题库
     * @return 操作是否成功
     */
    public Boolean del(QuestionBank questionBank) {
        Boolean result = false;
        int questionBankId = questionBank.getId();
        //1.调用getReadableDatabase方法,来初始化数据库的创建
        SQLiteDatabase db = mySqliteOpenHelper.getWritableDatabase();
        //2.开启事务
        db.beginTransaction();
        try {
            //3.删除questionbank表中1条记录,条件:_id=questionBank.getId()
            db.execSQL("delete from questionbank where _id = ? ;", new Object[]{questionBankId});
            //4.删除question表中,qbid=questionBank.getId()的全部记录
            db.execSQL("delete from question where qbid = ?;", new Object[]{questionBankId});
            //5.事务执行成功
            db.setTransactionSuccessful();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //结束事务,关闭数据库
            db.endTransaction();
            db.close();
        }

        return result;
    }

    /**
     * 修改题库名称
     * @param id    题库_id
     * @param title 修改后的题库名称
     * @return  影响的行数.小于1说明操作失败
     */
    public int updateQuestionBankTitle(int id, String title) {
        SQLiteDatabase db = mySqliteOpenHelper.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        int result = db.update("questionbank", values, "_id = ?", new String[]{Integer.toString(id)});
        db.close();
        return result;
    }

    /**
     * 查询题库列表
     *
     * @return 查询到的题库列表.即使数据库中没有数据, 也返回一个size为0的ArrayList, 不会返回null
     */
    public ArrayList<QuestionBank> queryQuestionbankList() {
        ArrayList<QuestionBank> questionBanks = new ArrayList<>();
        //执行sql语句需要sqliteDatabase对象
        //调用getReadableDatabase方法,来初始化数据库的创建
        SQLiteDatabase db = mySqliteOpenHelper.getReadableDatabase();
        //sql:sql语句，  selectionArgs:查询条件占位符的值,返回一个cursor对象
        Cursor cursor = db.rawQuery("select _id, title, addDate,count from questionbank;", null);
        //解析Cursor中的数据
        if (cursor != null && cursor.getCount() > 0) {
            //循环遍历结果集，获取每一行的内容
            QuestionBank questionBank;
            while (cursor.moveToNext()) {
                questionBank = new QuestionBank();
                //获取数据
                questionBank.setId(cursor.getInt(0));
                questionBank.setTitle(cursor.getString(1));
                questionBank.setAddDate(cursor.getString(2));
                questionBank.setCount(cursor.getInt(3));
                questionBanks.add(questionBank);
            }
            cursor.close();
        }
        //关闭数据库对象
        db.close();
        return questionBanks;
    }

    /**
     * 按sql语句查询1个题库中题目信息
     * @param qbid 题库id
     * @return 题目列表
     */
    private ArrayList<Question> queryQuestionList(int qbid, String sqlString) {
        ArrayList<Question> questions = new ArrayList<>();
        //执行sql语句需要sqliteDatabase对象
        //调用getReadableDatabase方法,来初始化数据库的创建
        SQLiteDatabase db = mySqliteOpenHelper.getReadableDatabase();
        //sql:sql语句，  selectionArgs:查询条件占位符的值,返回一个cursor对象
        Cursor cursor = db.rawQuery(sqlString, new String[]{Integer.toString(qbid)});
        //解析Cursor中的数据
        Question question;
        if (cursor != null && cursor.getCount() > 0) {
            questions = new ArrayList<>();
            cursor.moveToFirst();
            do {
                question = new Question(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getInt(2),
                        cursor.getString(3),

                        cursor.getString(8),
                        cursor.getInt(9),
                        cursor.getInt(10),
                        cursor.getInt(11) > 0,

                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6),
                        cursor.getString(7)
                        );
                questions.add(question);
            } while (cursor.moveToNext());
            cursor.close();
        }
        //关闭数据库
        db.close();
        return questions;
    }

    /**
     * 查询1个题库中全部题目
     * @param qbid 题库id
     * @return  题目列表
     */
    /*public ArrayList<Question> queryAllQuestionList(int qbid) {
        String sqlString = "select * from question where qbid = ? order by type asc, _id asc;";
        return queryQuestionList(qbid, sqlString);
    }*/

    /**
     * 查询1个题库中全部题目
     * @param qbid  题库id
     * @param range 范围.0-全部;1-仅错题;2-仅收藏
     * @param order 顺序.0-正序;1-反序;3-随机
     * @return  题目列表
     */
    public ArrayList<Question> queryQuestionList(int qbid, int range, int order) {
        //准备查询字符串
        StringBuilder sqlString = new StringBuilder("select * from question where qbid = ?");
        //确定查询条件
        switch (range) {
            case RANGE_ALL:
                break;
            case RANGE_WRONG:
                sqlString.append(" and wrong > 0");
                break;
            case RANGE_COLLECT:
                sqlString.append(" and collect > 0");
                break;
            default:
                break;
        }
        //确定顺序
        switch (order) {
            case ORDER_NORMAL:
                sqlString.append(" order by type asc, _id asc;");
                break;
            case ORDER_REVERSE:
                sqlString.append(" order by type asc, _id desc;");
                break;
            case ORDER_RANDOM:
                sqlString.append(" order by type asc, random()");
                break;
            default:
                break;
        }
        return queryQuestionList(qbid, sqlString.toString());
    }

    /**
     * 查询题库中的题目数量
     * @param qbid  题库id
     * @param range 0-全部题目;1-错题;2-收藏题
     * @return  题目数量
     */
    public int queryQuestionCount(int qbid, int range) {
        int count = -1;
        StringBuilder sql = new StringBuilder("select count(_id) from question where qbid = ?");
        if (range == 1) {
            sql.append(" and wrong > 0");
        } else if (range == 2) {
            sql.append(" and collect > 0");
        }
        SQLiteDatabase db = mySqliteOpenHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(sql.toString(), new String[]{Integer.toString(qbid)});
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            count = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return count;
    }

    /**
     * 查询1个题库中全部做错的题
     * @param qbid  题库id
     * @return 题目列表
     *//*
    public ArrayList<Question> queryWrongQuestionList(int qbid) {
        String sqlString = "select * from question where qbid = ? and wrong > 0 order by type asc, _id asc;";
        return queryQuestionList(qbid, sqlString);
    }

    *//**
     * 查询1个题库中全部收藏题
     * @param qbid  题库id
     * @return 题目列表
     *//*
    public ArrayList<Question> queryCollectQuestionList(int qbid) {
        String sqlString = "select * from question where qbid = ? and collect > 0 order by type asc, _id asc;";
        return queryQuestionList(qbid, sqlString);
    }*/

    /**
     * 查询1个题库中指定类型的题目信息列表
     * @param qbid 题库号
     * @param type 题型
     * @return 返回题目列表
     */
    public ArrayList<Question> queryQuestionListByType(int qbid, int type) {
        String sqlString = "select * from question where qbid = ? and type = " + type + " order by type asc, _id asc;";
        return queryQuestionList(qbid, sqlString);
    }

    /**
     * 修改题目信息
     * @param id    题目索引
     * @param title 题目
     * @param optionA   选项A
     * @param optionB   选项B
     * @param optionC   选项C
     * @param optionD   选项D
     * @param answer @return      操作结果
     */
    public boolean updateQuestion(int id, String title, String optionA, String optionB, String optionC, String optionD, String answer) {
        SQLiteDatabase db = mySqliteOpenHelper.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("optionA", optionA);
        values.put("optionB", optionB);
        values.put("optionC", optionC);
        values.put("optionD", optionD);
        values.put("answer", answer);
        int result = db.update("question", values, "_id = ?", new String[]{Integer.toString(id)});
        db.close();
        return result > 0;
    }

    /**
     * 修改题目收藏标志
     * @param flag  新标志
     * @param id    题目索引
     * @return      操作结果
     */
    public boolean updateQuestionCollectFlag(boolean flag, int id) {
        SQLiteDatabase db = mySqliteOpenHelper.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("collect", flag);
        int result = db.update("question", values, "_id = ?", new String[]{Integer.toString(id)});
        db.close();
        return result > 0;
    }

    /**
     * 更新题目回答正确的次数
     * @param correct   正确次数
     * @param id        题目索引
     * @return          操作结果
     */
    public boolean updateQuestionCorrect(int correct, int id) {
        SQLiteDatabase db = mySqliteOpenHelper.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("correct", correct);
        int result = db.update("question", values, "_id = ?", new String[]{Integer.toString(id)});
        db.close();
        return result > 0;
    }

    /**
     * 更新题目回答错误的次数
     * @param wrong     错误次数
     * @param id        题目索引
     * @return          操作结果
     */
    public boolean updateQuestionWrong(int wrong, int id) {
        SQLiteDatabase db = mySqliteOpenHelper.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("wrong", wrong);
        int result = db.update("question", values, "_id = ?", new String[]{Integer.toString(id)});
        db.close();
        return result > 0;
    }

    /**
     * 删除1个题目
     * @param qbid  题库_id
     * @param id    题目_id
     * @return      操作结果
     */
    public Boolean delQuestionById(int count, int qbid, int id) {
        Boolean result = false;
        SQLiteDatabase db = mySqliteOpenHelper.getWritableDatabase();
        //2.开启事务
        db.beginTransaction();
        try {
            //3.删除question表中1条记录,条件:_id=id
            db.execSQL("delete from question where _id = ? ;", new Object[]{Integer.toString(id)});
            //4.更新questionbank表的count字段
            //db.execSQL("update questionbank set count = ? where _id = ? ;", new Object[]{Integer.toString(count), Integer.toString(qbid)});
            ContentValues values = new ContentValues();
            values.put("count", count);
            db.update("questionbank", values, "_id = ?", new String[]{Integer.toString(qbid)});
            //5.事务执行成功
            db.setTransactionSuccessful();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //结束事务,关闭数据库
            db.endTransaction();
            db.close();
        }

        return result;
    }
}
