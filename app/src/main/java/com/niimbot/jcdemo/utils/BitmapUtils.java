package com.niimbot.jcdemo.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;


import android.graphics.RectF;

/**
 * 图像绘制工具类，用于在Android平台上绘制图像。
 *
 * @author zhangbin
 */
public class BitmapUtils {
    private static Bitmap sBitmap;
    private static Canvas sCanvas;

    private static final int MIN_FONT_SIZE = 16;  // 最小字号
    /**
     * 旋转角度：0度
     */
    public static final int ROTATION_0 = 0;
    /**
     * 旋转角度：90度
     */
    public static final int ROTATION_90 = 90;
    /**
     * 旋转角度：180度
     */
    public static final int ROTATION_180 = 180;
    /**
     * 旋转角度：270度
     */
    public static final int ROTATION_270 = 270;

    /**
     * 对齐方式：左对齐
     */
    public static final int ALIGN_LEFT = 0;
    /**
     * 对齐方式：居中对齐
     */
    public static final int ALIGN_CENTER = 1;
    /**
     * 对齐方式：右对齐
     */
    public static final int ALIGN_RIGHT = 2;
    /**
     * 对齐方式：顶部对齐
     */
    public static final int ALIGN_TOP = 0;
    /**
     * 对齐方式：中部对齐
     */
    public static final int ALIGN_MIDDLE = 1;
    /**
     * 对齐方式：底部对齐
     */
    public static final int ALIGN_BOTTOM = 2;

    /**
     * 绘制文本框方法
     *
     * @param text                文本内容
     * @param x                   文本框左上角X坐标
     * @param y                   文本框左上角Y坐标
     * @param width               文本框宽度
     * @param height              文本框高度
     * @param horizontalAlignment 水平对齐方式
     * @param verticalAlignment   垂直对齐方式
     * @param letterSpacing       字间距
     * @param lineSpacing         行间距
     * @param textSize            字号
     */
    public static void drawText(String text, int x, int y, int width, int height,
                                int horizontalAlignment, int verticalAlignment,
                                float letterSpacing, float lineSpacing, float textSize) {

        // 检查画布是否初始化
        if (sCanvas == null || sBitmap == null) {
            throw new IllegalArgumentException("绘制文本前请先设置画布");
        }


        // 检查文本对齐方式是否合法
        if (!isValidAlignment(horizontalAlignment, verticalAlignment)) {
            throw new IllegalArgumentException("无效的对齐方式");
        }

        // 创建文本画笔
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setTextSize(textSize);
        textPaint.setLetterSpacing((float) letterSpacing);


        Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
        if (horizontalAlignment == ALIGN_CENTER) {
            alignment = Layout.Alignment.ALIGN_CENTER;
        } else if (horizontalAlignment == ALIGN_RIGHT) {
            alignment = Layout.Alignment.ALIGN_OPPOSITE;
        }
        // 创建StaticLayout来处理自动换行和行间距
        StaticLayout staticLayout = new StaticLayout(text, textPaint, width,
                alignment, 1.0f, lineSpacing, true);
        float textY;
        switch (verticalAlignment) {
            case ALIGN_MIDDLE:
                textY = y + (height - staticLayout.getHeight()) / 2f;
                break;
            case ALIGN_BOTTOM:
                textY = y + height - staticLayout.getHeight();
                break;
            default:
                textY = y;
        }


        if (staticLayout.getHeight() >= height) {

            textY = y;
        }


        // 绘制文本
        sCanvas.save();
        sCanvas.translate(x, textY);
        staticLayout.draw(sCanvas);
        sCanvas.restore();
    }


    /**
     * 页面绘制方法
     *
     * @param width    宽度
     * @param height   高度
     * @param rotation 旋转角度（0、90、180或270）
     * @throws IllegalArgumentException 如果宽度或高度不是正整数，或旋转角度不在指定范围内
     */
    public static void startPage(int width, int height, int rotation) {
        // 检查宽度和高度是否为正整数
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("宽度和高度必须为正整数");
        }

        // 检查画布旋转角度是否合法
        if (!isValidRotation(rotation)) {
            throw new IllegalArgumentException("无效的旋转角度，旋转角度必须为0、90、180或270");
        }

        // 创建一个空白的Bitmap对象
        sBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // 创建一个画布对象，并将Bitmap与画布关联起来
        sCanvas = new Canvas(sBitmap);
        sCanvas.drawColor(Color.WHITE);
        // 设置画布的旋转角度
        sCanvas.rotate(rotation, width / 2f, height / 2f);
    }


    /**
     * 结束页面绘制方法
     *
     * @return 绘制完成的位图对象
     */
    public static Bitmap endPage() {
        // 检查画布是否初始化
        if (sCanvas == null || sBitmap == null) {
            throw new IllegalArgumentException("结束绘制前，请先设置画布");
        }
        // 提取与Canvas关联的Bitmap
        Bitmap bitmap = sBitmap;

        // 绘制完成后释放Canvas资源

        sCanvas = null;
        sBitmap = null;

        return bitmap;
    }

    /**
     * 检查旋转角度是否合法
     *
     * @param rotation 旋转角度
     * @return 是否合法
     */
    private static boolean isValidRotation(int rotation) {
        return rotation == ROTATION_0 || rotation == ROTATION_90 || rotation == ROTATION_180 || rotation == ROTATION_270;
    }

    /**
     * 检查对齐方式是否合法
     *
     * @param horizontalAlignment 水平对齐方式
     * @param verticalAlignment   垂直对齐方式
     * @return 是否合法
     */
    private static boolean isValidAlignment(int horizontalAlignment, int verticalAlignment) {
        return (horizontalAlignment == ALIGN_LEFT || horizontalAlignment == ALIGN_CENTER || horizontalAlignment == ALIGN_RIGHT)
                && (verticalAlignment == ALIGN_TOP || verticalAlignment == ALIGN_MIDDLE || verticalAlignment == ALIGN_BOTTOM);
    }

    private static final int BLOCK_SIZE = 16; // 自适应阈值算法中的块大小

    public static Bitmap binarizeImage(Bitmap image) {
        // 将图像转换为灰度图
        Bitmap grayImage = toGrayScale(image);

        // 获取图像的宽度和高度
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();

        // 创建二值化后的图像
        Bitmap binaryImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // 遍历图像的每个像素块
        for (int blockY = 0; blockY < height; blockY += BLOCK_SIZE) {
            for (int blockX = 0; blockX < width; blockX += BLOCK_SIZE) {
                // 计算当前块的边界
                int blockWidth = Math.min(BLOCK_SIZE, width - blockX);
                int blockHeight = Math.min(BLOCK_SIZE, height - blockY);

                // 计算当前块的灰度平均值
                int totalGray = 0;
                for (int y = blockY; y < blockY + blockHeight; y++) {
                    for (int x = blockX; x < blockX + blockWidth; x++) {
                        totalGray += Color.red(grayImage.getPixel(x, y));
                    }
                }
                int averageGray = totalGray / (blockWidth * blockHeight);

                // 根据灰度平均值进行二值化
                for (int y = blockY; y < blockY + blockHeight; y++) {
                    for (int x = blockX; x < blockX + blockWidth; x++) {
                        int gray = Color.red(grayImage.getPixel(x, y));
                        int threshold = gray > averageGray ? 255 : 0;
                        int binary = Color.rgb(threshold, threshold, threshold);
                        binaryImage.setPixel(x, y, binary);
                    }
                }
            }
        }

        // 返回二值化后的图像
        return binaryImage;
    }

//    public static Bitmap binarizeImage(Bitmap image) {
//        // 将图像转换为灰度图
//        Bitmap grayImage = toGrayScale(image);
//
//        // 获取图像的宽度和高度
//        int width = grayImage.getWidth();
//        int height = grayImage.getHeight();
//
//        // 创建二值化后的图像
//        Bitmap binaryImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//
//        // 遍历图像的每个像素
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                // 获取当前像素的灰度值
//                int gray = Color.red(grayImage.getPixel(x, y));
//
//                // 利用反向投影算法进行二值化
//                int threshold = 255 - gray;
//                int binary = Color.rgb(threshold, threshold, threshold);
//
//                // 将二值化后的像素值设置到二值化图像中
//                binaryImage.setPixel(x, y, binary);
//            }
//        }
//
//        // 返回二值化后的图像
//        return binaryImage;
//    }

    private static Bitmap toGrayScale(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Bitmap grayImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = image.getPixel(x, y);
                int gray = (int) (Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114);
                grayImage.setPixel(x, y, Color.rgb(gray, gray, gray));
            }
        }

        return grayImage;
    }
}

