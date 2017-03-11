package com.codeaftercode.reciteboy.exercise;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.codeaftercode.reciteboy.R;
import com.codeaftercode.reciteboy.bean.QuestionBank;
import com.codeaftercode.reciteboy.common.MyApplication;
import com.codeaftercode.reciteboy.databinding.FragmentExerciseBinding;
import com.codeaftercode.reciteboy.db.Dao;


/**
 * Created by codeaftercode on 2017/2/11.
 */

public class ExerciseFragment extends Fragment implements View.OnClickListener {
    private Context mContext;
    private FragmentExerciseBinding binding;
    //适配器
    private ArrayAdapter<QuestionBank> mAdapter;

    public void notifyDataSetChanged() {
        if (null != mAdapter && mContext != null) {
            mAdapter.notifyDataSetChanged();
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getContext();
        binding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.fragment_exercise, container, false);
        initData();
        initEvent();

        return binding.getRoot();
    }

    private void initEvent() {
        //开始练习按钮
        binding.exerciseStart.setOnClickListener(this);
        //动态加载:选中条目后,获取该条目对应的题库信息,查询题目数量,错题数量,收藏题数量
        binding.spinnerSelectQuestionbank.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //
                final int id = MyApplication.getQuestionBankArrayList().get(i).getId();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Dao dao = new Dao(mContext);
                        binding.setCount(dao.queryQuestionCount(id, Dao.RANGE_ALL));
                        binding.setWrong(dao.queryQuestionCount(id, Dao.RANGE_WRONG));
                        binding.setCollect(dao.queryQuestionCount(id, Dao.RANGE_COLLECT));
                    }
                }).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }


    private void initData() {
        //mAdapter = getAdapter();
        mAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, MyApplication.getQuestionBankArrayList());
        //设置样式
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //加载适配器
        binding.spinnerSelectQuestionbank.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.exercise_start:
                startExercise();
                break;
        }
    }

    /**
     * 开始练习
     * 1.获取题库id,出题范围,出题顺序.同时进行检查,排除非法数据
     * 2.转入练习页
     */
    private void startExercise() {
        //1.获取题库
        QuestionBank questionBank = (QuestionBank) binding.spinnerSelectQuestionbank.getSelectedItem();
        if (null == questionBank || questionBank.getId() < 0) {
            //获取题库出错
            Toast.makeText(mContext, getResources().getString(R.string.toast_import_questionbank), Toast.LENGTH_SHORT).show();
            return;
        }

        //2.获取出题范围
        int range;
        //根据范围,获取题目列表
        switch (binding.rgSelectRange.getCheckedRadioButtonId()) {
            case R.id.rb_range_all:
                //全部
                range = Dao.RANGE_ALL;
                break;
            case R.id.rb_range_wrong:
                //错题
                if (binding.rbRangeWrong.isEnabled()) {
                    range = Dao.RANGE_WRONG;
                } else {
                    Toast.makeText(mContext, getResources().getString(R.string.toast_select_range), Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            case R.id.rb_range_favorite:
                //收藏
                if (binding.rbRangeFavorite.isEnabled()) {
                    range = Dao.RANGE_COLLECT;
                } else {
                    Toast.makeText(mContext, getResources().getString(R.string.toast_select_range), Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            default:
                Toast.makeText(mContext, getResources().getString(R.string.toast_select_range), Toast.LENGTH_SHORT).show();
                return;
        }

        //3.获取出题顺序
        int order;
        switch (binding.rgSelectOrder.getCheckedRadioButtonId()) {
            case R.id.rb_order_normal:
                //正序
                order = Dao.ORDER_NORMAL;
                break;
            case R.id.rb_order_reverse:
                //反序
                order = Dao.ORDER_REVERSE;
                break;
            case R.id.rb_order_random:
                //随机
                order = Dao.ORDER_RANDOM;
                break;
            default:
                Toast.makeText(mContext, getResources().getString(R.string.toast_select_order), Toast.LENGTH_SHORT).show();
                return;
        }

        //4.转入练习页
        Intent intent = new Intent(mContext, ExerciseActivity.class);

        //传递题库
        Bundle bundle = new Bundle();
        bundle.putSerializable("questionbank", questionBank);
        intent.putExtras(bundle);
        //传递出题范围
        intent.putExtra("range", range);
        //传递出题顺序
        intent.putExtra("order", order);
        startActivity(intent);
    }

}
