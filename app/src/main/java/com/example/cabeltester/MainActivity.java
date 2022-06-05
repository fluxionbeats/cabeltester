package com.example.cabeltester;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private String[] testerContent = {"Не подключено", "Кабель 1", "Кабель 2", "Кабель 3", "Кабель 4", "Кабель 5", "Кабель 6"};
    public final String TAG = getClass().getSimpleName();

    private View btBluetoothHint;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQ_ENABLE_BLUETOOTH = 1001;
    private ProgressDialog mProgressDialog;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();
    private ListView listDevices;
    private DeviceListAdapter mDeviceListAdapter;

    private BluetoothSocket mBluetoothSocket;
    private OutputStream mOutputStream;
    private View btStartScan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btBluetoothHint = findViewById(R.id.hint_bluetooth);
        btBluetoothHint.setOnClickListener(clickListener);
        btStartScan = findViewById(R.id.start_scan);
        btStartScan.setOnClickListener(clickListener);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "onCreate: Ваше устройство не поддерживает bluetooth.");
            finish();
        }

        mDeviceListAdapter = new DeviceListAdapter(this, R.layout.devices_item, mDevices);

        enableBluetooth();

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setIcon(R.drawable.ic_cabeltester);
        ArrayAdapter<String> testerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, testerContent);
        testerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spTesterContent = (Spinner) findViewById(R.id.spTesterContent);
        spTesterContent.setAdapter(testerAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_bluetooth:
                searchDevices();
                break;
            case R.id.menu_exit:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void searchDevices() {
        Log.d(TAG, "searchDevices()");
        enableBluetooth();
        if (!mBluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "searchDevices: начинаем поиск устройств");
            mBluetoothAdapter.startDiscovery();
        }
        if (mBluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "searchDevices: поиск уже был запущен. перезапускаем его ещё раз.");
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter.startDiscovery();
        }
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

    }


    private void showListDevices() {
        Log.d(TAG, "showListDevices()");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Найденные устройства");
        View view = getLayoutInflater().inflate(R.layout.list_devices, null);
        listDevices = view.findViewById(R.id.list_devices);
        listDevices.setAdapter(mDeviceListAdapter);
        listDevices.setOnItemClickListener(itemOnClickListener);

        builder.setView(view);
        builder.setNegativeButton("OK", null);
        builder.create();
        builder.show();
    }

    private void checkPermissionLocation() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int check = checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            check += checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

            if (check != 0) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1002);
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ENABLE_BLUETOOTH) {
            if (!mBluetoothAdapter.isEnabled()) {
                //пытаемся включить повторно
                enableBluetooth();
            }
        }
    }

    private void enableBluetooth() {
        Log.d(TAG, "enableBluetooth()");
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableBluetooth: Bluetooth выключен, пытаемся включить..");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE_BLUETOOTH);
        }
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.equals(btBluetoothHint)) {
                searchDevices();
                Log.d(TAG, "onClick: нажата подсказка о включении Bluetooth.");
            }
            else if (view.equals(btStartScan)){
                showToastMessage("Функция временно недоступна.");
            }
        }
    };


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                showToastMessage("Начат поиск устройств.");
                mProgressDialog = ProgressDialog.show(MainActivity.this, "Поиск устройств", "Пожалуйста, подождите...");
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                showToastMessage("Поиск устройств завершен.");
                mProgressDialog.dismiss();
                showListDevices();
            } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (!mDevices.contains(device)) {
                        mDeviceListAdapter.add(device);
                    }
                }
            }
        }
    };

    private AdapterView.OnItemClickListener itemOnClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            BluetoothDevice device = mDevices.get(position);
            startConnection(device);
        }
    };

    private void setMessage(String command) {
        byte[] buffer = command.getBytes();
        if (mOutputStream != null) {
            try {
                mOutputStream.write(buffer);
                mOutputStream.flush();
                showToastMessage("Данные успешно отправлены!");
            } catch (IOException e) {
                showToastMessage("Ошибка отправки данных!");
                e.printStackTrace();

            }

        }
    }

    private void changeMenuForConnectedDevice(BluetoothDevice device) {
        if (device != null) {
            TextView title = findViewById(R.id.page_title);
            title.setText("Вы подключены к " + device.getName());
        }
    }

    private void startConnection(BluetoothDevice device) {
        if (device != null) {
            try {
                Method method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                mBluetoothSocket = (BluetoothSocket) method.invoke(device, 1);
                mBluetoothSocket.connect();
                mOutputStream = mBluetoothSocket.getOutputStream();
                showToastMessage("Устройство подключено!");
                // изменение данных на странице
                changeMenuForConnectedDevice(device);
            } catch (Exception e) {
                e.printStackTrace();
                showToastMessage("Ошибка подключения!");
            }

        }
    }


}

