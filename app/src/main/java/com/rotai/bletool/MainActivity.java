package com.rotai.bletool;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;

import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rotai.bletool.adapter.LeDeviceListAdapter;
import com.rotai.bletool.utils.ByteUtils;


import java.io.File;
import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.callback.BleConnCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    //DEBUG
    private static final String TAG = "BleTool";
    private static final boolean T = true;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Ble<BleDevice> mBle;




    Button bSelect,bUpdate;

    ProgressBar mProgressBar;
    Toolbar mToolbar;
    TextView tv;

    int index = 0;




    // 本地蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter = null;
    //已连接的设备
    //BleDevice connectedDevice = null;



    ListView mListView ;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT>=23)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 10);   }
        }



        setContentView(R.layout.activity_main);
        mProgressBar = findViewById(R.id.progressBar);

        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView = findViewById(R.id.listView);
        mListView.setAdapter(mLeDeviceListAdapter);
        mListView.setOnItemClickListener(this);

        //设置Toolbar
        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle("BLE调试工具");
        setSupportActionBar(mToolbar);
        tv = findViewById(R.id.tv);

        bSelect =  findViewById(R.id.buttonSelect);
        bUpdate = findViewById(R.id.buttonUpdate);
        bSelect.setOnClickListener(this);
        bUpdate.setOnClickListener(this);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 判断蓝牙是否可用
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙是不可用的", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initBle();

    }

    private void initBle() {
         mBle = Ble.options()
                 .setLogBleExceptions(true)//设置是否输出打印蓝牙日志（非正式打包请设置为true，以便于调试）
                 .setThrowBleException(true)//设置是否抛出蓝牙异常
                 .setAutoConnect(false)//设置是否自动连接
                 .setConnectFailedRetryCount(3)
                 .setConnectTimeout(10 * 1000)//设置连接超时时长（默认10*1000 ms）
                 .setScanPeriod(6 * 1000)//设置扫描时长（默认10*1000 ms）
                 .setUuid_service(UUID.fromString("0000abf0-0000-1000-8000-00805f9b34fb"))//主服务的uuid
                 .setUuid_write_cha(UUID.fromString("0000abf1-0000-1000-8000-00805f9b34fb"))//可写特征的uuid
                 .setUuid_notify(UUID.fromString("0000abf2-0000-1000-8000-00805f9b34fb"))//消息通知的uuid

                 .create(getApplicationContext());

     }








    /**
     * 重新扫描
     */
    private void bleScan() {
        if (mBle != null && !mBle.isScanning()) {
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.addDevices(mBle.getConnetedDevices());
            mBle.startScan(scanCallback);
        }
    }

    BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {

            synchronized (mBle.getLocker()) {
                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                bleScan();
                break;
            case R.id.menu_introduced:
                Toast.makeText(MainActivity.this,"233333",Toast.LENGTH_LONG).show();
                break;

        }
        return false;
    }

    @Override//Item点击事件
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BleDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null ) return;
        if (mBle.isScanning()){
            mBle.stopScan();
        }
        if (device.isConnected()){//设备已连接就断开连接
            mBle.disconnect(device, new BleConnCallback<BleDevice>() {
                @Override
                public void onConnectionChanged(BleDevice device) {
                    Toast.makeText(MainActivity.this,"设备已断开",Toast.LENGTH_LONG).show();
                }
            });

        }else if (!device.isConnectting()){//设备未连接且不处于正在连接的状态，就建立连接
            mBle.connect(device, connectCallback);

        }
    }

    byte[] data;

    int total = 0;


    private  void initalOta(){
        Log.w(TAG,"初始化OTA");
        mBle.write(mBle.getConnetedDevices().get(0), ("OTA").getBytes(), new BleWriteCallback<BleDevice>() {
            @Override
            public void onWriteSuccess(BluetoothGattCharacteristic characteristic) {
            }
        });
    }

    /**
     * OTA升级，每次分别写入1000字节的数据
     * 参数：
     * data：传输的数据
     * index：当前包的索引
     */

    private void otaProcess(){


        Log.w(TAG,"正在发送第"+index+"/"+total+"个");
        if (index< total){
            byte[] otaData = Arrays.copyOfRange(data, (index-1)*20, (index)*20);
            mBle.write(mBle.getConnetedDevices().get(0), otaData, new BleWriteCallback<BleDevice>() {
                @Override
                public void onWriteSuccess(BluetoothGattCharacteristic characteristic) {

                }
            });
//
        }else if(index==total){
            byte[] otaData = Arrays.copyOfRange(data, (index-1)*20, data.length);
            mBle.write(mBle.getConnetedDevices().get(0), otaData, new BleWriteCallback<BleDevice>() {
                @Override
                public void onWriteSuccess(BluetoothGattCharacteristic characteristic) {

                }
            });
        }
        else{
            //发送完毕

            mBle.write(mBle.getConnetedDevices().get(0), ("OTAOVER").getBytes(), new BleWriteCallback<BleDevice>() {
                @Override
                public void onWriteSuccess(BluetoothGattCharacteristic characteristic) {
                }
            });
        }


    }

    int count = 0;
    /**
     * 设置通知的回调
     */
    private BleNotiftCallback<BleDevice> bleNotiftCallback = new BleNotiftCallback<BleDevice>() {
        @Override
        public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            L.e(TAG, "onChanged==uuid:" + uuid.toString());
            L.e(TAG, "onChanged==address:" + device.getBleAddress());
            L.e(TAG, "onChanged==data:" + Arrays.toString(characteristic.getValue()));
            String tmp = null;
            try {
                tmp = new String(characteristic.getValue(),"ascii");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            switch (tmp){
                case "REQUEST":
                    index += 1;
                    otaProcess();
                    count += 1;

                    Log.w(TAG,"第"+count+"次请求");
                    break;
                case "OTA":
                    Log.w(TAG,"当前包接收成功  "+index);

                    break;

                case "OTAOK":

                    break;
                default:
                    Log.w(TAG,"收到数据："+tmp);

                    break;


            }


        }
    };

    /**
     * 连接的回调
     */
    private BleConnCallback<BleDevice> connectCallback = new BleConnCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(final BleDevice device) {
            if (device.isConnected()) {
                /*连接成功后，设置通知*/
                mBle.startNotify(device, bleNotiftCallback);
                Toast.makeText(MainActivity.this,"已连接设备",Toast.LENGTH_LONG).show();

            }
            L.e(TAG, "onConnectionChanged: " + device.isConnected());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onConnectException(BleDevice device, int errorCode) {
            super.onConnectException(device, errorCode);
            Toast.makeText(MainActivity.this,"连接异常，异常状态码:" + errorCode,Toast.LENGTH_LONG).show();

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if(T) Log.e(TAG, "- ON STOP -");

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private static final int FILE_SELECT_CODE = 0;

    @Override
    public void onClick(View v) {  //蓝牙点击事件
        if (!mBle.getConnetedDevices().isEmpty()){
            switch (v.getId()){
                case R.id.buttonSelect://选择文件按钮
                    Toast.makeText(MainActivity.this,"选择文件",Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    try {
                        startActivityForResult(Intent.createChooser(intent, "选择文件"), FILE_SELECT_CODE);
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(this, "亲，木有文件管理器啊-_-!!", Toast.LENGTH_SHORT).show();
                    }
                case R.id.buttonUpdate:
                    if (filepath == null){
                        Toast.makeText(this, "请先选择升级所需固件", Toast.LENGTH_SHORT).show();
                    }else{
                        index = 0;
                        count = 0;
                        sendEntityData(filepath);
                        Toast.makeText(this, "正在升级", Toast.LENGTH_SHORT).show();
                    }
                default:
                    break;
            }
        }
        else {
            Toast.makeText(MainActivity.this,"请先连接蓝牙设备",Toast.LENGTH_LONG).show();
        }

    }



    /**
     * 发送大数据量的包
     */
    private void sendEntityData(String path) {


        File file = new File(path);
        Log.w(TAG,"文件路径为："+path);
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
             data = ByteUtils.toByteArray(inStream);

             if (data.length%20 != 0){
                 total = data.length/20+1;
             }else{
                 total = data.length/20;
             }
            Log.w(TAG,"文件总大小为:"+(data.length/1000)+"K字节，本次升级需进行"+total+"次数据写入");
            initalOta();//告诉设备进入OTA状态
        } catch (IOException e) {
            e.printStackTrace();
        }



    }



    private  String filepath;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            Log.e(TAG, "onActivityResult() error, resultCode: " + resultCode);
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if (requestCode == FILE_SELECT_CODE) {
            Uri uri = data.getData();
            Log.i(TAG, "------->" + uri.getPath());
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT){

                filepath = getPath(this,uri);
            }else{
                filepath = getRealPathFromURI(uri);

            }
            tv.setText(filepath);
        }
        super.onActivityResult(requestCode, resultCode, data);

        if(T) Log.d(TAG, "onActivityResult " + resultCode);


    }

    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(null!=cursor&&cursor.moveToFirst()){
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
            cursor.close();
        }
        return res;
    }


    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    @SuppressLint("NewApi")
    public String getPath(final Context context, final Uri uri) {


        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;


        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];


                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {


                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));


                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];


                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }


                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};


                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {


        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};


        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }



}



