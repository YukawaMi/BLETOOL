package com.rotai.bletool;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.BluetoothService;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // 常量,显示当前的连接状态
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    //DEBUG
    private static final String TAG = "BleTool";
    private static final boolean T = true;

    //从BluetoothChatService发送处理程序的消息类型
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int RETURN_FILRPATH = 3;
    private static final int CONNECTED=1;
    private static final int DISCONNECTED=0;


    private ListView mConversationView;
    Button bSelect,bUpdate;

    EditText count,settime;

    Toolbar mToolbar;
    TextView tv;

    int state=DISCONNECTED;


    // 连接设备的名称
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    // 本地蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter = null;

    BluetoothClient mClient = new BluetoothClient(this);

    private BluetoothService mBleService = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT>=23)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 10);   }
        }

        setContentView(R.layout.activity_main);
        //设置Toolbar
        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle("BLE调试工具");
        setSupportActionBar(mToolbar);
        tv = findViewById(R.id.tv);

        bSelect =  findViewById(R.id.buttonSelect);
        bUpdate = findViewById(R.id.buttonUpdate);
        bSelect.setOnClickListener(this);
        bUpdate.setOnClickListener(this);




    }
     private void setupBle(){

         Log.d(TAG, "setupChat()");

         mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
         mConversationView =  findViewById(R.id.in);
         mConversationView.setAdapter(mConversationArrayAdapter);
         // 初始化BluetoothService进行蓝牙连接


         mOutStringBuffer = new StringBuffer("");
     }




    private void ensureDiscoverable() {
        Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.discoverable:
                // 允许被发现设备
                ensureDiscoverable();
                return true;
            case R.id.scan:
                Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                break;
        }
        return false;
    }

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
        if (state==DISCONNECTED){
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

                default:
                    break;
            }
        }
        else if(state==DISCONNECTED){
            Toast.makeText(MainActivity.this,"请先连接蓝牙设备",Toast.LENGTH_LONG).show();
        }

    }



    private  String filepath;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (resultCode != Activity.RESULT_OK) {
//            Log.e(TAG, "onActivityResult() error, resultCode: " + resultCode);
//            super.onActivityResult(requestCode, resultCode, data);
//            return;
//        }
//
//        if (requestCode == FILE_SELECT_CODE) {
//            Uri uri = data.getData();
//            Log.i(TAG, "------->" + uri.getPath());
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT){
//                filepath = getPath(this,uri);
//            }else{
//                filepath = getRealPathFromURI(uri);
//
//            }
//            tv.setText(filepath);
//        }
//        super.onActivityResult(requestCode, resultCode, data);

        if(T) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case RETURN_FILRPATH:
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


            case REQUEST_CONNECT_DEVICE:
                // 当DeviceListActivity返回与设备连接的消息
                if (resultCode == Activity.RESULT_OK) {
                    // 得到链接设备的MAC
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // 得到BLuetoothDevice对象
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // 试图连接到设备
                    //mChatService.connect(device);
                    state =CONNECTED;
                }else{
                    state=DISCONNECTED;
                }
                break;
            case REQUEST_ENABLE_BT:
                // 判断蓝牙是否启用
                if (resultCode == Activity.RESULT_OK) {
                    // 建立连接

                } else {
                    Log.d(TAG, "蓝牙未启用");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }

    }

    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(null!=cursor&&cursor.moveToFirst()){;
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



