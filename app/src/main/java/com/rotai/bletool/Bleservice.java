package com.rotai.bletool;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import java.util.Set;

import static android.support.v4.app.ActivityCompat.startActivityForResult;


//建立一个蓝牙服务类

public class Bleservice extends Service {
    private BleBinder mBleBinder;
    private Handler mBleHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBleBinder;
    }

    public class BleBinder extends Binder {
        //在此提供对外部的调用方法，当某活动绑定此服务后，获得返回mBleBinder对象，外部活动通过操作mBleBinder的方法来控制蓝牙设备。
        public void startScan(){
            //开始扫描......
        }

        public void stopScan(){
            //停止扫描........
        }
        //更多方法........需要注意的是，某些方法为耗时操作，有必要时应该开启子线程去执行。
        //而且蓝牙很多时候都是异步操作，需要使用许多回调方法。
        //如果此服务为独立进程服务，并为其他app提供数据，需要注意方法同步。
    }
}


    //适配器与蓝牙管理器的成员变量。
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;

    //检查设备是否支持BLE功能。
    private boolean checkIfSupportBle(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    //如果设备支持BLE，那么就可以获取蓝牙适配器。
    private BluetoothAdapter getAdapter(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        } else {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return mBluetoothAdapter;
    }

    //获取完适配器后，需要检测是否已经打开蓝牙功能，如果没有，就需要开启。
//开启蓝牙功能需要一小段时间，具体涉及的线程操作或同步对象不在此讨论，视实际情况按需编写。
    private void enableBluetooth(){
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    //此方法用于获取在手机中已经获取并绑定了的设备
    private void getBoundDevices(){
        Set<BluetoothDevice> boundDevices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice device : boundDevices){
            //对device进行其他操作，比如连接等。
        }
    }
