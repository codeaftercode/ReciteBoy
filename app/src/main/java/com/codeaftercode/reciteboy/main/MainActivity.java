package com.codeaftercode.reciteboy.main;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.codeaftercode.reciteboy.found.FoundFragment;
import com.codeaftercode.reciteboy.R;
import com.codeaftercode.reciteboy.bean.Question;
import com.codeaftercode.reciteboy.bean.QuestionBank;
import com.codeaftercode.reciteboy.common.BaseActivity;
import com.codeaftercode.reciteboy.common.MyApplication;
import com.codeaftercode.reciteboy.databinding.ActivityMainBinding;
import com.codeaftercode.reciteboy.db.Dao;
import com.codeaftercode.reciteboy.exercise.ExerciseFragment;
import com.codeaftercode.reciteboy.me.MeFragment;
import com.codeaftercode.reciteboy.questionbank.QuestionbankFragment;
import com.codeaftercode.reciteboy.util.FileUtil;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by codeaftercode on 2017/2/12.
 */

public class MainActivity extends BaseActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {
    //用户选择目标文件成功
    private static final int SELECT_QUESTIONBANK = 1;
    //导入题库成功
    private static final int IMPORT_QUESTIONBANK_SUCCESS = 2;
    //导入题库失败
    private static final int IMPORT_QUESTIONBANK_FAIL = 3;
    //题库信息改变
    public static final int QUESTIONBANK_CHANGE = 4;



    private ActivityMainBinding binding;
    private Context mContext;

    private List<Fragment> mTabs = new ArrayList<>();
    private FragmentPagerAdapter mAdapter;
    private QuestionbankFragment mQuestionbankFragment;
    private ExerciseFragment mExerciseFragment;
    private FoundFragment mFoundFragment;
    private MeFragment mMeFragment;

    private List<ChangeColorIconWithText> mTabIndicators = new ArrayList<>();

    private Handler mHandler = new MyHandler(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mContext = this;

        initView();
        initData();
        binding.mainViewpager.setAdapter(mAdapter);
        initEvent();
    }

    /**
     * 初始化所有事件
     */
    private void initEvent() {
        binding.mainIndicatorOne.setOnClickListener(this);
        binding.mainIndicatorTwo.setOnClickListener(this);
        binding.mainIndicatorThree.setOnClickListener(this);
        binding.mainIndicatorFour.setOnClickListener(this);

        binding.mainViewpager.addOnPageChangeListener(this);
    }

    private void initData() {

    }

    private void initView() {
        //下方导航按钮
        mTabIndicators.add(binding.mainIndicatorOne);
        mTabIndicators.add(binding.mainIndicatorTwo);
        mTabIndicators.add(binding.mainIndicatorThree);
        mTabIndicators.add(binding.mainIndicatorFour);
        binding.mainIndicatorOne.setIconAlpha(1.0f);

        //4个Fragment模块
        mQuestionbankFragment = new QuestionbankFragment();
        mTabs.add(mQuestionbankFragment);

        mExerciseFragment = new ExerciseFragment();
        mTabs.add(mExerciseFragment);

        mFoundFragment = new FoundFragment();
        mTabs.add(mFoundFragment);

        mMeFragment = new MeFragment();
        mTabs.add(mMeFragment);

        mAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {

            @Override
            public int getCount() {
                return mTabs.size();
            }

            @Override
            public Fragment getItem(int position) {
                return mTabs.get(position);
            }
        };
    }

    /**
     * 显示菜单
     * @param menu 菜单资源
     * @return 操作结果
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_import:
                //引导用户选择题库文件
                selectQuestionBankFile();
                return true;
            case R.id.action_add_friend:
                Toast.makeText(this, getResources().getString(R.string.menu_addfriend), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_scan:
                Toast.makeText(this, getResources().getString(R.string.menu_scan), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_feedback:
                Toast.makeText(this, getResources().getString(R.string.menu_feedback), Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * 选择题库文件
     */
    private void selectQuestionBankFile() {
        //调用系统文件选择对话框,供用户选择文件
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("xls/xls");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.tip_select_file)), SELECT_QUESTIONBANK);
    }

    /**
     * 调用文件管理器后供用户选择文件后,此方法执行
     * 从用户选择的文件中解析题库,存入数量库
     * @param requestCode   请求码为IMPORT_QUESTIONBANK,表示调用了文件管理器借用用户选择文件
     * @param resultCode    结果码为RESULT_OK,表示用户正确选择了文件
     * @param data          携带用户选择的文件信息
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SELECT_QUESTIONBANK:
                //导入题库:用户已选择目标文件
                if (resultCode == RESULT_OK) {
                    importQuestionbank(data);
                }
                break;
            case QUESTIONBANK_CHANGE:
                //查看题库信息后返回结果
                if (resultCode == RESULT_OK){
                    //返回OK表示修改了题库信息,需要通知适配器更新数据
                    notifyDataSetChanged();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void importQuestionbank(final Intent data) {
        //显示进度条
        binding.npbMainImport.setProgress(0);
        binding.npbMainImport.setVisibility(View.VISIBLE);
        //多线程读取数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                //1.获取文件路径,文件名
                Uri uri = data.getData();
                String path = FileUtil.getPath(mContext, uri);
                String fileName = FileUtil.getFileName(path);
                updateNumberProgressBar(10);
                //binding.npbMainImport.incrementProgressBy(10);
                //封装questionBank
                QuestionBank questionBank = new QuestionBank();
                questionBank.setTitle(fileName);
                questionBank.setAddDate(getDateAndTime());
                updateNumberProgressBar(20);
                //2.解析文件,暂存入ArrayList
                ArrayList<Question> questions = FileUtil.readExcel(path);
                questionBank.setCount(questions.size());
                updateNumberProgressBar(30);
                //binding.npbMainImport.setMax(questions.size());
                //3.利用Dao向数据库插入数据
                Dao dao = new Dao(mContext);
                boolean insertResult = dao.add(questionBank, questions);
                updateNumberProgressBar(30);
                if (insertResult) {
                    //新导入了1个题库,更新MyApplication中的题库列表
                    MyApplication.getQuestionBankArrayList().add(questionBank);
                    updateNumberProgressBar(binding.npbMainImport.getMax() - binding.npbMainImport.getProgress());
                    mHandler.sendMessage(mHandler.obtainMessage(IMPORT_QUESTIONBANK_SUCCESS));
                } else {
                    mHandler.sendMessage(mHandler.obtainMessage(IMPORT_QUESTIONBANK_FAIL));
                }
            }
        }).start();
    }

    private void updateNumberProgressBar(final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.npbMainImport.incrementProgressBy(progress);
            }
        });
    }

    private String getDateAndTime() {
        //获取当前日期和时间
        Date date = new Date();
        //设置显示格式
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        //设置时区
        dateFormat.setTimeZone(TimeZone.getDefault());
        //返回结果
        return  dateFormat.format(date);
    }

    public void notifyDataSetChanged() {
        //更新题库Adapter:
        /*QuestionbankFragment.MyAdapter questionbankAdapter = mQuestionbankFragment.getAdapter();
        if (null == questionbankAdapter) {
            mQuestionbankFragment.initAdapter();
        } else {
            questionbankAdapter.notifyDataSetChanged();
            //TODO 修改名称不用通知,导入和删除分别处理
            //questionbankAdapter.notifyItemInserted();
            //questionbankAdapter.notifyItemRemoved();
        }*/
        mQuestionbankFragment.getAdapter().notifyDataSetChanged();
        //更新练习Adapter
        /*ArrayAdapter<QuestionBank> questionBankArrayAdapter = mExerciseFragment.getAdapter();
        if (null == questionBankArrayAdapter) {
            mExerciseFragment.initAdapter();
        } else {
            questionBankArrayAdapter.notifyDataSetChanged();
        }*/
        //mExerciseFragment.getAdapter().notifyDataSetChanged();
        mExerciseFragment.notifyDataSetChanged();
    }


    /**
     * 重置其他的TabIndicator的颜色
     */
    private void resetOtherTabs() {
        for (int i = 0; i < mTabIndicators.size(); i++) {
            mTabIndicators.get(i).setIconAlpha(0);
        }
    }

    /**
     * 点击Tab选项卡
     *
     * @param view 选项卡
     */
    private void clickTab(View view) {
        resetOtherTabs();

        switch (view.getId()) {
            case R.id.main_indicator_one:
                binding.mainIndicatorOne.setIconAlpha(1.0f);
                binding.mainViewpager.setCurrentItem(0, false);
                break;
            case R.id.main_indicator_two:
                binding.mainIndicatorTwo.setIconAlpha(1.0f);
                binding.mainViewpager.setCurrentItem(1, false);
                break;
            case R.id.main_indicator_three:
                binding.mainIndicatorThree.setIconAlpha(1.0f);
                binding.mainViewpager.setCurrentItem(2, false);
                break;
            case R.id.main_indicator_four:
                binding.mainIndicatorFour.setIconAlpha(1.0f);
                binding.mainViewpager.setCurrentItem(3, false);
                break;
        }
    }

    @Override
    public void onClick(View view) {
        clickTab(view);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (positionOffset > 0) {
            ChangeColorIconWithText left = mTabIndicators.get(position);
            ChangeColorIconWithText right = mTabIndicators.get(position + 1);
            left.setIconAlpha(1 - positionOffset);
            right.setIconAlpha(positionOffset);
        }
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private static class MyHandler extends Handler {
        WeakReference<MainActivity> mActivity;
        MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            MainActivity theActivity = mActivity.get();
            switch (msg.what) {
                case IMPORT_QUESTIONBANK_SUCCESS:
                    Toast.makeText(theActivity, theActivity.getResources().getString(R.string.import_success), Toast.LENGTH_SHORT).show();
                    //通知适配器更新数据
                    theActivity.notifyDataSetChanged();
                    //隐藏进度条
                    theActivity.binding.npbMainImport.setVisibility(View.GONE);
                    break;
                case IMPORT_QUESTIONBANK_FAIL:
                    Toast.makeText(theActivity, theActivity.getResources().getString(R.string.import_fail), Toast.LENGTH_SHORT).show();
                    //隐藏进度条
                    theActivity.binding.npbMainImport.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }
        }
    }
}
