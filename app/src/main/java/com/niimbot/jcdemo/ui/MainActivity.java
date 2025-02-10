package com.niimbot.jcdemo.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.Toast;

import com.gengcon.www.jcprintersdk.callback.PrintCallback;

import com.niimbot.jcdemo.app.MyApplication;
import com.niimbot.jcdemo.bean.Dish;
import com.niimbot.jcdemo.databinding.ActivityMainBinding;
import com.niimbot.jcdemo.utils.AssetCopier;
import com.niimbot.jcdemo.utils.ImgUtil;
import com.niimbot.jcdemo.utils.PrintUtil;
import com.permissionx.guolindev.PermissionX;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 主页
 *
 * @author zhangbin
 * 2022.03.17
 */
public class MainActivity extends AppCompatActivity {
    private static final Map<Integer, String> ERROR_MESSAGES = new HashMap<>();

    static {
        ERROR_MESSAGES.put(1, "盒盖打开");
        ERROR_MESSAGES.put(2, "缺纸");
        ERROR_MESSAGES.put(3, "电量不足");
        ERROR_MESSAGES.put(4, "电池异常");
        ERROR_MESSAGES.put(5, "手动停止");
        ERROR_MESSAGES.put(6, "数据错误");
        ERROR_MESSAGES.put(7, "温度过高");
        ERROR_MESSAGES.put(8, "出纸异常");
        ERROR_MESSAGES.put(9, "正在打印");
        ERROR_MESSAGES.put(10, "没有检测到打印头");
        ERROR_MESSAGES.put(11, "环境温度过低");
        ERROR_MESSAGES.put(12, "打印头未锁紧");
        ERROR_MESSAGES.put(13, "未检测到碳带");
        ERROR_MESSAGES.put(14, "不匹配的碳带");
        ERROR_MESSAGES.put(15, "用完的碳带");
        ERROR_MESSAGES.put(16, "不支持的纸张类型");
        ERROR_MESSAGES.put(17, "纸张类型设置失败");
        ERROR_MESSAGES.put(18, "打印模式设置失败");
        ERROR_MESSAGES.put(19, "设置浓度失败");
        ERROR_MESSAGES.put(20, "写入rfid失败");
        ERROR_MESSAGES.put(21, "边距设置失败");
        ERROR_MESSAGES.put(22, "通讯异常");
        ERROR_MESSAGES.put(23, "打印机连接断开");
        ERROR_MESSAGES.put(24, "画板参数错误");
        ERROR_MESSAGES.put(25, "旋转角度错误");
        ERROR_MESSAGES.put(26, "json参数错误");
        ERROR_MESSAGES.put(27, "出纸异常(B3S)");
        ERROR_MESSAGES.put(28, "检查纸张类型");
        ERROR_MESSAGES.put(29, "RFID标签未进行写入操作");
        ERROR_MESSAGES.put(30, "不支持浓度设置");
        ERROR_MESSAGES.put(31, "不支持的打印模式");
    }

    private static final String TAG = "MainActivity";
    private static final String RB_THERMAL = "热敏";
    private ActivityMainBinding bind;
    private Context context;
    /**
     * 图像数据
     */
    private ArrayList<String> jsonList;
    /**
     * 图像处理数据
     */
    private ArrayList<String> infoList;
    /**
     * 总页数
     */
    private int pageCount;

    /**
     * 页打印份数
     */
    private int quantity;
    /**
     * 是否打印错误
     */
    private boolean isError;
    /**
     * 是否取消打印
     */
    private boolean isCancel;

    /**
     * 打印模式
     */
    private int printMode;

    /**
     * 打印浓度
     */
    private int printDensity;

    /**
     * 打印倍率（分辨率）
     */
    private Float printMultiple;

    /**
     * 打印进度loading
     */
    private MyDialogLoadingFragment fragment;
    private ExecutorService executorService;

    Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 全局变量，用于跟踪已生成的打印数据页数
     */
    private int generatedPrintDataPageCount = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bind = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(bind.getRoot());
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void init() {
        context = getApplicationContext();
        //设置自定义字体路径名称
        String customFontDirectory = "custom_font";
        //复制字体文件到内部存储
        AssetCopier.copyAssetsToInternalStorage(context, "ZT008.ttf", customFontDirectory);
        permissionRequest();
        //注册线程池
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("print_pool_%d");
            return thread;
        };

        executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());

        initPrint();
        initEvent();
    }

    private void initPrint() {
        initPrintData();
        pageCount = 0;
        quantity = 1;
        isError = false;
        isCancel = false;
    }

    private void initPrintData() {
        jsonList = new ArrayList<>();
        infoList = new ArrayList<>();
    }

    private void initEvent() {
        bind.btnConnect.setOnClickListener(v -> {
            Intent intent = new Intent(context, ConnectActivity.class);
            startActivity(intent);
        });
        bind.btnSinglePagePrint.setOnClickListener(v -> {
            printMode = bind.rbThermal.isChecked() ? 1 : 2;
            executorService.submit(() -> printLabel(1, 1));
        });

        bind.btnMultiplePagePrint.setOnClickListener(v -> {
            printMode = bind.rbThermal.isChecked() ? 1 : 2;
            executorService.submit(() -> printLabel(3, 2));

        });

        bind.btnImagePrint.setOnClickListener(v -> {
            printMode = bind.rbThermal.isChecked() ? 1 : 2;
            executorService.submit(this::printImage);

        });

        bind.btnBitmapPrint.setOnClickListener(v -> {
            printMode = bind.rbThermal.isChecked() ? 1 : 2;
            executorService.submit(this::printBitmap);
        });


        bind.rgPrintMode.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButton = findViewById(checkedId);
            String printModeOption = radioButton.getText().toString();

            SharedPreferences preferences = context.getSharedPreferences("printConfiguration", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            if (RB_THERMAL.equals(printModeOption)) {
                editor.putInt("printMode", 1);
            } else {
                editor.putInt("printMode", 2);
            }

            editor.apply();
        });

    }


    /**
     * 打印标签
     *
     * @param pages  页数（页表示数据不一样）
     * @param copies 份数
     */
    private void printLabel(int pages, int copies) {
        // 检查是否连接了打印机
        if (PrintUtil.isConnection() != 0) {
            handler.post(() -> Toast.makeText(MyApplication.getInstance(), "未连接打印机", Toast.LENGTH_SHORT).show());
            return;
        }

        handler.post(() -> {
            // 实例化加载中的 Fragment
            fragment = new MyDialogLoadingFragment("打印中");
            fragment.show(getSupportFragmentManager(), "PRINT");
        });


        // 重置错误和取消打印状态
        isError = false;
        isCancel = false;
        // 初始化打印数据
        initPrintData();
        // 在每次打印任务前初始化生成的打印数据页数
        generatedPrintDataPageCount = 0;
        // 设置打印的总页数和份数
        pageCount = pages;
        quantity = copies;
        int totalQuantity = pageCount * quantity;
        /*
         * 该方法用于设置要打印的总份数。表示所有页面的打印份数之和。
         * 例如，如果你有3页需要打印，第一页打印3份，第二页打印2份，第三页打印5份，那么总打印份数的值应为10（3+2+5）
         */
        PrintUtil.getInstance().setTotalPrintQuantity(totalQuantity);
        // 打印参数设置
        Log.d(TAG, "测试:参数设置-打印浓度： " + printDensity + "，打印模式:" + printMode);
        /*
         * 参数1：打印浓度 ，参数2:纸张类型 参数3:打印模式
         * 打印浓度 B50/B50W/T6/T7/T8 建议设置6或8，Z401/B32建议设置8，B3S/B21/B203/B1建议设置3
         */
        PrintUtil.getInstance().startPrintJob(printDensity, 1, printMode, new PrintCallback() {
            @Override
            public void onProgress(int pageIndex, int quantityIndex, HashMap<String, Object> hashMap) {
                //pageIndex为打印页码进度，quantityIndex为打印份数进度，如第二页第三份
                // 更新打印进度
                String progressMessage = "打印进度:已打印到第" + pageIndex + "页,第" + quantityIndex + "份";

                Log.d(TAG, "测试:" + progressMessage);
                handler.post(() -> {
                    fragment.setStateStr(progressMessage);
                });
                // 处理打印完成情况
                if (pageIndex == pageCount && quantityIndex == quantity) {
                    Log.d(TAG, "测试:onProgress: 结束打印");
                    //endJob，使用方法含义更明确的endPrintJob
                    if (PrintUtil.getInstance().endPrintJob()) {
                        Log.d(TAG, "结束打印成功");
                    } else {
                        Log.d(TAG, "结束打印失败");
                    }

                    handlePrintResult(fragment, "打印成功");

                }


            }


            @Override
            public void onError(int i) {

            }


            @Override
            public void onError(int errorCode, int printState) {
                Log.d(TAG, "测试：报错");
                isError = true;
                String errorMsg = ERROR_MESSAGES.getOrDefault(errorCode, "");
                handlePrintResult(fragment, errorMsg);
            }

            @Override
            public void onCancelJob(boolean isSuccess) {
                Log.d(TAG, "onCancelJob: " + isSuccess);
                isCancel = true;
            }

            /**
             * SDK缓存空闲回调，可以在此处传入打印数据
             *
             * @param pageIndex 当前回调函数处理下一页的打印索引
             * @param bufferSize 缓存空间的大小
             */
            @Override
            public void onBufferFree(int pageIndex, int bufferSize) {
                /*
                 * 1.如果未结束打印，且SDK缓存出现空闲，则自动回调该接口，此回调会上报多次，直到打印结束。
                 * 2.打印过程中，如果出现错误、取消打印，或 pageIndex 超过总页数，则返回。(此处控制代码必须得保留，否则会导致打印失败)
                 */
                if (isError || isCancel || pageIndex > pageCount) {
                    return;
                }

                Log.d(TAG, "测试-空闲数据回调-数据生成判断-总页数 " + pageCount + ",已生成页数:" + generatedPrintDataPageCount + ",空闲回调数据长度：" + bufferSize);
                // 生成打印数据
                generatePrintDataIfNeeded(bufferSize);

            }
        });

    }

    /**
     * 处理打印结果，根据传入的文本消息显示相应的提示信息。
     *
     * @param fragment 要关闭的相关界面的 Fragment 实例。
     * @param message  要显示的文本消息，可以是打印完成消息或错误消息。
     */
    private void handlePrintResult(MyDialogLoadingFragment fragment, String message) {
        handler.post(() -> {
            if (fragment != null) {
                fragment.dismiss();
            }
            Toast.makeText(MyApplication.getInstance(), message, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 根据需要生成打印数据，确保不超过总页数限制。
     *
     * @param bufferSize 当前缓存空间的大小。
     */
    private void generatePrintDataIfNeeded(int bufferSize) {
        // 如果已生成的打印数据页数小于总页数，则继续生成
        if (generatedPrintDataPageCount < pageCount) {
            // 计算本次要生成的数据长度，以免超过总页数
            int commitDataLength = Math.min((pageCount - generatedPrintDataPageCount), bufferSize);
            // 生成数据
            generateMultiPagePrintData(generatedPrintDataPageCount, generatedPrintDataPageCount + commitDataLength);
            // 提交打印数据
            PrintUtil.getInstance().commitData(jsonList.subList(generatedPrintDataPageCount, generatedPrintDataPageCount + commitDataLength), infoList.subList(generatedPrintDataPageCount, generatedPrintDataPageCount + commitDataLength));
            // 更新已生成的打印数据页数
            generatedPrintDataPageCount += commitDataLength;
        }
    }

    /**
     * 生成多页的打印数据。
     *
     * @param index      起始索引，生成数据的起始页。
     * @param cycleIndex 结束索引，生成数据的结束页。
     */
    private void generateMultiPagePrintData(int index, int cycleIndex) {
        while (index < cycleIndex) {
            // 设置打印参数
            float width = 70;
            float height = 50;
            int orientation = 90;
            float marginX = 2.0F;
            float marginY = 2.0F;
            //矩形框类型
            float rectangleWidth = width - marginX * 2;
            float rectangleHeight = height - marginY * 2;
            float lineWidth = 0.5F;
            //1.圆 2.椭圆 3.矩形 4.圆角矩形
            int graphType = 3;
            float lineHeight = rectangleHeight / 5.0F;
            float titleWidth = rectangleWidth * 2 / 5.0F;
            float contentWidth = rectangleWidth * 3 / 5.0F;
            float fontSize = 3.0F;
            // 设置初始偏移量
            float offsetY = 0F;
            float offsetX = 0F;
            // 计算绘制线条的 y 坐标
            float secondLineY = marginY + lineHeight * 2 - lineWidth + offsetY;
            float thirdLineY = marginY + lineHeight * 3 - lineWidth + offsetY;
            float fourthLineY = marginY + lineHeight * 4 - lineWidth + offsetY;
            List<String> fonts = new ArrayList<>();
            fonts.add("ZT008.ttf");
            // 设置画布大小
            PrintUtil.getInstance().drawEmptyLabel(width, height, orientation, fonts);
            //绘制图形
            PrintUtil.getInstance().drawLabelGraph(marginX + offsetX, marginY + offsetY, rectangleWidth, rectangleHeight, graphType, 0, 2, lineWidth, 1, new float[]{0.7575f, 0.7575f});

            //绘制线条
            PrintUtil.getInstance().drawLabelLine(marginX + offsetX, marginY + lineHeight - lineWidth + offsetY, rectangleWidth, lineWidth, 0, 1, new float[]{});
            PrintUtil.getInstance().drawLabelLine(marginX + offsetX, secondLineY, rectangleWidth, lineWidth, 0, 1, new float[]{});
            PrintUtil.getInstance().drawLabelLine(marginX + offsetX, thirdLineY, rectangleWidth, lineWidth, 0, 1, new float[]{});
            PrintUtil.getInstance().drawLabelLine(marginX + offsetX, fourthLineY, rectangleWidth, lineWidth, 0, 1, new float[]{});
            PrintUtil.getInstance().drawLabelLine(marginX + titleWidth - lineWidth + offsetX, marginY + lineHeight + offsetY, lineWidth, rectangleHeight - lineHeight, 0, 1, new float[]{});

            //绘制大标题，换行模式使用6，宽高固定，内容过大时缩放（区别于模式1的地方在于 文字内容按预算字号，预算文本框宽度排版后未超出预设高度时，不会放大文字，而是按照预设对齐方式将文字对齐文本框）
            PrintUtil.getInstance().drawLabelText(marginX * 3 + offsetX, marginY + offsetY, rectangleWidth - marginX * 4, lineHeight, "武汉精臣智慧标识科技有限公司", "ZT008", fontSize * 1.5F, 0, 1, 1, 6, 0, 1, new boolean[]{false, false, false, false});
            // 绘制小标题
            PrintUtil.getInstance().drawLabelText(marginX * 2.5f + offsetX, marginY + lineHeight - lineWidth + offsetY, titleWidth - marginX * 3, lineHeight, "型号", "宋体", fontSize, 0, 1, 1, 6, 0, 1, new boolean[]{false, false, false, false});
            PrintUtil.getInstance().drawLabelText(marginX * 2.5f + offsetX, secondLineY, titleWidth - marginX * 3, lineHeight, "资产编号", "宋体", fontSize, 0, 1, 1, 6, 0, 1, new boolean[]{false, false, false, false});
            PrintUtil.getInstance().drawLabelText(marginX * 2.5f + offsetX, thirdLineY, titleWidth - marginX * 3, lineHeight, "启用日期", "宋体", fontSize, 0, 1, 1, 6, 0, 1, new boolean[]{false, false, false, false});
            PrintUtil.getInstance().drawLabelText(marginX * 2.5f + offsetX, fourthLineY, titleWidth - marginX * 3, lineHeight, "存放地点", "宋体", fontSize, 0, 1, 1, 6, 0, 1, new boolean[]{false, false, false, false});

            PrintUtil.getInstance().drawLabelText(marginX * 2.5f + titleWidth + offsetX, marginY + lineHeight - lineWidth + offsetY, contentWidth - marginX * 3, lineHeight, "DELL显示器 E6540", "宋体", fontSize, 0, 0, 1, 6, 0, 1, new boolean[]{false, false, false, false});
            PrintUtil.getInstance().drawLabelText(marginX * 2.5f + titleWidth + offsetX, secondLineY, contentWidth - marginX * 3, lineHeight, "C212004", "宋体", fontSize, 0, 0, 1, 6, 0, 1, new boolean[]{false, false, false, false});
            PrintUtil.getInstance().drawLabelText(marginX * 2.5f + titleWidth + offsetX, thirdLineY, contentWidth - marginX * 3, lineHeight, "2014-06-10", "宋体", fontSize, 0, 0, 1, 6, 0, 1, new boolean[]{false, false, false, false});
            PrintUtil.getInstance().drawLabelText(marginX * 2.5f + titleWidth + offsetX, fourthLineY, contentWidth - marginX * 3, lineHeight, (index + 1) + "号办公室", "宋体", fontSize, 0, 0, 1, 6, 0, 1, new boolean[]{false, false, false, false});


            //生成打印数据
            byte[] jsonByte = PrintUtil.getInstance().generateLabelJson();

            //转换为jsonStr
            String jsonStr = new String(jsonByte);


            jsonList.add(jsonStr);
            //除B32/Z401/T8的printMultiple为11.81，其他的为8
            String jsonInfo = "{  " + "\"printerImageProcessingInfo\": " + "{    " + "\"orientation\":" + orientation + "," + "   \"margin\": [      0,      0,      0,      0    ], " + "   \"printQuantity\": " + quantity + ",  " + "  \"horizontalOffset\": 0,  " + "  \"verticalOffset\": 0,  " + "  \"width\":" + width + "," + "   \"height\":" + height + "," + "\"printMultiple\":" + printMultiple + "," + "  \"epc\": \"\"  }}";
            infoList.add(jsonInfo);

            index++;
        }
    }

    /**
     * 图片单页打印
     */
    private void printImage() {


        if (PrintUtil.isConnection() != 0) {
            handler.post(() -> Toast.makeText(MyApplication.getInstance(), "未连接打印机", Toast.LENGTH_SHORT).show());
            return;
        }
        fragment = new MyDialogLoadingFragment("打印中");
        fragment.show(getSupportFragmentManager(), "PRINT");

        //重置错误状态变量
        isError = false;
        //重置取消打印状态变量
        isCancel = false;
        //清除数据
        initPrintData();
        //宽高单位mm，图片尺寸为毫米x倍率=像素尺寸
        float width = 40;
        float height = 20;
        int orientation = 0;
        pageCount = 1;
        quantity = 1;
        //除B32/Z401/T8的printMultiple（倍率）为11.81，其他的为8
        String jsonInfo = "{  " + "\"printerImageProcessingInfo\": " + "{    " + "\"orientation\":" + orientation + "," + "   \"margin\": [      0,      0,      0,      0    ], " + "   \"printQuantity\": " + quantity + ",  " + "  \"horizontalOffset\": 0,  " + "  \"verticalOffset\": 0,  " + "  \"width\":" + width + "," + "   \"height\":" + height + "," + "\"printMultiple\":" + printMultiple + "," + "  \"epc\": \"\"  }}";

        infoList.add(jsonInfo);
        //设置画布大小
        PrintUtil.getInstance().drawEmptyLabel(width, height, orientation, "");
        String imageData = getJson(MainActivity.this, "image.json").replace("\"", "");
        PrintUtil.getInstance().drawLabelImage(imageData, 0, 0, width, height, 0, 1, 127);
        //生成打印数据
        byte[] jsonByte = PrintUtil.getInstance().generateLabelJson();
        //转换为jsonStr
        String jsonStr = new String(jsonByte);
        jsonList.add(jsonStr);


        int totalQuantity = pageCount * quantity;
        /*
         * 该方法用于设置要打印的总份数。表示所有页面的打印份数之和。
         * 例如，如果你有3页需要打印，第一页打印3份，第二页打印2份，第三页打印5份，那么总打印份数的值应为10（3+2+5）
         */
        PrintUtil.getInstance().setTotalPrintQuantity(totalQuantity);
        /*
         * 参数1：打印浓度 ，参数2:纸张类型 参数3:打印模式
         * 打印浓度 B50/B50W/T6/T7/T8 建议设置6或8，Z401/B32建议设置8，B3S/B21/B203/B1建议设置3
         */
        PrintUtil.getInstance().startPrintJob(printDensity, 1, printMode, new PrintCallback() {
            @Override
            public void onProgress(int pageIndex, int quantityIndex, HashMap<String, Object> hashMap) {
                //pageIndex为打印页码进度，quantityIndex为打印份数进度，如第二页第三份
                handler.post(() -> fragment.setStateStr("打印进度:已打印到第" + pageIndex + "页,第" + quantityIndex + "份"));
                Log.d(TAG, "打印进度:已打印到第: " + pageIndex);
                //打印进度回调
                if (pageIndex == pageCount && quantityIndex == quantity) {
                    Log.d(TAG, "测试:onProgress: 结束打印");
                    //endJob，使用方法含义更明确的endPrintJob
                    if (PrintUtil.getInstance().endPrintJob()) {
                        Log.d(TAG, "结束打印成功");
                    } else {
                        Log.d(TAG, "结束打印失败");
                    }

                    handlePrintResult(fragment, "打印成功");

                }


            }

            @Override
            public void onError(int i) {

            }


            @Override
            public void onError(int errorCode, int printState) {
                Log.d(TAG, "测试:onError");
                isError = true;
                String errorMsg = ERROR_MESSAGES.getOrDefault(errorCode, "");
                handlePrintResult(fragment, errorMsg);

            }

            @Override
            public void onCancelJob(boolean isSuccess) {
                //取消打印成功回调
                isCancel = true;
            }

            @Override
            public void onBufferFree(int pageIndex, int bufferSize) {
                /*
                 * 1.如果未结束打印，且SDK缓存出现空闲，则自动回调该接口，此回调会上报多次，直到打印结束。
                 * 2.打印过程中，如果出现错误、取消打印，或 pageIndex 超过总页数，则返回。(此处控制代码必须得保留，否则会导致打印失败)
                 */
                if (isError||isCancel||pageIndex > pageCount) {
                    return;
                }



                //pageIndex下一页的打印索引，bufferSize缓存控件
                PrintUtil.getInstance().commitData(jsonList, infoList);


            }
        });


    }

    private void printBitmap() {
        if (PrintUtil.isConnection() != 0) {
            handler.post(() -> Toast.makeText(MyApplication.getInstance(), "未连接打印机", Toast.LENGTH_SHORT).show());
            return;
        }

        fragment = new MyDialogLoadingFragment("打印中");
        fragment.show(getSupportFragmentManager(), "PRINT");


        //重置错误状态变量
        isError = false;
        //重置取消打印状态变量
        isCancel = false;

        int orientation = 0;
        pageCount = 1;
        quantity = 1;
        final int[] generatedPrintDataPageCount = {0};
        int totalQuantity = pageCount * quantity;
        /*
         * 该方法用于设置要打印的总份数。表示所有页面的打印份数之和。
         * 例如，如果你有3页需要打印，第一页打印3份，第二页打印2份，第三页打印5份，那么总打印份数的值应为10（3+2+5）
         */
        PrintUtil.getInstance().setTotalPrintQuantity(totalQuantity);
        /*
         * 参数1：打印浓度 ，参数2:纸张类型 参数3:打印模式
         * 打印浓度 B50/B50W/T6/T7/T8 建议设置6或8，Z401/B32建议设置8，B3S/B21/B203/B1建议设置3
         */
        PrintUtil.getInstance().startPrintJob(printDensity, 3, printMode, new PrintCallback() {
            @Override
            public void onProgress(int pageIndex, int quantityIndex, HashMap<String, Object> hashMap) {
                //pageIndex为打印页码进度，quantityIndex为打印份数进度，如第二页第三份
                handler.post(() -> fragment.setStateStr("打印进度:已打印到第" + pageIndex + "页,第" + quantityIndex + "份"));
                Log.d(TAG, "测试：打印进度:已打印到第: " + pageIndex);
                //打印进度回调
                if (pageIndex == pageCount && quantityIndex == quantity) {
                    Log.d(TAG, "测试:onProgress: 结束打印");
                    //endJob已废弃，使用方法含义更明确的endPrintJob
                    if (PrintUtil.getInstance().endPrintJob()) {
                        Log.d(TAG, "结束打印成功");
                    } else {
                        Log.d(TAG, "结束打印失败");
                    }


                    handlePrintResult(fragment, "打印成功");
                }


            }

            @Override
            public void onError(int i) {

            }


            @Override
            public void onError(int errorCode, int printState) {
                Log.d(TAG, "测试:onError");
                isError = true;
                String errorMsg = ERROR_MESSAGES.getOrDefault(errorCode, "");
                handlePrintResult(fragment, errorMsg);
            }

            @Override
            public void onCancelJob(boolean isSuccess) {
                //取消打印成功回调
                isCancel = true;
            }

            @Override
            public void onBufferFree(int pageIndex, int bufferSize) {
                /*
                 * 1.如果未结束打印，且SDK缓存出现空闲，则自动回调该接口，此回调会上报多次，直到打印结束。
                 * 2.打印过程中，如果出现错误、取消打印，或 pageIndex 超过总页数，则返回。(此处控制代码必须得保留，否则会导致打印失败)
                 */
                if (isError || isCancel || pageIndex > pageCount) {
                    return;
                }


                if (generatedPrintDataPageCount[0] < pageCount) {
                    ArrayList<Dish> dishList = new ArrayList<>();
                    dishList.add(new Dish("辣椒炒肉", "中辣", 29.9, 1));
                    dishList.add(new Dish("土豆牛腩", "中辣", 49.9, 1));

                    Bitmap bitmap = ImgUtil.Companion.generatePosReceiptImage(dishList);
                    int bitmapWidth = bitmap.getWidth();
                    int bitmapHeight = bitmap.getHeight();
                    PrintUtil.getInstance().commitImageData(orientation, bitmap, (int) (bitmapWidth / printMultiple), (int) (bitmapHeight / printMultiple), 1, 0, 0, 0, 0, "");


                }


            }
        });


    }

    /**
     * 从 Assets 文件夹中读取 JSON 文件内容并返回字符串形式
     *
     * @param context  上下文对象
     * @param fileName JSON 文件名
     * @return JSON 文件内容字符串
     */
    public String getJson(Context context, String fileName) {
        // 用于存储读取到的 JSON 内容
        StringBuilder stringBuilder = new StringBuilder();
        try {
            // 获取 AssetManager 对象
            AssetManager assetManager = context.getAssets();
            // 使用 try-with-resources 以确保资源在使用后自动关闭
            try (BufferedReader bf = new BufferedReader(new InputStreamReader(assetManager.open(fileName)))) {
                String line;
                // 逐行读取 JSON 文件内容
                while ((line = bf.readLine()) != null) {
                    stringBuilder.append(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 返回读取到的 JSON 内容字符串
        return stringBuilder.toString();
    }

    /**
     * 请求所需的权限
     */
    private void permissionRequest() {
        // 根据 Android 版本选择不同的权限数组
        String[] permissions = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT} : new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

        // 使用 PermissionX 请求权限，并设置回调处理函数
        PermissionX.init(MainActivity.this).permissions(permissions).request(this::handlePermissionResult);
    }

    private void handlePermissionResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
        if (allGranted) {
            handleAllPermissionsGranted();
        } else {
            handler.post(() -> showPermissionFailedToast(deniedList));
        }
    }

    /**
     * 处理所有权限已被授予的情况
     */
    private void handleAllPermissionsGranted() {
        if (!isGpsEnabled(context)) {
            handler.post(this::showGpsEnableDialog);
        }
    }

    /**
     * 显示权限请求失败的 Toast
     *
     * @param deniedList 被拒绝的权限列表
     */
    private void showPermissionFailedToast(List<String> deniedList) {
        Toast.makeText(this, "权限打开失败" + deniedList, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示提示用户开启GPS的对话框
     */
    private void showGpsEnableDialog() {
        String message = "请开启GPS，未开启可能导致无法正常进行蓝牙搜索";
        int dialogType = 1;
        MyDialogFragment fragment = new MyDialogFragment(message, dialogType);
        fragment.show(getSupportFragmentManager(), "GPS");
    }

    /**
     * 检查GPS是否已开启
     *
     * @param context 上下文对象
     * @return 若GPS已开启则返回true，否则返回false
     */
    public boolean isGpsEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = context.getSharedPreferences("printConfiguration", Context.MODE_PRIVATE);
        printMode = preferences.getInt("printMode", 1);
        printDensity = preferences.getInt("printDensity", 3);
        //除B32/Z401/T8的printMultiple为11.81，其他的为8
        printMultiple = preferences.getFloat("printMultiple", 8.0F);
        if (printMode == 1) {
            bind.rbThermal.setChecked(true);
        } else {
            bind.rbThermalTransfer.setChecked(true);
        }
    }
}