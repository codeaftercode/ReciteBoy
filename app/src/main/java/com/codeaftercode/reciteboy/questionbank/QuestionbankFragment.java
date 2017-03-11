package com.codeaftercode.reciteboy.questionbank;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codeaftercode.reciteboy.R;
import com.codeaftercode.reciteboy.bean.QuestionBank;
import com.codeaftercode.reciteboy.common.MyApplication;
import com.codeaftercode.reciteboy.common.RecyclerViewDivider;
import com.codeaftercode.reciteboy.databinding.FragmentQuestionbankBinding;
import com.codeaftercode.reciteboy.databinding.ItemQuestionbankBinding;
import com.codeaftercode.reciteboy.main.MainActivity;

import java.util.ArrayList;

/**
 * Created by codeaftercode on 2017/2/11.
 * 1.显示题库列表
 */

public class QuestionbankFragment extends Fragment {
    private Context mContext;
    private FragmentQuestionbankBinding binding;
    //适配器
    private MyAdapter mAdapter;

    public MyAdapter getAdapter() {
        if (null == mAdapter) {
            mAdapter = new MyAdapter(MyApplication.getQuestionBankArrayList());
        }
        return mAdapter;
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_questionbank, container, false);
        mContext = getContext();

        init();

        return binding.getRoot();
    }

    private void init() {
        //TODO 空数据处理
        //binding.lvQuestionbankFragment.setEmptyView(binding.tvQuestionbankFragmentEmptyView);

        //布局管理器
        binding.recyclerViewQuestionbankFragment.setLayoutManager(new LinearLayoutManager(mContext));

        //初始化适配器
        mAdapter = getAdapter();
        //绑定适配器
        binding.recyclerViewQuestionbankFragment.setAdapter(mAdapter);
        //添加水平分隔线
        binding.recyclerViewQuestionbankFragment.addItemDecoration(new RecyclerViewDivider(mContext, LinearLayoutManager.HORIZONTAL));
        //设置监听-打开活动:题库信息
        binding.recyclerViewQuestionbankFragment.addOnItemTouchListener(new RecyclerViewClickListener(mContext, binding.recyclerViewQuestionbankFragment, new RecyclerViewClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                //单击:显示题目列表
                Intent intent = new Intent(mContext, QuestionbankInfoActivity.class);
                intent.putExtra("index", position);
                ((MainActivity) mContext).startActivityForResult(intent, MainActivity.QUESTIONBANK_CHANGE);
            }

            @Override
            public void onItemLongClick(View view, int position) {
                //长按
            }
        }));
    }

    //自定义RecyclerView适配器
    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        //数据
        private ArrayList<QuestionBank> datas;
        //构造方法
        MyAdapter(ArrayList<QuestionBank> datas) {
            this.datas = datas;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ItemQuestionbankBinding binding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.item_questionbank, parent, false);
            return new MyViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.getBinding().setQuestionbank(datas.get(position));
            //当绑定的数据修改时更新视图
            //此方法必须执行在UI线程
            holder.getBinding().executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return datas.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            private ItemQuestionbankBinding binding;

            MyViewHolder(ViewDataBinding binding) {
                super(binding.getRoot());
                this.binding = (ItemQuestionbankBinding) binding;
            }

            public ItemQuestionbankBinding getBinding() {
                return binding;
            }
        }
    }


}
