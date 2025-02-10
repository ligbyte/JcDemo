package com.niimbot.jcdemo.utils;

import android.Manifest;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.permissionx.guolindev.PermissionX;

import java.util.List;

/**
 * 权限工具类
 *
 * @author zhangbin
 */
public class PermissionUtils {
    private static final String TAG = "PermissionUtils";

    public static void requestPermission(FragmentActivity activity,String[] permissions){
        PermissionX.init(activity)
                .permissions(permissions)
                .request(PermissionUtils::handlePermissionResult) ;
    }


    private static void handlePermissionResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
        if (allGranted) {
            // 全部权限通过
            Log.d(TAG, "handlePermissionResult: "+grantedList.toString());
        } else {
            // 显示拒绝权限
            Log.d(TAG, "handlePermissionResult: "+deniedList.toString());
        }
    }
}
