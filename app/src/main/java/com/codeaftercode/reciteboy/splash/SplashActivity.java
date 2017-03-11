package com.codeaftercode.reciteboy.splash;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;

import com.codeaftercode.reciteboy.R;
import com.codeaftercode.reciteboy.common.MyApplication;
import com.codeaftercode.reciteboy.main.MainActivity;
import com.codeaftercode.reciteboy.util.PrefManager;
import com.codeaftercode.reciteboy.welcome.WelcomeActivity;

import java.lang.ref.WeakReference;

/**
 * Created by codeaftercode on 2017/2/10.
 * 欢迎页
 * 1.判断是否第一次打开应用,是则直接转入引导页
 * 2.加载动画
 * 3.转入主页
 */

public class SplashActivity extends Activity {
    //初始化成功
    private static final int INIT_SUCCESS = 200;
    private Handler mHandler = new MyHandler(this);
    private Context mContext;
    //动画时间:2秒
    private long mAnimationTime = 2000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        //取消系统标题栏
        //supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //在setContentView()前检查是否第一次运行
        PrefManager prefManager = new PrefManager(this);
        if(prefManager.isFirstTimeLaunch()){
            //修改首次运行标志,重定向至引导页
            prefManager.setFirstTimeLaunch(false);
            redirectToWelcome();
            return;
        }

        //读取题库列表
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                MyApplication.initQuestionBankArrayList(mContext);
                //等待动画结束
                long time = mAnimationTime - System.currentTimeMillis() + startTime;
                if (time > 0) {
                    try {
                        Thread.sleep(time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mHandler.sendMessage(mHandler.obtainMessage(INIT_SUCCESS));
            }
        }).start();

        final View view = View.inflate(this, R.layout.activity_splash, null);
        setContentView(view);


        //渐变展示启动屏
        AlphaAnimation aa = new AlphaAnimation(0.3f,1.0f);
        aa.setDuration(mAnimationTime);
        view.startAnimation(aa);
    }

    /**
     * 跳转到引导页
     */
    private void redirectToWelcome(){
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }

    /**
     * 跳转到主页
     */
    private void launchHomeScreen(){
        startActivity(new Intent(this,MainActivity.class));
        finish();
    }

    private static class MyHandler extends Handler {
        WeakReference<SplashActivity> mActivity;
        MyHandler(SplashActivity activity) {
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            SplashActivity theActivity = mActivity.get();
            switch (msg.what) {
                case INIT_SUCCESS:
                    theActivity.launchHomeScreen();
                    break;

                default:
                    break;
            }
        }
    }
}


