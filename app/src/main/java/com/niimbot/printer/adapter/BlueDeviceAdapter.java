package com.niimbot.printer.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.niimbot.jcdemo.R;
import com.niimbot.printer.bean.BlueDeviceInfo;

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_item, parent, false);
        return new BlueDeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlueDeviceViewHolder holder, int position) {
        holder.tvName.setText(blueDeviceInfoList.get(position).getDeviceName());
        holder.tvAddress.setText(blueDeviceInfoList.get(position).getDeviceHardwareAddress());
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

        holder.tvStatus.setText(status);
        holder.tvStatus.setTextColor(color);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));

    }


    @Override
    public int getItemCount() {
        return blueDeviceInfoList != null ? blueDeviceInfoList.size() : 0;
    }

    public static class BlueDeviceViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName;
        public TextView tvAddress;

        public TextView tvStatus;

        public BlueDeviceViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}
