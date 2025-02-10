package com.niimbot.jcdemo.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.niimbot.jcdemo.bean.BlueDeviceInfo;
import com.niimbot.jcdemo.databinding.BluetoothItemBinding;

import java.util.List;

/**
 * 蓝牙适配器
 *
 * @author zhangbin
 * @date 2021/03/25
 */
public class BlueDeviceAdapter extends RecyclerView.Adapter<BlueDeviceAdapter.BlueDeviceViewHolder> {
    private final List<BlueDeviceInfo> blueDeviceInfoList;
    private OnItemClickListener listener;

    public BlueDeviceAdapter(List<BlueDeviceInfo> blueDeviceInfoList) {
        this.blueDeviceInfoList = blueDeviceInfoList;
    }

    public interface OnItemClickListener {
        /**
         * 选项点击
         *
         * @param position 位置
         */
        void onItemClick(int position);
    }

    public void setOnClickListener(OnItemClickListener onItemClickListener) {
        listener = onItemClickListener;
    }

    @NonNull
    @Override
    public BlueDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BluetoothItemBinding bind = BluetoothItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new BlueDeviceViewHolder(bind);
    }

    @Override
    public void onBindViewHolder(@NonNull BlueDeviceViewHolder holder, int position) {
        holder.bind.tvName.setText(blueDeviceInfoList.get(position).getDeviceName());
        holder.bind.tvAddress.setText(blueDeviceInfoList.get(position).getDeviceHardwareAddress());
        String status = "";
        int color  ;
        switch (blueDeviceInfoList.get(position).getConnectState()) {
            case 10:
                status = "未配对";
              color  = Color.BLACK;
                break;
            case 11:
                status = "配对中";
                color  = Color.BLACK;
                break;
            case 12:
                status = "已配对";
                color  = Color.BLACK;
                break;
            default:
                color  = Color.BLACK;
                break;
        }

        holder.bind.tvStatus.setText(status);
        holder.bind.tvStatus.setTextColor(color);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));

    }


    @Override
    public int getItemCount() {
        return blueDeviceInfoList != null ? blueDeviceInfoList.size() : 0;
    }

    public static class BlueDeviceViewHolder extends RecyclerView.ViewHolder {
        BluetoothItemBinding bind;

        public BlueDeviceViewHolder(BluetoothItemBinding bind) {
            super(bind.getRoot());
            this.bind = bind;

        }
    }
}
