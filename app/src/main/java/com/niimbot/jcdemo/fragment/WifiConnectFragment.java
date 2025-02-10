package com.niimbot.jcdemo.fragment;

import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.gengcon.www.jcprintersdk.bean.PrinterDevice;
import com.gengcon.www.jcprintersdk.callback.ScanCallback;
import com.niimbot.jcdemo.R;
import com.niimbot.jcdemo.adapter.BlueDeviceAdapter;
import com.niimbot.jcdemo.adapter.WifiDeviceAdapter;
import com.niimbot.jcdemo.app.MyApplication;
import com.niimbot.jcdemo.bean.BlueDeviceInfo;
import com.niimbot.jcdemo.bean.WifiDeviceInfo;
import com.niimbot.jcdemo.databinding.FragmentWifiConnectBinding;
import com.niimbot.jcdemo.ui.MyDialogLoadingFragment;
import com.niimbot.jcdemo.utils.PrintUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class WifiConnectFragment extends Fragment {
    private static final String TAG = "WifiConnectFragment";
    private MyDialogLoadingFragment fragment;
    private ExecutorService executorService;
    private Context context;
    private static final String USER_DEFINED = "自定义";
    private FragmentWifiConnectBinding bind;
    private List<WifiDeviceInfo> wifiDeviceList;
    private Set<String> deviceList;

    private WifiDeviceAdapter wifiDeviceAdapter;

    private WifiDeviceAdapter.OnItemClickListener itemClickListener;
    private int itemPosition;
    private WifiDeviceInfo lastConnectedDevice;

    /**
     * 打印机过滤
     */
    private String printNameStart = "";

    Handler handler = new Handler(Looper.getMainLooper());
    private boolean isSaveInstanceStateCalled = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        isSaveInstanceStateCalled = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        isSaveInstanceStateCalled = false;
        if (fragment != null) {
            fragment.dismiss();
        }
        init();
    }


    private void init() {
        context = MyApplication.getInstance();
        bind.spinKit.setVisibility(View.GONE);
        deviceList = new HashSet<>();
        wifiDeviceList = new ArrayList<>();


        //注册蓝牙列表适配器
        wifiDeviceAdapter = new WifiDeviceAdapter(wifiDeviceList);

        bind.rvDeviceList.setAdapter(wifiDeviceAdapter);
        bind.rvDeviceList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        //注册线程池
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("wifi_connect_pool_%d");
            return thread;
        };

        executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());
        initEvent();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        bind = FragmentWifiConnectBinding.inflate(inflater, container, false);
        return bind.getRoot();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }


    private void initEvent() {
        final String[] connectedDeviceName = {""};
        bind.rgPrintModel.setOnCheckedChangeListener((group, checkedId) -> {
            bind.spinKit.setVisibility(View.GONE);
            // 先设置为隐藏，后面根据情况调整可见性
            bind.etModel.setVisibility(View.GONE);

            if (R.id.rb_k3w == checkedId) {
                printNameStart = "K3_W";
            } else if (R.id.rb_all == checkedId) {
                printNameStart = "";
            } else if (R.id.rb_input == checkedId) {
                bind.etModel.setVisibility(View.VISIBLE);
                printNameStart = USER_DEFINED;
            }

        });
        bind.clSearch.setOnClickListener(view -> {
            Handler handler = new Handler(Looper.getMainLooper());
            bind.spinKit.setVisibility(View.VISIBLE);
            int itemCount = wifiDeviceList.size();
            wifiDeviceList.clear();
            deviceList.clear();
            wifiDeviceAdapter.notifyItemRangeRemoved(0, itemCount);
            requireActivity();
            WifiManager wifiManager = (WifiManager) requireActivity().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null || !wifiManager.isWifiEnabled()) {
                bind.spinKit.setVisibility(View.GONE);
                return;
            }


            PrintUtil.getInstance().scanWifiPrinter(new ScanCallback() {
                @Override
                public void onScan(PrinterDevice printerDevice) {
                    Log.d(TAG, "测试：onScan: " + printerDevice.getDeviceName());
                    if (TextUtils.isEmpty(printNameStart) || isDeviceNameValid(printerDevice, printNameStart)) {
                        if (deviceList.add(printerDevice.getDeviceName())) {
                            int position = wifiDeviceList.size();
                            wifiDeviceList.add(new WifiDeviceInfo(printerDevice.getDeviceName(), printerDevice.getDeviceIp(), printerDevice.port, 11));
                            handler.post(() -> wifiDeviceAdapter.notifyItemInserted(position));

                        }
                    }


                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "onFinish: " + "搜索完成");
                    handler.post(() -> {
                        bind.spinKit.setVisibility(View.GONE);
                        Toast.makeText(context, "搜索完成", Toast.LENGTH_SHORT).show();
                    });

                }
            });
        });


        executorService.submit(() -> {
            Log.d(TAG, "打印机连接状态: " + PrintUtil.isConnection());
            SharedPreferences preferences = context.getSharedPreferences("wifiDeviceConnectedPrinterInfo", Context.MODE_PRIVATE);
            String deviceName = preferences.getString("deviceName", "");
            String ip = preferences.getString("ip", "");
            int port = preferences.getInt("port", 9000);
            int connectState = preferences.getInt("connectState", 12);
            if (PrintUtil.isConnection() == 0 ) {
                handler.post(() -> {
                    if (!deviceName.isEmpty()&&PrintUtil.getConnectedType()==1) {
                        lastConnectedDevice = new WifiDeviceInfo(deviceName, ip, port, connectState);
                        bind.tvConnected.setVisibility(View.VISIBLE);
                        connectedDeviceName[0] = lastConnectedDevice.getDeviceName();
                        bind.tvName.setText(connectedDeviceName[0]);
                        bind.tvAddress.setText(lastConnectedDevice.getIp());
                        bind.tvStatus.setText("断开");
                        bind.clConnected.setVisibility(View.VISIBLE);
                    }
                });

            }else {
                closeProcess();
            }
        });

        itemClickListener = position -> {
            Handler handler = new Handler(Looper.getMainLooper());
            itemPosition = position;
            WifiDeviceInfo wifiDeviceInfo = wifiDeviceList.get(itemPosition);
            handler.post(() -> bind.spinKit.setVisibility(View.GONE));
            executorService.submit(() -> {
                fragment = new MyDialogLoadingFragment("连接中");
                fragment.show(requireActivity().getSupportFragmentManager(), "CONNECT");
                Log.d(TAG, "测试:开始连接: ");
                PrintUtil.setConnectedType(-1);
                int connectResult = PrintUtil.getInstance().connectWifiPrinter(wifiDeviceInfo.getIp(), wifiDeviceInfo.getPort());
                String hint = "";
                Log.d(TAG, "测试:网络连接: " + connectResult);
                switch (connectResult) {
                    case 0 -> {
                        Log.d(TAG, "测试:网络连接：流程1");
                        lastConnectedDevice = new WifiDeviceInfo(wifiDeviceInfo.getDeviceName(), wifiDeviceInfo.getIp(), wifiDeviceInfo.getPort(), 12);
                        lastConnectedDevice.setConnectState(13);
                        wifiDeviceList.remove(position);
                        hint = "连接成功";
                        Log.d(TAG, "测试:网络连接：流程2");
                        SharedPreferences preferences = context.getSharedPreferences("printConfiguration", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        SharedPreferences connectedPrinterInfo = context.getSharedPreferences("wifiDeviceConnectedPrinterInfo", Context.MODE_PRIVATE);
                        SharedPreferences.Editor connectedPrinterInfoEditor = connectedPrinterInfo.edit();
                        Log.d(TAG, "测试:网络连接：流程3");
                        String printerName = wifiDeviceInfo.getDeviceName();
                        Log.d(TAG, "测试:网络连接：流程4");
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
                        connectedPrinterInfoEditor.putString("ip", lastConnectedDevice.getIp());
                        connectedPrinterInfoEditor.putInt("port", lastConnectedDevice.getPort());
                        connectedPrinterInfoEditor.putInt("connectState", lastConnectedDevice.getConnectState());
                        connectedPrinterInfoEditor.apply();
                    }
                    case -1 -> hint = "连接失败";
                    case -2 -> hint = "不支持的机型";
                    default -> {
                    }
                }

                Log.d(TAG, "测试:网络连接：流程7结束");

                String finalHint = hint;
                handler.post(() -> {

                    if (lastConnectedDevice != null) {
                        wifiDeviceAdapter.notifyItemRemoved(position);
                        bind.tvConnected.setVisibility(View.VISIBLE);
                        connectedDeviceName[0] = lastConnectedDevice.getDeviceName();
                        bind.tvName.setText(connectedDeviceName[0]);
                        bind.tvAddress.setText(lastConnectedDevice.getIp());
                        bind.tvStatus.setText("断开");
                        bind.clConnected.setVisibility(View.VISIBLE);
                    }


                    if (isSaveInstanceStateCalled) {
                        // 已经调用了，不执行 DialogFragment.dismiss() 方法
                        return;
                    }

                    fragment.dismiss();
                    Toast.makeText(context, finalHint, Toast.LENGTH_SHORT).show();
                });

            });
        };

        wifiDeviceAdapter.setOnClickListener(itemClickListener);

        bind.tvStatus.setOnClickListener(v -> {
            PrintUtil.close();
            closeProcess();

        });
    }

    private void closeProcess(){
        SharedPreferences preferences = context.getSharedPreferences("printConfiguration", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("printMode", 1);
        editor.putInt("printDensity", 3);
        editor.putFloat("printMultiple", 8);
        editor.apply(); //提交修改

        SharedPreferences connectedPrinterInfo = context.getSharedPreferences("wifiConnectedPrinterInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor connectedPrinterInfoEditor = connectedPrinterInfo.edit();
        connectedPrinterInfoEditor.putString("deviceName", "");
        connectedPrinterInfoEditor.putString("deviceHardwareAddress", "");
        connectedPrinterInfoEditor.putInt("connectState", 12);
        connectedPrinterInfoEditor.apply();
        if (lastConnectedDevice != null) {
            lastConnectedDevice.setConnectState(11);
            wifiDeviceList.add(lastConnectedDevice);
        }

        handler.post(() -> {
            bind.tvConnected.setVisibility(View.GONE);
            bind.clConnected.setVisibility(View.GONE);
            wifiDeviceAdapter.notifyItemChanged(wifiDeviceList.size() - 1);
        });

        lastConnectedDevice = null;
    }

    private boolean isDeviceNameValid(PrinterDevice printerDevice, String printNameStart) {
        return printerDevice.getDeviceName().startsWith(printNameStart);
    }
}