package com.codeaftercode.reciteboy.found;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.codeaftercode.reciteboy.R;
import com.codeaftercode.reciteboy.databinding.FragmentFoundBinding;
import com.codeaftercode.reciteboy.util.FileUtil;

import java.io.File;


/**
 * Created by codeaftercode on 2017/2/11.
 */

public class FoundFragment extends Fragment implements View.OnClickListener {
    private Context mContext;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        FragmentFoundBinding mBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.fragment_found, container, false);
        mBinding.tvQuestionbankDemo.setOnClickListener(this);
        return mBinding.getRoot();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_questionbank_demo:
                exportQuestionbankDemo();
                break;
            default:
                break;
        }
    }

    /**
     * 导出题库模板
     */
    private void exportQuestionbankDemo() {
        final Resources resources = getResources();

        //没有存储卡，取消操作
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(mContext, resources.getString(R.string.found_insert_sdcard), Toast.LENGTH_SHORT).show();
            return;
        }

        final String desPath = Environment.getExternalStorageDirectory().getPath() + resources.getString(R.string.fount_export_destination_file);
        final File file = new File(desPath);
        if (file.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(resources.getString(R.string.common_alert))
                    .setMessage(resources.getString(R.string.found_overwrite))
                    .setPositiveButton(resources.getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            execExportQuestionbank(file);
                        }
                    })
                    .setNegativeButton(resources.getString(R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //取消操作
                            Toast.makeText(mContext, getResources().getString(R.string.found_export_cancel), Toast.LENGTH_LONG).show();
                        }
                    })
                    .setCancelable(false)
                    .show();
        } else {
            execExportQuestionbank(file);
        }
    }

    /**
     * 执行导出题为模板操作
     * @param file  目标文件
     */
    private void execExportQuestionbank(File file) {
        if (FileUtil.exportQuestionbankDemo(mContext, file)) {
            Toast.makeText(mContext, getResources().getString(R.string.found_export_success) + "\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mContext, getResources().getString(R.string.found_export_fail), Toast.LENGTH_SHORT).show();
        }
    }
}
