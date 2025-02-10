package com.niimbot.jcdemo.utils;


import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.Toast;

import com.gengcon.www.jcprintersdk.JCPrintApi;
import com.gengcon.www.jcprintersdk.callback.Callback;
import com.gengcon.www.jcprintersdk.callback.PrintCallback;
import com.niimbot.jcdemo.app.MyApplication;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 打印工具类
 *
 * @author zhangbin
 */
public class PrintUtil {
    private static final String TAG = "PrintUtil";
    private static int mConnectedType = -1;
    /**
     * 单例实例，使用 volatile 保证多线程可见性和有序性
     */
    private static volatile JCPrintApi api;

    /**
     * 回调接口，用于处理打印机状态变化事件
     */
    private static final Callback CALLBACK = new Callback() {
        private static final String TAG = "PrintUtil";

        /**
         * 连接成功回调
         *
         * @param address 设备地址，蓝牙为蓝牙 MAC 地址，WIFI 为 IP 地址
         * @param type   连接类型，0 表示蓝牙连接，1 表示 WIFI 连接
         */
        @Override
        public void onConnectSuccess(String address, int type) {
            mConnectedType = type;
        }

        /**
         * 断开连接回调
         * 当设备断开连接时，将调用此方法。
         */
        @Override
        public void onDisConnect() {
            mConnectedType = -1;
        }

        /**
         * 电量变化回调
         * 当设备电量发生变化时，将调用此方法。
         *
         * @param powerLevel 电量等级，取值范围为 1 到 4，代表有 1 到 4 格电，满电是 4 格
         */
        @Override
        public void onElectricityChange(int powerLevel) {

        }

        /**
         * 监测上盖状态变化回调
         * 当上盖状态发生变化时，将调用此方法。目前该回调仅支持 H10/D101/D110/D11/B21/B16/B32/Z401/B3S/B203/B1/B18 系列打印机。
         *
         * @param coverStatus 上盖状态，0 表示上盖打开，1 表示上盖关闭
         */
        @Override
        public void onCoverStatus(int coverStatus) {

        }

        /**
         * 监测纸张状态变化
         * 当纸张状态发生变化时，将调用此方法。目前该回调仅支持H10/D101/D110/D11/B21/B16/B32/Z401/B203/B1/B18 系列打印机。
         *
         * @param paperStatus 0为不缺纸 1为缺纸
         */
        @Override
        public void onPaperStatus(int paperStatus) {

        }

        /**
         * 监测标签rfid读取状态变化
         * 当标签rfid读取状态发生变化时，将调用此方法。
         *
         * @param rfidReadStatus 0为未读取到标签RFID 1为成功读取到标签RFID 目前该回调仅支持H10/D101/D110/D11/B21/B16/B32/Z401/B203/B1/B18 系列打印机。
         */
        @Override
        public void onRfidReadStatus(int rfidReadStatus) {

        }


        /**
         * 监测碳带rfid读取状态变化
         * 当碳带rfid读取状态发生变化时，将调用此方法。
         *
         * @param ribbonRfidReadStatus 0为未读取到碳带RFID 1为成功读取到碳带RFID 目前该回调仅支持B18/B32/Z401/P1/P1S 系列打印机。
         */
        @Override
        public void onRibbonRfidReadStatus(int ribbonRfidReadStatus) {

        }

        /**
         * 监测碳带状态变化
         * 当纸张状态发生变化时，将调用此方法
         *
         * @param ribbonStatus 0为无碳带 1为有碳带 目前该回调仅支持B18/B32/Z401/P1/P1S系列打印机。
         */
        @Override
        public void onRibbonStatus(int ribbonStatus) {

        }


        /**
         * 固件异常回调，需要升级
         * 当设备连接成功但出现固件异常时，将调用此方法，表示需要进行固件升级。
         */
        @Override
        public void onFirmErrors() {

        }
    };


    /**
     * 获取 JCPrintApi 单例实例
     *
     * @return JCPrintApi 实例
     */
    public static JCPrintApi getInstance() {
        // 双重检查锁定以确保只初始化一次实例
        if (api == null) {
            synchronized (PrintUtil.class) {
                if (api == null) {
                    api = JCPrintApi.getInstance(CALLBACK);
                    //api.init已废弃，使用initSdk替代，方法名含义更准确
                    api.initSdk(MyApplication.getInstance());
                    //获取内置目录路径
                    File directory = MyApplication.getInstance().getFilesDir();
                    //获取自定义字体路径
                    File customFontDirectory = new File(directory, "custom_font");
                    api.initDefaultImageLibrarySettings(customFontDirectory.getAbsolutePath(), "");

                }
            }
        }

        return api;

    }


    /**
     * 通过打印机mac地址进行蓝牙连接开启打印机（同步）
     *
     * @param address 打印机地址
     * @return 成功与否
     */
    public static int connectBluetoothPrinter(String address) {
        // 获取单例实例以确保线程安全
        JCPrintApi localApi = getInstance();
        //api.openPrinterByAddress(address)，使用connectBluetoothPrinter替代，方法名含义更准确
        return localApi.connectBluetoothPrinter(address);
    }

    /**
     * 关闭打印机
     */
    public static void close() {
        // 获取单例实例以确保线程安全
        JCPrintApi localApi = getInstance();
        localApi.close();
    }


    public static int getConnectedType() {
        return mConnectedType;
    }

    public static void setConnectedType(int connectedType) {
        mConnectedType = connectedType;
    }

    /**
     * 检查打印机是否连接
     *
     * @return 连接状态代码
     */
    public static int isConnection() {
        // 获取单例实例以确保线程安全
        JCPrintApi localApi = getInstance();
        return localApi.isConnection();
    }


}
