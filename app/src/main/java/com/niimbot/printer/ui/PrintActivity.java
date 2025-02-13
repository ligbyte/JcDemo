package com.niimbot.printer.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.flyco.animation.BounceEnter.BounceTopEnter;
import com.flyco.animation.SlideExit.SlideBottomExit;
import com.flyco.animation.ZoomEnter.ZoomInEnter;
import com.flyco.animation.ZoomExit.ZoomInExit;
import com.flyco.animation.ZoomExit.ZoomOutExit;
import com.flyco.dialog.entity.DialogMenuItem;
import com.flyco.dialog.listener.OnOperItemClickL;
import com.flyco.dialog.widget.NormalListDialog;
import com.gengcon.www.jcprintersdk.callback.PrintCallback;
import com.niimbot.printer.Constant;
import com.niimbot.jcdemo.R;
import com.niimbot.printer.app.MyApplication;
import com.niimbot.printer.bean.BlueDeviceInfo;
import com.niimbot.printer.bean.Dish;
import com.niimbot.printer.utils.AssetCopier;
import com.niimbot.printer.utils.BluetoothUtils;
import com.niimbot.printer.utils.ImgUtil;
import com.niimbot.printer.utils.PrintUtil;
import com.permissionx.guolindev.PermissionX;
import com.wind.dialogtiplib.dialog_tip.TipLoadDialog;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class PrintActivity extends AppCompatActivity {
    private static final Map<Integer, String> ERROR_MESSAGES = new HashMap<>();

    private Button btnBitmapPrint;

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

    boolean hasDiscoveryBluetooth = false;

    /**
     * 打印进度loading
     */
    public TipLoadDialog tipLoadDialog;
    private ExecutorService executorService;

    Handler handler = new Handler(Looper.getMainLooper());

    private static final String USER_DEFINED = "自定义";
    private BluetoothAdapter mBluetoothAdapter;
    private Set<String> deviceList;
    private List<BlueDeviceInfo> blueDeviceList;


    private int itemPosition;
    private BlueDeviceInfo lastConnectedDevice;


    /**
     * 打印机过滤
     */
    private String printNameStart = "B3S";

    private boolean isSaveInstanceStateCalled = false;


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        isSaveInstanceStateCalled = true;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {


        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //蓝牙发现
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.v(TAG, "测试:搜索中");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device != null) {
                    @SuppressLint("MissingPermission") String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress();
                    @SuppressLint("MissingPermission") int deviceStatus = device.getBondState();


                    @SuppressLint("MissingPermission") boolean supportBluetoothType = device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC || device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL;
                    boolean supportPrintName;

                    if (TextUtils.isEmpty(printNameStart)) {
                        supportPrintName = deviceName != null;
                    } else {
                        supportPrintName = deviceName != null && deviceName.startsWith(printNameStart);
                    }


                    if (supportBluetoothType && supportPrintName) {
                        Log.d(TAG, "limebluetooth打印机 测试:打印机名称: " + deviceName + " 设备地址:" + deviceHardwareAddress + " 设备类型:" + device.getType() + "  deviceStatus: " + deviceStatus);
                        if (deviceList.add(deviceName)) {
                            Log.d(TAG, "limebluetooth打印机 测试:打印机名称 235: " + deviceName);
                            hasDiscoveryBluetooth = true;
                            dismissLoadingDialog();
                            Log.d(TAG, "limedialog dismissLoadingDialog: " + 224);
                            blueDeviceList.add(new BlueDeviceInfo(deviceName, deviceHardwareAddress, deviceStatus));
                            ArrayList<DialogMenuItem> mMenuItems = new ArrayList<>();
                            for (int i = 0; i < blueDeviceList.size(); i++) {
                                //自动重连
                                if (blueDeviceList.get(i).getConnectState() == 12) {
                                    connectPrinterByPosition(i);
                                    return;
                                }
                                mMenuItems.add(new DialogMenuItem(blueDeviceList.get(i).getDeviceName(), R.mipmap.ic_printer));
                            }

                            NormalListDialog(mMenuItems);

                        }
                    }

                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.v(TAG, "测试:开始搜索");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.v(TAG, "测试:搜索结束");
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                Log.d(TAG, "测试:配对状态改变:0 " + state);
                if (itemPosition != -1 && itemPosition < blueDeviceList.size()) {
                    Log.d(TAG, "测试:配对状态改变:1 ");
                    blueDeviceList.get(itemPosition).setConnectState(state);
                    Log.d(TAG, "测试:配对状态改变:2 ");
                    connectPrinterByPosition(itemPosition);
                }

            } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                dismissLoadingDialog();
                Log.d(TAG, "limedialog dismissLoadingDialog: " + 259);
            }
        }
    };


    private void NormalListDialog(ArrayList<DialogMenuItem> mMenuItems) {
        final NormalListDialog dialog = new NormalListDialog(PrintActivity.this, mMenuItems);
        dialog.title("请选择打印机")//
                .showAnim(new ZoomInEnter())
                .dismissAnim(new ZoomInExit())
                .show();
        dialog.setOnOperItemClickL(new OnOperItemClickL() {
            @Override
            public void onOperItemClick(AdapterView<?> parent, View view, int position, long id) {
                itemPosition = position;
                connectPrinterByPosition(position);
                dialog.dismiss();
            }
        });
    }

    private void connectPrinterByPosition(int position) {
        showLoadingDialog("连接打印机","CONNECT");
        Log.d(TAG, "limedialog showLoadingDialog: " + 283);
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        }

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // 如果有任何一个权限未被授予，则返回false
                dismissLoadingDialog();
                Log.d(TAG, "limedialog dismissLoadingDialog: " + 295);
                return ;
            }
        }

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        int connectState = blueDeviceList.get(position).getConnectState();
        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(blueDeviceList.get(position).getDeviceHardwareAddress());
        switch (connectState) {
            case Constant.NO_BOND -> executorService.submit(() -> {
                    showLoadingDialog("配对中","pairing");
                    Log.d(TAG, "limedialog showLoadingDialog: " + 309);
                Log.d(TAG, "配对: 开始");
                boolean returnValue = false;
                try {
                    returnValue = BluetoothUtils.createBond(bluetoothDevice);
                } catch (Exception e) {
                    Log.d(TAG, "闪退日志" + e.getMessage());
                }
                Log.d(TAG, "配对: 进行中:" + returnValue);

            });

            case Constant.BONDED -> executorService.submit(() -> {

                BlueDeviceInfo blueDeviceInfo = new BlueDeviceInfo(bluetoothDevice.getName(), bluetoothDevice.getAddress(), connectState);
                PrintUtil.setConnectedType(-1);
                int connectResult = PrintUtil.connectBluetoothPrinter(blueDeviceInfo.getDeviceHardwareAddress());
                Log.d(TAG, "测试：连接结果 " + connectResult);

                handler.post(() -> {
                    String hint = "";

                    switch (connectResult) {
                        case 0 -> {
                            lastConnectedDevice = blueDeviceInfo;
                            lastConnectedDevice.setConnectState(13);
                            blueDeviceList.remove(position);
                            hint = "连接成功";
                            SharedPreferences preferences = context.getSharedPreferences("printConfiguration", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            SharedPreferences connectedPrinterInfo = context.getSharedPreferences("connectedPrinterInfo", Context.MODE_PRIVATE);
                            SharedPreferences.Editor connectedPrinterInfoEditor = connectedPrinterInfo.edit();
                            String printerName = lastConnectedDevice.getDeviceName();
                            if (printerName.matches("^(B32|Z401|T8).*")) {
                                editor.putInt("printMode", 2);
                                editor.putInt("printDensity", 8);
                                editor.putFloat("printMultiple", 11.81F);
                            } else if (printerName.matches("^(M2).*")) {
                                editor.putInt("printMode", 2);
                                editor.putInt("printDensity", 3);
                                editor.putFloat("printMultiple", 11.81F);
                            } else {
                                editor.putInt("printMode", 1);
                                editor.putInt("printDensity", 3);
                                editor.putFloat("printMultiple", 8);
                            }
                            editor.apply(); //提交修改
                            connectedPrinterInfoEditor.putString("deviceName", lastConnectedDevice.getDeviceName());
                            connectedPrinterInfoEditor.putString("deviceHardwareAddress", lastConnectedDevice.getDeviceHardwareAddress());
                            connectedPrinterInfoEditor.putInt("connectState", lastConnectedDevice.getConnectState());
                            connectedPrinterInfoEditor.apply();
                            //连接成功，开始打印
                            printBitmap();
                        }
                        case -1 -> hint = "连接失败";
                        case -2 -> hint = "不支持的机型";
                        default -> {
                        }
                    }


                    if (isSaveInstanceStateCalled) {
                        // 已经调用了，不执行 DialogFragment.dismiss() 方法
                        return;
                    }



                    if (connectResult != 0) {
                        showLoadingDialog(hint,"FAIL");
                        Log.d(TAG, "limedialog showLoadingDialog: " + 379);
                    }

                });
            });
            default -> {
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void init() {

        deviceList = new HashSet<>();
        blueDeviceList = new ArrayList<>();

        btnBitmapPrint = findViewById(R.id.btn_bitmap_print);
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

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //注册广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        Log.d(TAG, "初始化:注册广播 ");
        registerReceiver(receiver, intentFilter);
        Log.d(TAG, "初始化: 注册完成");

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

        btnBitmapPrint.setOnClickListener(v -> {
            printMode = 1;
            executorService.submit(this::printBitmap);
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




    private void printBitmap() {
        if (PrintUtil.isConnection() != 0) {
            //handler.post(() -> Toast.makeText(MyApplication.getInstance(), "未连接打印机", Toast.LENGTH_SHORT).show());

            Log.d(TAG, "limepermission  测试：初始化：搜索 365 ");
            try {
                if (!mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(PrintActivity.this, "蓝牙未开启", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "limepermission   " +  370);
                } else {
                    //搜索蓝牙
                        showLoadingDialog("搜索打印机中","searching");
                        Log.d(TAG, "limedialog showLoadingDialog: " + 497);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!hasDiscoveryBluetooth){
                                    showLoadingDialog("请开启打印机","FAIL");
                                Log.d(TAG, "limedialog showLoadingDialog: " + 503);
                            }
                        }
                    },6000);
                    permissionBluetoothRequest();
                }
            }catch (Exception e){
                Log.e(TAG, "limepermission  374  " + e.getMessage());
            }

            return;
        }

        showLoadingDialog("打印中","PRINT");
        Log.d(TAG, "limedialog showLoadingDialog: " + 517);
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
                //handler.post(() -> tipsDialog.setStateStr("打印进度:已打印到第" + pageIndex + "页,第" + quantityIndex + "份"));
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
                    showLoadingDialog("打印成功","SUCCESS");
                    Log.d(TAG, "limedialog showLoadingDialog: " + 553);
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
                //handlePrintResult(tipsDialog, errorMsg);
                dismissLoadingDialog();
                Log.d(TAG, "limedialog dismissLoadingDialog: " + 572);
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
     * Gps开启状态判断
     *
     * @return Gps开启与否
     */
    public boolean isGpsOPen() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void permissionBluetoothRequest() {
        Log.d(TAG, "limepermission  permissionBluetoothRequest 486");
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        }

        PermissionX.init(PrintActivity.this)
                .permissions(permissions)
                .request(this::handleBluetoothPermissionResult);
    }

    private void handleBluetoothPermissionResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
        Log.d(TAG, "handleBluetoothPermissionResult 499");
        if (allGranted) {
            handleBluetoothAllPermissionsGranted();
        } else {
            Toast.makeText(PrintActivity.this, "权限打开失败" + deniedList, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleBluetoothAllPermissionsGranted() {
        Log.d(TAG, "limepermission  handleBluetoothAllPermissionsGranted 507");
        if (!isGpsOPen()) {
            //所有权限申请通过
            MyDialogFragment fragment = new MyDialogFragment("请开启GPS，未开启可能导致无法正常进行蓝牙搜索", 1);
            fragment.show(getSupportFragmentManager(), "GPS");
        } else {
            startBluetoothDiscovery();
        }
    }

    @SuppressLint({"MissingPermission", "NotifyDataSetChanged"})
    private void startBluetoothDiscovery() {
        Log.d(TAG, "limepermission 测试:开始搜索1 530");
        itemPosition = -1;
        //清空列表数据
        deviceList.clear();
        blueDeviceList.clear();
        Log.d(TAG, "limepermission 测试:开始搜索2 535");
        if (mBluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "limepermission 测试:开始搜索5 537");
            if (mBluetoothAdapter.cancelDiscovery()) {
                Log.d(TAG, "limepermission 测试:开始搜索6 539");

                executorService.execute(() -> {
                    try {
                        //取消后等待1s后再次搜索
                        Thread.sleep(1000);
                        mBluetoothAdapter.startDiscovery();
                        Log.d(TAG, "limepermission 测试:开始搜索7 546");

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.d(TAG, "limepermission 测试:开始搜索8 550");
                    }
                });


            }
        } else {
            Log.d(TAG, "测试:开始搜索9 557");
            mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "测试:开始搜索10 559");
        }
    }


    /**
     * 请求所需的权限
     */
    private void permissionRequest() {
        // 根据 Android 版本选择不同的权限数组
        String[] permissions = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT} : new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

        // 使用 PermissionX 请求权限，并设置回调处理函数
        PermissionX.init(PrintActivity.this).permissions(permissions).request(this::handlePermissionResult);
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

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void showLoadingDialog(String msg,String tag) {

        runOnUiThread(() -> {
        if (tipLoadDialog == null){
            tipLoadDialog = new TipLoadDialog(this);
        }
        if (tag.equals("SUCCESS")){
            tipLoadDialog.setMsgAndType(msg, TipLoadDialog.ICON_TYPE_SUCCESS).show();
        } else if (tag.equals("FAIL")){
            tipLoadDialog.setMsgAndType(msg, TipLoadDialog.ICON_TYPE_FAIL).show();
        }else {
            tipLoadDialog.setMsgAndType(msg, TipLoadDialog.ICON_TYPE_LOADING2).show();
        }

        });

    }


    private void dismissLoadingDialog() {
        runOnUiThread(() -> {


        if (tipLoadDialog != null) {
            tipLoadDialog.dismiss();
        }

        });
    }

}