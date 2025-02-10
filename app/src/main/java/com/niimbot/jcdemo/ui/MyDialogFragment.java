package com.niimbot.jcdemo.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;


public class MyDialogFragment extends DialogFragment {
    private static final String TAG = "MyDialogFragment";

    private final String message;
    private final int eventType;

    public MyDialogFragment(String message, int eventType) {
        this.message = message;
        this.eventType = eventType;
    }




    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("提示").setMessage(message).setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        if (eventType == 1) {
            builder.setPositiveButton("确定", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            });
        }



        return builder.create();
    }
}
