package com.example.bluetoothclient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class MainActivity extends Activity {

    private boolean CONTINUE_READ_WRITE = true;
    public TextView mValueView;

    public String fileName = "AccelData2.csv";
    public File file;
    public FileWriter writer;

    public File sdCard;

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public void createFile() {
        sdCard = new File(Environment.getExternalStorageDirectory() + "/Folder"); //storage/emulated/0/Folder

        if (!sdCard.exists()) { //
            sdCard.mkdirs();
        }

        final String fullFileName = sdCard.toString() + "/" + fileName;

        try {
            writer = new FileWriter(fullFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    /* "If you save the state of the application in a bundle (typically non-persistent,
    dynamic data in onSaveInstanceState), it can be passed back to onCreate if the activity
    needs to be recreated (e.g., orientation change) so that you don't lose this prior information.
     If no data was supplied, savedInstanceState is null."
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter != null && adapter.isDiscovering()){
            adapter.cancelDiscovery();
        }
        adapter.startDiscovery();

        mValueView = (TextView) findViewById(R.id.value_view);
        mValueView.setMovementMethod(ScrollingMovementMethod.getInstance());

        if (isExternalStorageWritable()) { // returns true
            createFile();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{unregisterReceiver(discoveryResult);}catch(Exception e){e.printStackTrace();}
        if(socket != null){
            try{
                is.close();
                os.close();
                socket.close();
                CONTINUE_READ_WRITE = false;
                writer.close();
            }catch(Exception e){}
        }
    }


    private BluetoothSocket socket;
    private OutputStreamWriter os;
    private InputStream is;
    private BluetoothDevice remoteDevice;
    private BroadcastReceiver discoveryResult = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            android.util.Log.e("TrackingFlow", "WWWTTTFFF");
            unregisterReceiver(this);
            remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            new Thread(reader).start();
        }
    };

    private Runnable reader = new Runnable() {

        @Override
        public void run() {
            try {
                android.util.Log.e("TrackingFlow", "Found: " + remoteDevice.getName());
                UUID uuid = UUID.fromString("4e5d48e0-75df-11e3-981f-0800200c9a66");
                socket = remoteDevice.createRfcommSocketToServiceRecord(uuid);
                socket.connect();
                android.util.Log.e("TrackingFlow", "Connected...");
                os = new OutputStreamWriter(socket.getOutputStream());
                is = socket.getInputStream();
                android.util.Log.e("TrackingFlow", "WWWTTTFFF34243");
                android.util.Log.e("TrackingFlow", "WWWTTTFFF3wwgftggggwww4243: " + CONTINUE_READ_WRITE);
                int bufferSize = 1024;
                int bytesRead = -1;
                byte[] buffer = new byte[bufferSize];
                //Keep reading the messages while connection is open...
                while(CONTINUE_READ_WRITE){
                    android.util.Log.e("TrackingFlow", "WWWTTTFFF3wwwww4243");
                    final StringBuilder sb = new StringBuilder();
                    bytesRead = is.read(buffer);
                    if (bytesRead != -1) {
                        String result = "";
                        while ((bytesRead == bufferSize) && (buffer[bufferSize-1] != 0)){
                            result = result + new String(buffer, 0, bytesRead - 1);
                            bytesRead = is.read(buffer);
                        }
                        result = result + new String(buffer, 0, bytesRead - 1);
                        sb.append(result);
                    }

                    android.util.Log.e("TrackingFlow", "Read: " + sb.toString());

                    //Show message on UIThread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mValueView.append(sb.toString());

                            try {
                                writer.append((sb.toString()));
                                writer.append(',');
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                android.util.Log.e("Error", "Cannot connect");
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
