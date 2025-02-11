package com.niimbot.jcdemo.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.niimbot.jcdemo.Constant;
import com.niimbot.jcdemo.R;
import com.niimbot.jcdemo.adapter.BlueDeviceAdapter;
import com.niimbot.jcdemo.app.MyApplication;
import com.niimbot.jcdemo.bean.BlueDeviceInfo;
import com.niimbot.jcdemo.ui.MyDialogFragment;
import com.niimbot.jcdemo.ui.MyDialogLoadingFragment;
import com.niimbot.jcdemo.ui.MyDialogWifiSetFragment;
import com.niimbot.jcdemo.utils.BluetoothUtils;
import com.niimbot.jcdemo.utils.PrintUtil;
import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class BluetoothConnectFragment extends Fragment {
    private Context context;
    private static final String USER_DEFINED = "自定义";
    private static final String TAG = "limeBluetoothConnectFragment";
    private ExecutorService executorService;
    private BluetoothAdapter mBluetoothAdapter;
    private Set<String> deviceList;
    private BlueDeviceAdapter blueDeviceAdapter;
    private List<BlueDeviceInfo> blueDeviceList;

    private MyDialogLoadingFragment fragment;
    private MyDialogWifiSetFragment dialogWifiSetFragment;

    private BlueDeviceAdapter.OnItemClickListener itemClickListener;
    private int itemPosition;
    private BlueDeviceInfo lastConnectedDevice;


    private SpinKitView spinKit;
    private RecyclerView rvDeviceList;
    private TextView tvConnected;
    private TextView tvName;
    private TextView tvAddress;
    private TextView tvStatus;

    private TextView tvWifiConfigure;
    private ConstraintLayout clConnected;

    private ConstraintLayout clSearch;

    private EditText etModel;

    /**
     * 打印机过滤
     */
    private String printNameStart = "B3S";
    Handler handler = new Handler(Looper.getMainLooper());

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


                    Log.d(TAG, "limebluetooth打印机  测试:打印机名称- " + deviceName + "，设备地址:" + deviceHardwareAddress + " 设备类型:" + device.getType());

                    if (USER_DEFINED.equals(printNameStart)) {
                        printNameStart = etModel.getText().toString().trim();
                    }

                    if (TextUtils.isEmpty(printNameStart)) {
                        supportPrintName = deviceName != null;
                    } else {
                        supportPrintName = deviceName != null && deviceName.startsWith(printNameStart);
                    }


                    if (supportBluetoothType && supportPrintName) {
                        if (deviceList.add(deviceName)) {
                            blueDeviceList.add(new BlueDeviceInfo(deviceName, deviceHardwareAddress, deviceStatus));
                            blueDeviceAdapter.notifyItemInserted(blueDeviceList.size());
                        }
                    }

                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.v(TAG, "测试:开始搜索");
                spinKit.setVisibility(View.VISIBLE);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.v(TAG, "测试:搜索结束");
                spinKit.setVisibility(View.GONE);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                Log.d(TAG, "测试:配对状态改变:0 " + state);
                if (itemPosition != -1 && itemPosition < blueDeviceList.size()) {
                    Log.d(TAG, "测试:配对状态改变:1 ");
                    blueDeviceList.get(itemPosition).setConnectState(state);
                    Log.d(TAG, "测试:配对状态改变:2 " );
                    blueDeviceAdapter.notifyItemChanged(itemPosition);
                }

            } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                if (fragment != null) {
                    fragment.dismiss();
                }
            }
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }



    @Override
    public void onResume() {
        super.onResume();
        isSaveInstanceStateCalled = false;
        if(fragment!=null){
            fragment.dismiss();
        }
        initEvent();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth_connect, container, false);

        spinKit = view.findViewById(R.id.spin_kit);
        rvDeviceList = view.findViewById(R.id.rv_device_list);
        tvConnected = view.findViewById(R.id.tv_connected);
        tvName = view.findViewById(R.id.tv_name);
        tvAddress = view.findViewById(R.id.tv_address);
        tvStatus = view.findViewById(R.id.tv_status);
        tvWifiConfigure = view.findViewById(R.id.tv_wifi_configure);
        clConnected = view.findViewById(R.id.cl_connected);
        clSearch = view.findViewById(R.id.cl_search);
        etModel = view.findViewById(R.id.et_model);

        return view;
    }

    private void init() {
        context = MyApplication.getInstance();
        spinKit.setVisibility(View.GONE);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new HashSet<>();
        blueDeviceList = new ArrayList<>();
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
        requireActivity().registerReceiver(receiver, intentFilter);
        Log.d(TAG, "初始化: 注册完成");
        //注册蓝牙列表适配器
        blueDeviceAdapter = new BlueDeviceAdapter(blueDeviceList);
        rvDeviceList.setAdapter(blueDeviceAdapter);
        rvDeviceList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        //注册线程池
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("connect_activity_pool_%d");
            return thread;
        };

        executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());



    }

    @SuppressLint({"NotifyDataSetChanged", "MissingPermission"})
    private void initEvent() {
        final String[] connectedDeviceName = {""};

        executorService.submit(() -> {
            SharedPreferences preferences = context.getSharedPreferences("connectedPrinterInfo", Context.MODE_PRIVATE);
            String deviceName = preferences.getString("deviceName", "");
            String deviceHardwareAddress = preferences.getString("deviceHardwareAddress", "");
            int connectState = preferences.getInt("connectState", 12);

            Log.d(TAG, "测试:配对状态改变:判断连接状态1 " );
            if (PrintUtil.isConnection() == 0) {
                Log.d(TAG, "测试:配对状态改变:判断连接状态 2" );
                handler.post(() -> {
                    if (!deviceName.isEmpty()&&PrintUtil.getConnectedType()==0) {
                        Log.d(TAG, "测试:配对状态改变:判断连接状态3 " );
                        lastConnectedDevice = new BlueDeviceInfo(deviceName, deviceHardwareAddress, connectState);
                        tvConnected.setVisibility(View.VISIBLE);
                        connectedDeviceName[0] = lastConnectedDevice.getDeviceName();
                        tvName.setText(connectedDeviceName[0]);
                        tvAddress.setText(lastConnectedDevice.getDeviceHardwareAddress());
                        setWifiConfigureDisplayStatus(connectedDeviceName[0]);
                        tvStatus.setText("断开");
                        clConnected.setVisibility(View.VISIBLE);
                    }
                });

            }else {
                Log.d(TAG, "测试:配对状态改变:判断连接状态4 " );
                closeProcess();
            }
        });

        itemClickListener = position -> {
            //连接蓝牙设备
            itemPosition = position;

            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }

            int connectState = blueDeviceList.get(position).getConnectState();
            BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(blueDeviceList.get(position).getDeviceHardwareAddress());
            switch (connectState) {
                case Constant.NO_BOND -> executorService.submit(() -> {
                    requireActivity().runOnUiThread(() -> {
                        spinKit.setVisibility(View.GONE);
                        fragment = new MyDialogLoadingFragment("配对中");
                        fragment.show(requireActivity().getSupportFragmentManager(), "pairing");
                    });
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
                    requireActivity().runOnUiThread(() -> {
                        spinKit.setVisibility(View.GONE);
                        fragment = new MyDialogLoadingFragment("连接中");
                        fragment.show(requireActivity().getSupportFragmentManager(), "CONNECT");
                    });

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
                            }
                            case -1 -> hint = "连接失败";
                            case -2 -> hint = "不支持的机型";
                            default -> {
                            }
                        }

                        if (lastConnectedDevice != null) {
                            blueDeviceAdapter.notifyItemRemoved(position);
                            tvConnected.setVisibility(View.VISIBLE);
                            connectedDeviceName[0] = lastConnectedDevice.getDeviceName();
                            tvName.setText(connectedDeviceName[0]);
                            tvAddress.setText(lastConnectedDevice.getDeviceHardwareAddress());
                            setWifiConfigureDisplayStatus(connectedDeviceName[0]);
                            tvStatus.setText("断开");
                            clConnected.setVisibility(View.VISIBLE);
                        }

                        if (isSaveInstanceStateCalled) {
                            // 已经调用了，不执行 DialogFragment.dismiss() 方法
                            return;
                        }


                        fragment.dismiss();
                        Toast.makeText(getActivity(), hint, Toast.LENGTH_SHORT).show();
                    });
                });
                default -> {
                }
            }
        };

        tvStatus.setOnClickListener(v -> {
            PrintUtil.close();
            closeProcess();

        });

        blueDeviceAdapter.setOnClickListener(itemClickListener);

        clSearch.setOnClickListener(v -> {
            Log.d(TAG, "测试：初始化：搜索 ");
            spinKit.setVisibility(View.GONE);
            if (!mBluetoothAdapter.isEnabled()) {
                Toast.makeText(getActivity(), "蓝牙未开启", Toast.LENGTH_SHORT).show();
            } else {
                permissionBluetoothRequest();
            }


        });

        tvWifiConfigure.setOnClickListener(v -> {
            WifiManager wifiManager = (WifiManager) requireActivity().getSystemService(Context.WIFI_SERVICE);
            String connectedWifiName = "";
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                connectedWifiName = wifiInfo.getSSID().replace("\"", "");
            }


            dialogWifiSetFragment = new MyDialogWifiSetFragment(connectedWifiName, (wifiName, wifiPassword) -> {

                // 校验 wifi名称
                if (TextUtils.isEmpty(wifiName)) {
                    Toast.makeText(getActivity(), "请输入 WiFi 名称", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 校验 wifi密码
                if (TextUtils.isEmpty(wifiPassword)) {
                    Toast.makeText(getActivity(), "请输入 WiFi 密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                fragment = new MyDialogLoadingFragment("配置中");
                fragment.show(requireActivity().getSupportFragmentManager(), "configure");
                // 通过Handler将wifi账号和密码传递到主线程
                handler.post(() -> {
                    // 在主线程中执行UI相关的操作
                    // 模拟WiFi配网操作
                    simulateWiFiConfiguration(wifiName, wifiPassword);
                });


            });
            dialogWifiSetFragment.show(requireActivity().getSupportFragmentManager(), "configure");


        });


    }

    // 在该类中添加方法来模拟WiFi配网操作
    private void simulateWiFiConfiguration(String wifiName, String wifiPassword) {
        // 通过线程池在子线程中执行WiFi配网操作
        executorService.execute(() -> {
            int wifiConfigureResult = PrintUtil.getInstance().configurationWifi(wifiName, wifiPassword);
            // 在主线程中处理UI相关的操作
            handler.post(() -> {
                // 配网结果处理
                if (wifiConfigureResult == 0) {
                    // 配置成功
                    Toast.makeText(getActivity(), "配置成功", Toast.LENGTH_SHORT).show();
                } else if (wifiConfigureResult == -1) {
                    // 配置失败
                    Toast.makeText(getActivity(), "配置失败", Toast.LENGTH_SHORT).show();
                } else if (wifiConfigureResult == -3) {
                    // 设备不支持
                    Toast.makeText(getActivity(), "您的设备不支持", Toast.LENGTH_SHORT).show();
                }

                // 配网完成，关闭加载对话框
                fragment.dismiss();
            });
        });
    }

    private void  closeProcess() {
        SharedPreferences preferences = context.getSharedPreferences("printConfiguration", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("printMode", 1);
        editor.putInt("printDensity", 3);
        editor.putFloat("printMultiple", 8);
        editor.apply(); //提交修改

        SharedPreferences connectedPrinterInfo = context.getSharedPreferences("connectedPrinterInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor connectedPrinterInfoEditor = connectedPrinterInfo.edit();
        connectedPrinterInfoEditor.putString("deviceName", "");
        connectedPrinterInfoEditor.putString("deviceHardwareAddress", "");
        connectedPrinterInfoEditor.putInt("connectState", 11);
        connectedPrinterInfoEditor.apply();

        if (lastConnectedDevice != null) {
            lastConnectedDevice.setConnectState(12);
            blueDeviceList.add(lastConnectedDevice);
        }

        requireActivity().runOnUiThread(() -> {
            tvConnected.setVisibility(View.GONE);
            clConnected.setVisibility(View.GONE);
            blueDeviceAdapter.notifyItemChanged(blueDeviceList.size() - 1);
        });
        lastConnectedDevice = null;



    }



    /**
     * Gps开启状态判断
     *
     * @return Gps开启与否
     */
    public boolean isGpsOPen() {
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void permissionBluetoothRequest() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        }

        PermissionX.init(requireActivity())
                .permissions(permissions)
                .request(this::handlePermissionResult);
    }

    private void handlePermissionResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
        if (allGranted) {
            handleAllPermissionsGranted();
        } else {
            Toast.makeText(getActivity(), "权限打开失败" + deniedList, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleAllPermissionsGranted() {
        if (!isGpsOPen()) {
            //所有权限申请通过
            MyDialogFragment fragment = new MyDialogFragment("请开启GPS，未开启可能导致无法正常进行蓝牙搜索", 1);
            fragment.show(requireActivity().getSupportFragmentManager(), "GPS");
        } else {
            startBluetoothDiscovery();
        }
    }

    @SuppressLint({"MissingPermission", "NotifyDataSetChanged"})
    private void startBluetoothDiscovery() {
        Log.d(TAG, "测试:开始搜索1");
        itemPosition = -1;
        int itemCount = blueDeviceList.size();
        //清空列表数据
        deviceList.clear();
        blueDeviceList.clear();
        Log.d(TAG, "测试:开始搜索2");
        blueDeviceAdapter.notifyItemRangeRemoved(0, itemCount);
        spinKit.setVisibility(View.VISIBLE);
        Log.d(TAG, "测试:开始搜索4");
        if (mBluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "测试:开始搜索5");
            if (mBluetoothAdapter.cancelDiscovery()) {
                Log.d(TAG, "测试:开始搜索6");

                executorService.execute(() -> {
                    try {
                        //取消后等待1s后再次搜索
                        Thread.sleep(1000);
                        mBluetoothAdapter.startDiscovery();
                        Log.d(TAG, "测试:开始搜索7");

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.d(TAG, "测试:开始搜索8");
                    }
                });


            }
        } else {
            Log.d(TAG, "测试:开始搜索9");
            mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "测试:开始搜索10");
        }
    }

    private void setWifiConfigureDisplayStatus(String deviceName) {
        if (deviceName.startsWith("K3_W")) {
            tvWifiConfigure.setVisibility(View.VISIBLE);
        } else {
            tvWifiConfigure.setVisibility(View.GONE);
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "测试:onDestroy: ");
        // 注销广播接收器
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        requireActivity().unregisterReceiver(receiver);
    }
}