package com.example.telephonefloatingtest;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MyPhoneReceiver myPhoneReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initViews();
    }

    private void findViews(){

    }

    private void registerBroadReceiver(){
        myPhoneReceiver = new MyPhoneReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(myPhoneReceiver,intentFilter);
    }

    private void initViews(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
             checkSpecialPermission();
        }
        else{
            /** 最好不要申请不存在的权限 */
            List<String> list = new ArrayList<>();
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                list.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED)
                list.add(Manifest.permission.READ_PHONE_STATE);
            String[] permissons = list.toArray(new String[list.size()]);
            ActivityCompat.requestPermissions(this,permissons,Constants.DANGEROUS_REQUEST_CODE);
        }
    }

    private void checkSpecialPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(!Settings.canDrawOverlays(this)){
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent,Constants.SPECIAL_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Constants.SPECIAL_REQUEST_CODE){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(!Settings.canDrawOverlays(this))
                    Toast.makeText(this,"悬浮窗权限被禁用！",Toast.LENGTH_SHORT).show();
                else
                    registerBroadReceiver();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == Constants.DANGEROUS_REQUEST_CODE){
            boolean allGranted = true;
            for(int i = 0; i < grantResults.length;i++){
                if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                    allGranted = false;
                    break;
                }
            }
            if (allGranted){
                checkSpecialPermission();
            }
            else
                Toast.makeText(this,"读写存储权限或者监听电话状态权限已经被禁用！",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(myPhoneReceiver != null)
            unregisterReceiver(myPhoneReceiver);
    }


    /** 自定义广播接收器 */
    class MyPhoneReceiver extends BroadcastReceiver {
        private boolean incomingFlag = false;
        private boolean outgoingFlag = false;
        private ServiceHelper serviceHelper;
        private TelephonyManager telephonyManager;
        private int previousState = -1;

        @Override
        public void onReceive(final Context context, Intent intent) {
            /** 去电逻辑,小米系统接收不到此广播，而华为可以 */
            if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
                outgoingFlag = true;
                Log.i("PhoneTest","outgoing call!!!");
                serviceHelper = new ServiceHelper() {
                    @Override
                    public void operateService() {
                        Intent startIntent = new Intent(context, MyVideoFloatingService.class);
                        context.startService(startIntent);
                    }
                };
                serviceHelper.operateService();
            }
            /** 来电逻辑 */
            else {
                Log.i("PhoneTest","receive a call!!!");
                if(serviceHelper == null)
                    serviceHelper = new ServiceHelper() {
                        @Override
                        public void operateService() {
                            Intent startIntent = new Intent(context, MyVideoFloatingService.class);
                            context.startService(startIntent);
                        }
                    };
                serviceHelper.operateService();

                if(telephonyManager == null){
                    telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
                    telephonyManager.listen(new MyPhoneStateListener(context), PhoneStateListener.LISTEN_CALL_STATE);
                }
            }
        }

        /***
         * 继承PhoneStateListener类，我们可以重新其内部的各种监听方法
         *然后通过手机状态改变时，系统自动触发这些方法来实现我们想要的功能
         */
        class MyPhoneStateListener extends PhoneStateListener {
            private Context mContext;

            public MyPhoneStateListener(Context context) {
                mContext = context;
            }

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.i("PhoneTest","ringing");
                        incomingFlag = true;
                        serviceHelper = new ServiceHelper() {
                            @Override
                            public void operateService() {
                                Intent startIntent = new Intent(mContext, MyVideoFloatingService.class);
                                mContext.startService(startIntent);
                            }
                        };
                        previousState = TelephonyManager.CALL_STATE_RINGING;
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.i("PhoneTest","offhook");
                        if (incomingFlag) {
                            incomingFlag = false;
                            serviceHelper = new ServiceHelper() {
                                @Override
                                public void operateService() {
                                    Intent stopIntent = new Intent(mContext, MyVideoFloatingService.class);
                                    mContext.stopService(stopIntent);
                                }
                            };
                        }
                        else if(outgoingFlag || previousState == TelephonyManager.CALL_STATE_IDLE){
                            outgoingFlag = false;
                            serviceHelper = new ServiceHelper() {
                                @Override
                                public void operateService() {
                                    Intent startIntent = new Intent(mContext, MyVideoFloatingService.class);
                                    mContext.startService(startIntent);
                                }
                            };
                        }
                        previousState = TelephonyManager.CALL_STATE_OFFHOOK;
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.i("PhoneTest","idle");
                        if(incomingFlag || outgoingFlag
                                || previousState == TelephonyManager.CALL_STATE_IDLE
                                || previousState == TelephonyManager.CALL_STATE_OFFHOOK){
                            incomingFlag = false;
                            serviceHelper = new ServiceHelper() {
                                @Override
                                public void operateService() {
                                    Intent stopIntent = new Intent(mContext, MyVideoFloatingService.class);
                                    mContext.stopService(stopIntent);
                                }
                            };
                        }
                        previousState = TelephonyManager.CALL_STATE_IDLE;
                        break;
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        }

    }

    interface ServiceHelper {
        void operateService();
    }

}
