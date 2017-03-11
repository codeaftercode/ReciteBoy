package com.codeaftercode.reciteboy.questionbank;

import android.app.AlertDialog;
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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.codeaftercode.reciteboy.R;
import com.codeaftercode.reciteboy.bean.Question;
import com.codeaftercode.reciteboy.bean.QuestionBank;
import com.codeaftercode.reciteboy.common.BaseActivity;
import com.codeaftercode.reciteboy.common.MyApplication;
import com.codeaftercode.reciteboy.databinding.ActivityQuestionbankInfoBinding;
import com.codeaftercode.reciteboy.db.Dao;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by codeaftercode on 2017/2/15.
 * 题库信息
 * 1.显示题目列表(按题型分组)
 * 2.修改题库名称
 * 3.删除题库
 */

public class QuestionbankInfoActivity extends BaseActivity {
    public static final int QUESTION_CHANGE = 1;
    //加载数据成功
    private static final int QUESTIONBANK_LIST_SUCCESS = 2;
    //删除题库成功
    private static final int DELETE_QUESTIONBANK_SUCCESS = 3;
    private static final int DELETE_QUESTIONBANK_FAIL = 4;
    private static final int EDIT_QUESTIONBANK_TITLE_SUCCESS = 5;
    private static final int EDIT_QUESTIONBANK_TITLE_FAIL = 6;

    private Context mContext;
    private ActivityQuestionbankInfoBinding binding;

    //当前题库序号
    private int mIndex;
    //当前题库
    private QuestionBank mQuestionbank;
    //显示题型
    private TextView[] tvs = new TextView[Question.TYPES.length];
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            //改变上方4个TextView的外观
            setTextViewBackgroundColor(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
    //用于显示题目信息的ListView
    private ListView[] lvs = new ListView[tvs.length];
    //数据
    private ArrayList<?>[] questions = new ArrayList<?>[tvs.length];
    //适配器
    private ArrayAdapter[] adapters = new ArrayAdapter[tvs.length];
    private ActionBar mActionBar;
    private Handler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_questionbank_info);
        mContext = this;

        init();

        mActionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case QUESTION_CHANGE:
                if (resultCode == RESULT_OK) {
                    //因执行删除操作,数据发生变化
                    int adapterIndex = data.getIntExtra("adapter_index", -1);
                    int questionIndex = data.getIntExtra("question_index", -1);
                    if (adapterIndex < 0 || adapterIndex >= adapters.length || questionIndex < 0 || questionIndex >= questions[adapterIndex].size()) {
                        Toast.makeText(mContext, getResources().getString(R.string.tip_index_error), Toast.LENGTH_SHORT).show();
                    } else {
                        questions[adapterIndex].remove(questionIndex);
                        adapters[adapterIndex].notifyDataSetChanged();
                        /*if (questions[adapterIndex].size() < 1) {
                            //TODO 这种题型题目已经全部被删除
                        }*/
                    }
                }
                //通知上一个活动:题库数据更新(count-1)
                setResult(RESULT_OK);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    /**
     * 绑定适配器
     * @param index 序号
     */
    private void bindListData(final int index) {
        //适配器
        ArrayAdapter<?> questionArrayAdapter = new ArrayAdapter<>(QuestionbankInfoActivity.this, R.layout.item_qb_info_listview, questions[index]);
        adapters[index] = questionArrayAdapter;

        //ListView
        lvs[index].setAdapter(questionArrayAdapter);
        lvs[index].setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = new Intent(mContext, QuestionInfoActivity.class);
                //向下一个活动传递数据:
                //[1]当前题库在MyApplication.getQuestionBankArrayList()中的索引
                intent.putExtra("questionbank_index", mIndex);
                //[2]当前题目在 当前题库 的同一类型题中的位置
                intent.putExtra("question_index", position);
                //[3]当前题库的同类型题列表
                intent.putExtra("list", questions[index]);
                //[4]当前适配器索引(当下一活动执行了删除操作后,返回此数据,便于通知适配器更新)
                intent.putExtra("adapter_index",index );

                startActivityForResult(intent, QUESTION_CHANGE);
            }
        });
    }

    private void init() {
        //准备数据
        // [1]获取从上一个活动传递过来的数据
        mIndex = getIntent().getIntExtra("index", 0);
        // [2]提取当前页对应的题库信息
        mQuestionbank = MyApplication.getQuestionBankArrayList().get(mIndex);

        //获取控件
        mActionBar = getSupportActionBar();

        //设置标题
        mActionBar.setTitle(mQuestionbank.getTitle());

        tvs[0] = binding.tvQuestionbankType0;
        tvs[1] = binding.tvQuestionbankType1;
        tvs[2] = binding.tvQuestionbankType2;
        tvs[3] = binding.tvQuestionbankType3;

        //多线程读取各种类型题列表
        for (int i = 0; i < Question.TYPES.length; i++) {
            final int finalIndex = i;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<Question> arrayList = new Dao(mContext).queryQuestionListByType(mQuestionbank.getId(), finalIndex);
                    if (null != arrayList && arrayList.size() > 0) {
                        //读取成功
                        questions[finalIndex] = arrayList;
                        mHandler.sendMessage(mHandler.obtainMessage(QUESTIONBANK_LIST_SUCCESS, finalIndex));
                    }
                }
            }).start();

            tvs[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    binding.viewPagerQbInfo.setCurrentItem(finalIndex, false);
                }
            });

            lvs[i] = new ListView(mContext);
        }

        //设置第1个TextView外观
        setTextViewBackgroundColor(0);

        //页面适配器
        QuestionbankInfoActivity.MyViewPagerAdapter myViewPagerAdapter = new QuestionbankInfoActivity.MyViewPagerAdapter();
        binding.viewPagerQbInfo.setAdapter(myViewPagerAdapter);
        binding.viewPagerQbInfo.addOnPageChangeListener(viewPagerPageChangeListener);
    }

    /**
     * 设置上方导航按钮的背景颜色
     * @param position 位置
     */
    public void setTextViewBackgroundColor(int position) {
        for (int i = 0; i < tvs.length; i++) {
            if (position == i) {
                tvs[i].setBackgroundColor(ContextCompat.getColor(mContext, R.color.highlight));
            } else {
                tvs[i].setBackgroundColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
            }
        }
    }

    /**
     * 显示菜单
     *
     * @param menu 菜单资源
     * @return 操作结果
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.questionbank_info, menu);
        return true;
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
            case R.id.action_edit_questionbank:
                //修改题库名称
                editQuestionbankTitle();
                break;
            case R.id.action_delete_questionbank:
                //删除题库
                deleteQuestionBank();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * 删除题库
     */
    private void deleteQuestionBank() {
        //1.弹出对话框,要求用户确认删除题库
        Resources resources = getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setTitle(resources.getString(R.string.tip))
                .setMessage(resources.getString(R.string.del_questionbank_confirm))
                .setPositiveButton(resources.getString(R.string.delete), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        execDeleteQuestionBank();
                    }
                })
                .setNegativeButton(resources.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void execDeleteQuestionBank() {
        Toast.makeText(mContext, getResources().getString(R.string.delete_executing) + mQuestionbank.getTitle(), Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //利用Dao删除题库
                Dao dao = new Dao(mContext);
                Boolean delResult = dao.del(mQuestionbank);
                if (delResult) {
                    mHandler.sendMessage(mHandler.obtainMessage(DELETE_QUESTIONBANK_SUCCESS));
                } else {
                    mHandler.sendMessage(mHandler.obtainMessage(DELETE_QUESTIONBANK_FAIL));
                }
            }
        }).start();
    }

    /**
     * 修改题库名称
     */
    private void editQuestionbankTitle() {
        final EditText et = new EditText(mContext);
        et.setText(mQuestionbank.getTitle());
        et.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
        et.setSelection(et.getText().length());
        final Resources resources = getResources();
        new AlertDialog.Builder(mContext)
                .setTitle(resources.getString(R.string.edit_questionbank_name))
                .setView(et)
                .setPositiveButton(resources.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String input = et.getText().toString();
                        if ("".equals(input)) {
                            Toast.makeText(mContext, resources.getString(R.string.edit_questionbank_name_empty), Toast.LENGTH_SHORT).show();
                        } else if (!mQuestionbank.getTitle().equals(input)) {
                            //执行修改
                            execEditQuestionbankTitle(input);
                        }
                    }
                })
                .setNegativeButton(resources.getString(R.string.cancel), null)
                .show();
    }

    /**
     * 执行修改题库名称操作
     * @param input 题库名称
     */
    private void execEditQuestionbankTitle(final String input) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Dao dao = new Dao(mContext);
                int lines = dao.updateQuestionBankTitle(mQuestionbank.getId(), input);
                if (lines > 0) {
                    //如果修改数据库操作成功
                    // 更新arrayList中的数据
                    mQuestionbank.setTitle(input);
                    mHandler.sendMessage(mHandler.obtainMessage(EDIT_QUESTIONBANK_TITLE_SUCCESS));
                } else {
                    mHandler.sendMessage(mHandler.obtainMessage(EDIT_QUESTIONBANK_TITLE_FAIL));
                }
            }
        }).start();

    }

    private static class MyHandler extends Handler {
        WeakReference<QuestionbankInfoActivity> mActivity;
        @SuppressWarnings("unused")
        private MyHandler(){}
        MyHandler(QuestionbankInfoActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            QuestionbankInfoActivity theActivity = mActivity.get();
            switch (msg.what) {
                case QUESTIONBANK_LIST_SUCCESS:
                    //多线程读取题目列表成功,绑定适配器
                    final int successIndex = (int) msg.obj;
                    theActivity.bindListData(successIndex);
                    break;
                case DELETE_QUESTIONBANK_SUCCESS:
                    //删除题库成功
                    // [1]在MyApplication.QuestionBankArrayList中删除此项
                    MyApplication.getQuestionBankArrayList().remove(theActivity.mIndex);
                    // [2]通知上一个活动:题库信息发生变化,由上一个活动通知适配器数据更新
                    theActivity.setResult(RESULT_OK);
                    // [3]提示用户
                    Toast.makeText(theActivity, theActivity.getResources().getString(R.string.delete_questionbank_success), Toast.LENGTH_SHORT).show();
                    // [4]关闭本页
                    theActivity.finish();
                    break;
                case DELETE_QUESTIONBANK_FAIL:
                    //删除题库失败
                    Toast.makeText(theActivity, theActivity.getResources().getString(R.string.delete_questionbank_fail), Toast.LENGTH_SHORT).show();
                    break;
                case EDIT_QUESTIONBANK_TITLE_SUCCESS:
                    // [2]修改actionbar标题
                    theActivity.mActionBar.setTitle(theActivity.mQuestionbank.getTitle());
                    // [3]通知上一个活动:题库信息已发生变化,由上一个活动通知适配器数据更新
                    //setResult(RESULT_OK);
                    // QuestionBank类继承BaseObservable,实现了自动刷新,不需要通知适配器
                    // [4]提示用户操作结果
                    Toast.makeText(theActivity, theActivity.getResources().getString(R.string.edit_questionbank_name_success), Toast.LENGTH_SHORT).show();
                    break;
                case EDIT_QUESTIONBANK_TITLE_FAIL:
                    Toast.makeText(theActivity, theActivity.getResources().getString(R.string.edit_questionbank_name_fail), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    public class MyViewPagerAdapter extends PagerAdapter {

        MyViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(lvs[position]);
            return lvs[position];
        }

        @Override
        public int getCount() {
            return lvs.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }
}
