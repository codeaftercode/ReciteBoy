package com.codeaftercode.reciteboy.questionbank;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.codeaftercode.reciteboy.R;
import com.codeaftercode.reciteboy.bean.Question;
import com.codeaftercode.reciteboy.bean.QuestionBank;
import com.codeaftercode.reciteboy.common.BaseActivity;
import com.codeaftercode.reciteboy.common.MyApplication;
import com.codeaftercode.reciteboy.databinding.ActivityQuestionInfoBinding;
import com.codeaftercode.reciteboy.db.Dao;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by codeaftercode on 2017/2/15.
 * 1.显示题目信息
 * 2.修改题目信息(未实现)
 * 4.收藏/取消收藏题目
 * 5.删除题目
 */

public class QuestionInfoActivity extends BaseActivity {
    private static final int EDIT_SUCCESS = 1;
    private static final int EDIT_FAIL = 2;
    //控件
    private Context mContext;
    private ActionBar mActionBar;

    //当前题目在 当前题库 的同一类型题中的位置(从0开始.显示时应+1)
    private int mQuestionIndex;
    //同种类型题列表
    private ArrayList<Question> mQuestionArrayList;
    //当前题库在 MyApplication中的索引
    private int mQuestionbankIndex;
    //当前题目所在的适配器索引
    private int mAdapterIndex;
    private ActivityQuestionInfoBinding binding;

    //手势
    private GestureDetector gestureDetector;

    private Handler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_question_info);
        mContext = this;

        init();

        update();

        mActionBar.setDisplayHomeAsUpEnabled(true);
        gestureDetector = new GestureDetector(mContext, onGestureListener);
    }

    @SuppressWarnings("unchecked")
    private void init() {
        // 准备数据:获取从上一个活动传递过来的数据
        Intent intent = getIntent();
        mQuestionbankIndex = intent.getIntExtra("questionbank_index", -1);
        mQuestionIndex = intent.getIntExtra("question_index", -1);
        mAdapterIndex = intent.getIntExtra("adapter_index", -1);
        mQuestionArrayList = (ArrayList<Question>) intent.getSerializableExtra("list");

        if (mQuestionbankIndex < 0 || mQuestionbankIndex >= MyApplication.getQuestionBankArrayList().size()
                || mQuestionIndex < 0 || mQuestionIndex >= mQuestionArrayList.size()
                || mAdapterIndex < 0) {
            //出错了
            Toast.makeText(mContext, "题库索引错误!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 设置标题
        mActionBar = getSupportActionBar();
        mActionBar.setTitle(MyApplication.getQuestionBankArrayList().get(mQuestionbankIndex).getTitle());
    }

    private void update() {
        //检查数据
        if (mQuestionIndex < 0 || mQuestionIndex >= mQuestionArrayList.size()) {
            Toast.makeText(mContext, "题目索引错误!", Toast.LENGTH_SHORT).show();
            finish();
        }
        Question question = mQuestionArrayList.get(mQuestionIndex);
        binding.setQuestion(question);
        binding.setIndex(mQuestionIndex + 1);
        binding.setEditable(false);
        //设置/重置收藏图标
        resetCollectIcon();
    }

    /**
     * 显示菜单
     *
     * @param menu 菜单资源
     * @return 操作结果
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.question_info, menu);
        resetCollectIcon();
        return true;
    }

    public void resetCollectIcon() {
        this.getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
    }

    /**
     * 根据收藏标记,动态修改菜单图标
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mQuestionArrayList.get(mQuestionIndex).isCollect()) {
            menu.findItem(R.id.action_collect_question).
                    setTitle("取消收藏").
                    setIcon(R.mipmap.menu_collect_true_icon);
        } else {
            menu.findItem(R.id.action_collect_question).
                    setTitle("收藏").
                    setIcon(R.mipmap.menu_collect_false_icon);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * action监听
     *
     * @param item action
     * @return 操作结果
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_question:
                //编辑
                editQuestion();
                break;
            case R.id.action_delete_question:
                //删除
                deleteQuestion();
                break;
            case R.id.action_collect_question:
                //收藏
                Dao dao = new Dao(mContext);
                Question question = mQuestionArrayList.get(mQuestionIndex);
                if (dao.updateQuestionCollectFlag(!question.isCollect(), question.getId())) {
                    resetCollectIcon();
                    question.setCollect(!question.isCollect());
                    Toast.makeText(mContext, question.isCollect() ? "已收藏!" : "已取消收藏!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "操作失败!", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * 修改本题
     */
    private void editQuestion() {
        //控件可编辑,显示提交按钮:
        binding.setEditable(true);
        //监听提交修改
        binding.btnQuestionEditSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateQuestion();
            }
        });
        //监听取消修改
        binding.btnQuestionEditCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //恢复数据
                binding.setQuestion(mQuestionArrayList.get(mQuestionIndex));
                //禁止编辑
                binding.setEditable(false);
            }
        });
        //监听重置
        binding.btnQuestionEditReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.setQuestion(mQuestionArrayList.get(mQuestionIndex));
            }
        });

    }

    /**
     * 执行修改本题
     */
    private void updateQuestion() {
        //1.获取题目信息
        final Question question = mQuestionArrayList.get(mQuestionIndex);
        //2.获取用户修改后的信息,同时检查用户输入是否合法
        //题目标题
        final String title = binding.etQuestionTitle.getText().toString();
        if (title.isEmpty()) {
            Toast.makeText(mContext, "题目不能为空!", Toast.LENGTH_SHORT).show();
            return;
        }
        //选项
        final String optionA = binding.etQuestionOptionA.getText().toString();
        final String optionB = binding.etQuestionOptionB.getText().toString();
        final String optionC = binding.etQuestionOptionC.getText().toString();
        final String optionD = binding.etQuestionOptionD.getText().toString();
        if (question.getType() < 2 && optionA.isEmpty() || optionB.isEmpty() || optionC.isEmpty() || optionD.isEmpty()) {
            Toast.makeText(mContext, "选项不能为空!", Toast.LENGTH_SHORT).show();
            return;
        }
        //获取答案并转换为大写
        final String answer = binding.etQuestionAnswer.getText().toString().toUpperCase();
        if (answer.isEmpty()) {
            Toast.makeText(mContext, "答案不能为空!", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (question.getType()) {
            case 0:
                if (answer.length() > 1) {
                    Toast.makeText(mContext, "单选题只能选择1个答案!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!"ABCD".contains(answer)) {
                    Toast.makeText(mContext, "单选题答案只能是ABCD中的1个!", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            case 1:
                if (answer.length() > Question.TYPES.length) {
                    Toast.makeText(mContext, "多选题最多有" + Question.TYPES.length + "个答案!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Pattern pattern = Pattern.compile("(?!.*[A].*[A])(?!.*[B].*[B])(?!.*[C].*[C])(?!.*[D].*[D])^[A-D]+$");
                Matcher matcher = pattern.matcher(answer);
                Boolean result = matcher.matches();
                if (!result) {
                    Toast.makeText(mContext, "多选题答案只能为ABCD中的1个或多个!", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            case 2:
                //TODO 检查SQL注入(如果联网必须检查,单机无所谓)
                // 填空题检查:字符串不能超出数据库支持的长度;
                break;
            case 3:
                if (!("对".equals(answer) || "错".equals(answer))) {
                    Toast.makeText(mContext, "判断题答案只能是对或错!", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            default:
                break;
        }

        //3.用户输入的数据全部合法,开始多线程存储,操作结果由Handle处理
        new Thread(new Runnable() {
            @Override
            public void run() {
                Dao dao = new Dao(mContext);
                Boolean result = dao.updateQuestion(question.getId(), title, optionA, optionB, optionC, optionD, answer);
                if (result) {
                    Question newQuestion = mQuestionArrayList.get(mQuestionIndex);
                    newQuestion.setTitle(title);
                    newQuestion.setOptionAtIndex(optionA, 0);
                    newQuestion.setOptionAtIndex(optionB, 1);
                    newQuestion.setOptionAtIndex(optionC, 2);
                    newQuestion.setOptionAtIndex(optionD, 3);
                    newQuestion.setAnswer(answer);
                    mHandler.sendMessage(mHandler.obtainMessage(EDIT_SUCCESS));
                } else {
                    mHandler.sendMessage(mHandler.obtainMessage(EDIT_FAIL));
                }
            }
        }).start();

        //4.退出编辑
        binding.setEditable(false);
    }

    /**
     * 删除本题
     */
    private void deleteQuestion() {
        final Question question = mQuestionArrayList.get(mQuestionIndex);
        //1.弹出对话框,要求用户确认删除题库
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setTitle("提示")
                .setMessage("确认从题库中删除本题?")
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(mContext, "正在删除...", Toast.LENGTH_SHORT).show();
                        //利用Dao删除题库
                        Dao dao = new Dao(mContext);
                        QuestionBank questionBank = MyApplication.getQuestionBankArrayList().get(mQuestionbankIndex);
                        Boolean delResult = dao.delQuestionById(questionBank.getCount(), question.getQbid(), question.getId());
                        if (delResult) {
                            //删除操作成功
                            // [1]通知上一个活动:题库信息发生变化,由上一个活动通知适配器数据更新
                            Intent intent = new Intent();
                            intent.putExtra("adapter_index", mAdapterIndex);
                            intent.putExtra("question_index", mQuestionIndex);
                            setResult(RESULT_OK, intent);
                            // [2]在mQuestionArrayList中删除此项,改变questionBank的count字段
                            //mQuestionArrayList.remove(mQuestionIndex);//失败,改为在上一活动中删除
                            questionBank.setCount(questionBank.getCount() - 1);
                            // [3]提示用户
                            Toast.makeText(mContext, "操作成功!", Toast.LENGTH_SHORT).show();
                            // [4]关闭本页
                            finish();
                        } else {
                            Toast.makeText(mContext, "操作失败!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(mContext, "取消删除", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }


    //手势监听
    private GestureDetector.OnGestureListener onGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                       float velocityY) {
                    float x = e2.getX() - e1.getX();
                    //float y = e2.getY() - e1.getY();

                    if (x > 0) {
                        doResult(RIGHT);
                    } else if (x < 0) {
                        doResult(LEFT);
                    }
                    return true;
                }
            };

    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    public void doResult(int action) {

        switch (action) {
            case RIGHT:
                //向右划,看前一题
                if (--mQuestionIndex < 0) {
                    mQuestionIndex++;
                    Toast.makeText(mContext, "已经是第一题!", Toast.LENGTH_SHORT).show();
                } else {
                    update();
                }
                break;

            case LEFT:
                int max = mQuestionArrayList.size() - 1;
                if (++mQuestionIndex > max) {
                    mQuestionIndex = max;
                    Toast.makeText(mContext, "已经是最后一题!", Toast.LENGTH_SHORT).show();
                } else {
                    update();
                }
                break;
        }
    }

    private static class MyHandler extends Handler {
        private WeakReference<QuestionInfoActivity> mActivity;
        MyHandler(QuestionInfoActivity activity) {
            mActivity = new WeakReference<>(activity);
        }



        @Override
        public void handleMessage(Message msg) {
            QuestionInfoActivity theActivity = mActivity.get();
            switch (msg.what) {
                case EDIT_SUCCESS:
                    theActivity.binding.setQuestion(theActivity.mQuestionArrayList.get(theActivity.mQuestionIndex));
                    Toast.makeText(theActivity, theActivity.getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
                    break;
                case EDIT_FAIL:
                    Toast.makeText(theActivity, theActivity.getResources().getString(R.string.fail), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }
}
