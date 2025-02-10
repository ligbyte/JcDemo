package com.niimbot.jcdemo.utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetCopier {
    public static void copyAssetsToInternalStorage(Context context, String assetName, String internalDirectoryName) {
        File internalDir = new File(context.getFilesDir(), internalDirectoryName);
        File outputFile = new File(internalDir, assetName);

        // 创建内部存储的目录（如果不存在）
        if (!internalDir.exists()) {
            internalDir.mkdir();
        }

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            // 打开assets文件夹中的文件
            inputStream = context.getAssets().open(assetName);

            // 创建输出流到指定的文件
            outputStream = new FileOutputStream(outputFile);

            // 从输入流中读取数据并写入输出流
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            // 刷新输出流
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
