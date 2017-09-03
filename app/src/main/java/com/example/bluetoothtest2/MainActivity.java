package com.example.bluetoothtest2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String TAG = "MainActivity";
    private BroadcastReceiver mRceiver;

    private Button buttonSerch;
    private Button buttonSend;
    private EditText editTextData;
    private ListView listViewDevices;
    private ArrayList<String> arrayList;
    private ArrayAdapter<String> arrayAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获得本地蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initView();

        checkBluetoothSupport();
        Toast.makeText(MainActivity.this, "开始搜索蓝牙设备,首次搜索时间较长,请耐心等待...", Toast.LENGTH_SHORT).show();

        serchBluetoothDevice();

        //设置广播接收器
        mRceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    arrayAdapter.add(device.getName()+ ":" + device.getAddress());
                    String dev = device.getName()+":"+device.getAddress();
                    if (!arrayList.contains(dev)){
                        arrayList.add(dev);
                    }
                    arrayAdapter.notifyDataSetChanged(); //通知ListView适配器 数据已经发生改变
                }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    Log.d(TAG,"搜索结束!");

                    //在主线程中显示搜索结束信息
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "搜索结束!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        };
        //注册广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mRceiver, intentFilter);
    }

    /**
     * 搜索蓝牙设备
     * startDiscovery方法是异步的
     */
    private void serchBluetoothDevice() {
        //搜索设备
        while (!bluetoothAdapter.startDiscovery()){
            Log.d(TAG, "尝试失败");
            try {
                Thread.sleep(100);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 检测是否支持蓝牙
     */
    private void checkBluetoothSupport() {
        //检测是否支持蓝牙
        if (bluetoothAdapter!=null) {
            //打开蓝牙
            if (!bluetoothAdapter.isEnabled()) {
                //对话框提示
//                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                //静默打开
                bluetoothAdapter.enable();
                bluetoothAdapter.cancelDiscovery();
            }
        }else{
            finish();
        }
    }

    /**
     * 初始化布局控件功能
     */
    private void initView() {
        buttonSerch = (Button)findViewById(R.id.button_serch);
        buttonSerch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                arrayList.clear();
                if (bluetoothSocket != null)
                    try {
                        if (bluetoothSocket.isConnected())
                            bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }

                Toast.makeText(MainActivity.this, "开始搜索蓝牙设备...", Toast.LENGTH_LONG).show();
                serchBluetoothDevice();
            }
        });

        listViewDevices = (ListView)findViewById(R.id.listView_devices);
        arrayList = new ArrayList<>();
//        getPairingBluetoothDevice();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList);
        listViewDevices.setAdapter(arrayAdapter);
        listViewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String devices = arrayAdapter.getItem(i);
                String address = devices.substring(devices.indexOf(":") + 1).trim();

                Toast.makeText(MainActivity.this, "正在连接,请稍等...", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "address -->  " + address);

                /**
                 * 连接与断开开启新线程原因:  如果不开新线程,程序在close的时候会闪退
                 */
                //连接新的设备时,断开当前连接
                if (bluetoothSocket != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "in  disconnect Thread ");
                            if (bluetoothSocket.isConnected()) {
                                try {
                                    bluetoothSocket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }// end of if
                        }
                    }).start();
                }


                //连接新设备
                    bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //获得设备
                                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                                bluetoothSocket.connect();
                                outputStream = bluetoothSocket.getOutputStream();

                                //在主线程中显示连接成功提示信息
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (bluetoothSocket.isConnected())
                                        Toast.makeText(MainActivity.this, "已连接!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
            }
        });

        editTextData = (EditText)findViewById(R.id.editText_sendData);
        buttonSend = (Button)findViewById(R.id.button_send);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = editTextData.getText().toString();
                if (outputStream != null) {
                    try {
                        outputStream.write(data.getBytes("ASCII"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else
                    Toast.makeText(MainActivity.this, "当前未连接任何设备!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 获得已经配对的蓝牙设备
     */
    private void getPairingBluetoothDevice() {
        //获得已经配对的设备
        Set<BluetoothDevice> deviceSet = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : deviceSet){
            arrayList.add(device.getName()+":"+device.getAddress());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (bluetoothSocket != null)
                        bluetoothSocket.close();
                    if (outputStream != null)
                        outputStream.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();

        //注销广播
        unregisterReceiver(mRceiver);
        //关闭蓝牙
        bluetoothAdapter.disable();
    }
}
