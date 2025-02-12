package com.niimbot.printer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.niimbot.jcdemo.R;
import com.niimbot.printer.bean.WifiDeviceInfo;

import java.util.List;

public class WifiDeviceAdapter extends RecyclerView.Adapter<WifiDeviceAdapter.WifiDevicerViewHolder>{

    private final List<WifiDeviceInfo> wifiDeviceList;
    private OnItemClickListener listener;


    public interface OnItemClickListener {
        /**
         * 选项点击
         *
         * @param position 位置
         */
        void onItemClick(int position);
    }


    public WifiDeviceAdapter(List<WifiDeviceInfo> wifiDeviceList) {
        this.wifiDeviceList = wifiDeviceList;
    }

    public void setOnClickListener(OnItemClickListener onItemClickListener) {
        listener = onItemClickListener;
    }


    @NonNull
    @Override
    public WifiDevicerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wifi_item, parent, false);
        return new WifiDevicerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiDeviceAdapter.WifiDevicerViewHolder holder, int position) {
        holder.tvName.setText(wifiDeviceList.get(position).getDeviceName());
        holder.tvAddress.setText(wifiDeviceList.get(position).getIp());
        holder.tvStatus.setText("未连接");
        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return wifiDeviceList!=null?wifiDeviceList.size():0;
    }


    static class  WifiDevicerViewHolder extends RecyclerView.ViewHolder{
        public TextView tvName;
        public TextView tvAddress;

        public TextView tvStatus;
        public WifiDevicerViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}
