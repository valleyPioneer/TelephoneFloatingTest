package com.example.telephonefloatingtest;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.VideoView;



/**
 * Created by 半米阳光 on 2017/8/2.
 */

public class MyVideoFloatingService extends Service {
    private VideoView videoView;
    private RelativeLayout mFloatLayout;
    WindowManager windowManager;
    WindowManager.LayoutParams params;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createFloatView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        videoView.setVideoPath(Constants.SDCATD_PATH + "/floatTest/1.mp4");
        videoView.start();

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mp, int what, int extra) {
                        /** 开始渲染第一帧时再注册监听器 */
                        if(what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START){
                            /**在该事件中videoview的大小并没有改变，必须等到视频开始播放时才会自动改变 */
                            videoView.setBackground(null);
                        }
                        return true;
                    }
                });
            }
        });
        return super.onStartCommand(intent, flags, startId);
    }


    private void createFloatView(){
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        mFloatLayout = (RelativeLayout) inflater.inflate(R.layout.service_video_float,null);

        params = new WindowManager.LayoutParams();
        //获取的是WindowManagerImpl.CompatModeWrapper
        windowManager = (WindowManager)getApplication().getSystemService(getApplication().WINDOW_SERVICE);

        //设置window type
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //设置图片格式，效果为背景透明
        params.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //调整悬浮窗显示的停靠位置为左侧置顶
        params.gravity = Gravity.START | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        params.x = 0;
        params.y = 0;

        /** 设置悬浮窗口长宽数据,必须设置否则windowmanager默认全屏显示，无法响应下层的点击事件 */
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;

        windowManager.addView(mFloatLayout,params);

        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        videoView = (VideoView)mFloatLayout.findViewById(R.id.video_view);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(videoView != null)
            videoView.suspend();
        if(mFloatLayout != null)
            windowManager.removeView(mFloatLayout);

    }
}
