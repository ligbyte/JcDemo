package com.niimbot.jcdemo.utils;

import android.bluetooth.BluetoothDevice;

import java.lang.reflect.Method;

/**
 * 蓝牙工具类，用于处理蓝牙设备配对等操作。
 * @author zhangbin
 */
public class BluetoothUtils {

    /**
     * 与设备配对
     *
     * @param btDevice 要配对的蓝牙设备
     * @return 配对是否成功
     * @throws Exception 反射调用可能引发异常
     */
    public static boolean createBond(BluetoothDevice btDevice) throws Exception {
        // 获取 BluetoothDevice 类的类对象
        Class<?> btClass = BluetoothDevice.class;

        // 获取 createBond 方法的引用
        Method createBondMethod = btClass.getMethod("createBond");
        // 调用 createBond 方法，返回配对结果
        return (Boolean) createBondMethod.invoke(btDevice);
    }


}
