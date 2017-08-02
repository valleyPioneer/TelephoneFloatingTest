package com.example.telephonefloatingtest;

import android.os.Environment;

/**
 * Created by 半米阳光 on 2017/8/2.
 */

public class Constants {
    /** sd卡根目录 */
    public static final String SDCATD_PATH = Environment.getExternalStorageDirectory().toString();

    /** 危险权限请求码 */
    public static final int DANGEROUS_REQUEST_CODE = 1;

    /** 特殊权限请求码 */
    public static final int SPECIAL_REQUEST_CODE = 2;
}
