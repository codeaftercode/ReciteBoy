package com.codeaftercode.reciteboy.me;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codeaftercode.reciteboy.R;
import com.codeaftercode.reciteboy.databinding.FragmentMeBinding;


/**
 * Created by codeaftercode on 2017/2/11.
 */

public class MeFragment extends Fragment {
    private FragmentMeBinding binding;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.fragment_me, container, false);
        return binding.getRoot();
    }
}
