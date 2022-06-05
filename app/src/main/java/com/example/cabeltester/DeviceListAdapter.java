package com.example.cabeltester;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {


    private LayoutInflater mLayoutInflater;
    private int mResourceView;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();

    public DeviceListAdapter(Context context, int resource, ArrayList<BluetoothDevice> devices) {
        super(context, resource, devices);

        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResourceView = resource;
        mDevices = devices;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = mLayoutInflater.inflate(mResourceView, null);

        BluetoothDevice device = mDevices.get(position);
        TextView tvName = convertView.findViewById(R.id.tvNameDevice);
        TextView tvAddress = convertView.findViewById(R.id.tvAddress);
        tvName.setText(device.getName());
        tvAddress.setText(device.getAddress());


        return convertView;
    }
}
