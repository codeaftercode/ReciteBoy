package com.codeaftercode.reciteboy.exercise;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.codeaftercode.reciteboy.R;
import com.codeaftercode.reciteboy.bean.Question;
import com.codeaftercode.reciteboy.bean.QuestionBank;
import com.codeaftercode.reciteboy.common.BaseActivity;
import com.codeaftercode.reciteboy.databinding.ActivityExerciseBinding;
import com.codeaftercode.reciteboy.databinding.ActivityExerciseReportBinding;
import com.codeaftercode.reciteboy.databinding.QuestionInfoType0Binding;
import com.codeaftercode.reciteboy.databinding.QuestionInfoType1Binding;
import com.codeaftercode.reciteboy.databinding.QuestionInfoType2Binding;
import com.codeaftercode.reciteboy.databinding.QuestionInfoType3Binding;
import com.codeaftercode.reciteboy.db.Dao;
import com.codeaftercode.reciteboy.util.LogUtil;
import com.codeaftercode.reciteboy.util.TimeUtils;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * Created by codeaftercode on 2017/2/27.
 */

public class ExerciseActivity extends BaseActivity implements View.OnClickListener {
    private static final int ERROR_EMPTY = 1;
    private static final int READY = 2;
    private Context mContext;
    private Handler mHandler;
    private ActivityExerciseBinding mBinding;
    QuestionInfoType0Binding mType0Binding;
    QuestionInfoType1Binding mType1Binding;
    QuestionInfoType2Binding mType2Binding;
    QuestionInfoType3Binding mType3Binding;

    //当前题库
    private QuestionBank mQuestionbank;
    //题目列表
    private ArrayList<Question> mQuestionArrayList;
    //当前题目索引
    private int mIndex = 0;
    //练习范围
    private int mRange;
    //练习顺序
    private int mOrder;
    //练习开始时间
    private long mStartTime;
    //各题型回答正确/错误次数
    private int[] mCorrectCount = new int[Question.TYPES.length];
    private int[] mIncorrectCount = new int[Question.TYPES.length];
    //各类型题题目总数
    private int[] mCount = new int[Question.TYPES.length];
    //练习结束标志
    private boolean isEnd = false;

    //手势
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_exercise);
        mHandler = new MyHandler(this);

        init();

        //手势监听
        gestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
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
        });
    }

    /**
     * 初始化
     */
    private void init() {
        //设置开始时间
        mStartTime = System.currentTimeMillis();
        //初始化答对/答错次数
        for (int i = 0; i < Question.TYPES.length; i++) {
            mCount[i] = mCorrectCount[i] = mIncorrectCount[i] = 0;
        }

        //获取范围,顺序
        Intent intent = getIntent();
        mQuestionbank = (QuestionBank) intent.getSerializableExtra("questionbank");
        //mQuestionArrayList = (ArrayList<Question>) intent.getSerializableExtra("questions");
        mRange = intent.getIntExtra("range", 0);
        mOrder = intent.getIntExtra("order", 0);
        //开启线程,读取符合条件的题目列表
        new Thread(new Runnable() {
            @Override
            public void run() {
                mQuestionArrayList = new Dao(mContext).queryQuestionList(mQuestionbank.getId(), mRange, mOrder);
                if (mQuestionArrayList.size() < 1) {
                    mHandler.sendMessage(mHandler.obtainMessage(ERROR_EMPTY));
                }
                mHandler.sendMessage(mHandler.obtainMessage(READY));
            }
        }).start();

        // 设置标题
        ActionBar mActionBar = getSupportActionBar();
        if (null != mActionBar) {
            mActionBar.setTitle(mQuestionbank.getTitle());
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }


        mType0Binding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.question_info_type0, null, false);
        mType1Binding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.question_info_type1, null, false);
        mType2Binding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.question_info_type2, null, false);
        mType3Binding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.question_info_type3, null, false);

    }


    /**
     * 控件数据绑定
     */
    private void bindData() {
        //获取题目
        Question question = mQuestionArrayList.get(mIndex);
        //绑定数据
        mBinding.setQuestion(question);
        //隐藏答案
        mBinding.exerciseAnswer.setVisibility(View.INVISIBLE);
        //提交按钮可用
        mBinding.exerciseSubmit.setEnabled(true);
        mBinding.exerciseSubmit.setBackgroundColor(ContextCompat.getColor(mContext, R.color.highlight));
        switch (question.getType()) {
            case 0:
                //单选题序号
                mBinding.setIndex(mIndex + 1);
                mBinding.layoutQuestionOptions.removeAllViews();
                mType0Binding.setQuestion(question);
                mBinding.layoutQuestionOptions.addView(mType0Binding.getRoot());
                //清空用户选择记录
                mType0Binding.rgQuestionInfoType0.clearCheck();
                break;
            case 1:
                //多选题序号
                mBinding.setIndex(mIndex + 1 - mCount[0]);
                mBinding.layoutQuestionOptions.removeAllViews();
                mType1Binding.setQuestion(question);
                mBinding.layoutQuestionOptions.addView(mType1Binding.getRoot());
                //清空用户选择记录
                mType1Binding.cbQuestionInfoType1OptionA.setChecked(false);
                mType1Binding.cbQuestionInfoType1OptionB.setChecked(false);
                mType1Binding.cbQuestionInfoType1OptionC.setChecked(false);
                mType1Binding.cbQuestionInfoType1OptionD.setChecked(false);
                break;
            case 2:
                //填空题序号
                mBinding.setIndex(mIndex + 1 - mCount[0] - mCount[1]);
                mBinding.layoutQuestionOptions.removeAllViews();
                mBinding.layoutQuestionOptions.addView(mType2Binding.getRoot());
                //清空用户输入
                mType2Binding.etQuestionInfoType2Blank1.setText("");
                break;
            case 3:
                //判断题序号
                mBinding.setIndex(mIndex + 1 - mCount[0] - mCount[1] - mCount[2]);
                mBinding.layoutQuestionOptions.removeAllViews();
                mBinding.layoutQuestionOptions.addView(mType3Binding.getRoot());
                //清空用户选择
                mType3Binding.rgQuestionInfoType3.clearCheck();
                break;
            default:
                break;
        }
    }

    //手势监听
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    public void doResult(int action) {
        if (isEnd) {
            //如果答题已经结束,则当前正在显示的是答题报告,不需要手势监听
            return;
        }
        switch (action) {
            case RIGHT:
                //向右划,看上一题
                if (--mIndex < 0) {
                    mIndex++;
                    Toast.makeText(mContext, getResources().getString(R.string.toast_first_question), Toast.LENGTH_SHORT).show();
                } else {
                    //重新绑定数据
                    bindData();
                }
                break;
            case LEFT:
                //向左划,看下一题
                int max = mQuestionArrayList.size() - 1;
                if (++mIndex > max) {
                    //弹出对话框询问用户练习结束,显示练习结果或返回,继续练习
                    Resources resources = getResources();
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);
                    builder.setTitle(resources.getString(R.string.exercise_over))
                            .setPositiveButton(resources.getString(R.string.exercise_analyse), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //TODO 生成本次练习报告
                                    long times = System.currentTimeMillis() - mStartTime;
                                    String exerciseTime = TimeUtils.formatTime(times);
                                    //计算正确率
                                    String[] accuracy = new String[Question.TYPES.length];
                                    for (int index = 0; index < Question.TYPES.length; index++) {
                                        if (mCount[index] > 0) {
                                            accuracy[index] = NumberFormat.getPercentInstance().format((float) mCorrectCount[index] / mCount[index]);
                                        } else {
                                            accuracy[index] = "-";
                                        }
                                    }

                                    //设置结束标志
                                    isEnd = true;

                                    //加载练习报告布局
                                    ActivityExerciseReportBinding activityExerciseReportBinding = DataBindingUtil.setContentView((ExerciseActivity) mContext, R.layout.activity_exercise_report);
                                    //绑定数据
                                    activityExerciseReportBinding.setExerciseRange(mRange);
                                    activityExerciseReportBinding.setExerciseOrder(mOrder);
                                    activityExerciseReportBinding.setExerciseTime(exerciseTime);
                                    activityExerciseReportBinding.setCorrectCount(mCorrectCount);
                                    activityExerciseReportBinding.setIncorrectCount(mIncorrectCount);
                                    activityExerciseReportBinding.setCount(mCount);
                                    activityExerciseReportBinding.setAccuracy(accuracy);
                                    activityExerciseReportBinding.exerciseExit.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            finish();
                                        }
                                    });
                                }
                            })
                            .setNegativeButton(resources.getString(R.string.exit), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            })
                            .show();
                } else {
                    //重新绑定数据
                    bindData();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.exercise_submit:
                submitAnswer();
                break;
            default:
                break;
        }
    }

    /**
     * 提交答案
     */
    private void submitAnswer() {
        //1.获取用户输入的答案
        String userAnswer = getUserAnswer();
        //2.获取正确答案
        Question question = mQuestionArrayList.get(mIndex);
        String answer = question.getAnswer();
        //3.检查答案
        Resources resources = getResources();
        if (userAnswer.equals(answer)) {
            //提示用户
            mBinding.exerciseAnswer.setText(resources.getString(R.string.answer_correct));
            //统计正确次数
            mCorrectCount[question.getType()]++;
            //更新数据库
            updateCorrect(question.getCorrect() + 1, question.getId());
        } else {
            //提示用户
            if (question.getType() == 3) {
                answer = "1".equals(answer) ? mType3Binding.rbQuestionInfoType3OptionCorrect.getText().toString() : mType3Binding.rbQuestionInfoType3OptionWrong.getText().toString();
            }
            mBinding.exerciseAnswer.setText(String.format(resources.getString(R.string.answer_incorrect), answer));
            //统计错误次数
            mIncorrectCount[question.getType()]++;
            //更新数据库
            updateWrong(question.getWrong() + 1, question.getId());
        }
        mBinding.exerciseAnswer.setVisibility(View.VISIBLE);
        //4.提交按钮不可用
        mBinding.exerciseSubmit.setEnabled(false);
        mBinding.exerciseSubmit.setBackgroundColor(ContextCompat.getColor(mContext, R.color.button_disable_bg));
    }

    /**
     * 更新:回答错误的次数
     *
     * @param wrong 错误次数
     * @param id    题目索引
     */
    private void updateWrong(final int wrong, final int id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Dao dao = new Dao(mContext);
                boolean result = dao.updateQuestionWrong(wrong, id);
                if (result) {
                    mQuestionArrayList.get(mIndex).setWrong(wrong);
                } else {
                    LogUtil.e("database", getResources().getString(R.string.db_write_update_wong_fail));
                }
            }
        }).start();
    }

    /**
     * 更新回答正确的次数
     *
     * @param correct 正确次数
     * @param id      题目索引
     */
    private void updateCorrect(final int correct, final int id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Dao dao = new Dao(mContext);
                if (dao.updateQuestionCorrect(correct, id)) {
                    mQuestionArrayList.get(mIndex).setCorrect(correct);
                } else {
                    LogUtil.e("database", getResources().getString(R.string.db_write_update_wong_fail));
                }
            }
        }).start();
    }

    /**
     * 获取用户输入的答案
     *
     * @return 用户的答案, 单选多选答案为大写, 按ABCD顺序排列;填空答案为文本;判断答案为1或0,分别代表对或错
     */
    private String getUserAnswer() {
        StringBuilder sb = new StringBuilder();
        switch (mQuestionArrayList.get(mIndex).getType()) {
            case 0:
                //单选
                switch (mType0Binding.rgQuestionInfoType0.getCheckedRadioButtonId()) {
                    case R.id.rb_question_info_type0_optionA:
                        sb.append('A');
                        break;
                    case R.id.rb_question_info_type0_optionB:
                        sb.append('B');
                        break;
                    case R.id.rb_question_info_type0_optionC:
                        sb.append('C');
                        break;
                    case R.id.rb_question_info_type0_optionD:
                        sb.append('D');
                        break;
                    default:
                        break;
                }
                break;
            case 1:
                //多选
                if (mType1Binding.cbQuestionInfoType1OptionA.isChecked()) {
                    sb.append('A');
                }
                if (mType1Binding.cbQuestionInfoType1OptionB.isChecked()) {
                    sb.append('B');
                }
                if (mType1Binding.cbQuestionInfoType1OptionC.isChecked()) {
                    sb.append('C');
                }
                if (mType1Binding.cbQuestionInfoType1OptionD.isChecked()) {
                    sb.append('D');
                }
                break;
            case 2:
                //填空
                sb.append(mType2Binding.etQuestionInfoType2Blank1.getText());
                break;
            case 3:
                //判断
                if (mType3Binding.rgQuestionInfoType3.getCheckedRadioButtonId() == R.id.rb_question_info_type3_optionCorrect) {
                    sb.append(1);
                } else {
                    sb.append(0);
                }
                break;
            default:
                break;
        }
        return sb.toString();
    }

    private static class MyHandler extends Handler {
        /*
        * Handler 类应该应该为static类型，否则有可能造成泄露。
        * 在程序消息队列中排队的消息保持了对目标Handler类的应用。
        * 如果Handler是个内部类，那 么它也会保持它所在的外部类的引用。
        * 为了避免泄露这个外部类，应该将Handler声明为static嵌套类，并且使用对外部类的弱应用。
        * */
        WeakReference<ExerciseActivity> mActivity;

        MyHandler(ExerciseActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ExerciseActivity theActivity = mActivity.get();
            switch (msg.what) {
                case ERROR_EMPTY:
                    Toast.makeText(theActivity, theActivity.getResources().getString(R.string.db_read_no_question), Toast.LENGTH_SHORT).show();
                    theActivity.finish();
                    break;
                case READY:
                    //数据准备好
                    //计算各类型题目总数
                    for (int i = 0; i < theActivity.mQuestionArrayList.size(); i++) {
                        theActivity.mCount[theActivity.mQuestionArrayList.get(i).getType()]++;
                    }
                    // 绑定数据
                    theActivity.mBinding.setCount(theActivity.mCount);
                    theActivity.bindData();
                    theActivity.mBinding.exerciseSubmit.setOnClickListener(theActivity);
                    break;
                default:
                    break;
            }
        }
    }
}
