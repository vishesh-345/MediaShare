package admin.encryption;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;

public class HomeActivity extends AppCompatActivity {


    private BluetoothAdapter btAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        initialize();
        initializeWidget();
        registerCallback();
    }

    @Override
    public void onBackPressed() {
        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }else {
            super.onBackPressed();
        }
    }

    private void registerCallback(){
        IntentReceiver receiver = new IntentReceiver();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver,filter);
    }

    private void initialize(){

        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null) {
            Toast.makeText(this,"device does not supports !!!",Toast.LENGTH_LONG).show();
        }
    }

    private void initializeWidget() {


        TextView deviceTitle = findViewById(R.id.device_name);
        TextView deviceMac = findViewById(R.id.device_mac);

        deviceTitle.setText(btAdapter.getName());
        deviceMac.setText(btAdapter.getAddress());

        SwitchCompat switchCompat = findViewById(R.id.switcher);
        switchCompat.setChecked(isBluetoothEnabled());
        CompoundButton.OnCheckedChangeListener onCheckedChangeListener = (button, checked) -> {

            boolean enabled = isBluetoothEnabled();

            if(checked == true && enabled == false) {
                setEnable(true);
            }else if(checked == false && enabled == true){
                setEnable(false);
            }
        };
        switchCompat.setOnCheckedChangeListener(onCheckedChangeListener);
        //switchCompat.setOnClickListener(param -> startActivity(new Intent(this,ScannerActivity.class)));

        FragmentPagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        ViewPager pager = findViewById(R.id.view_pager);
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageLimit(1);
        TabLayout layout = findViewById(R.id.tab_layout);
        layout.setupWithViewPager(pager);

        View.OnClickListener receiveClickListener = param->{

            if(!isBluetoothEnabled()){
                Toast.makeText(this,"Enable Bluetooth First !!!",Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(this,ReceiverActivity.class);
            startActivity(intent);
        };
        AppCompatButton appCompatFileReceive = findViewById(R.id.file_receive_button);
        appCompatFileReceive.setOnClickListener(receiveClickListener);

        AppCompatButton appCompatChatReceive = findViewById(R.id.chat_receive_button);
        appCompatChatReceive.setOnClickListener(receiveClickListener);

        View.OnClickListener fileSendClickListener = param->{

            if(!isBluetoothEnabled()){
                Toast.makeText(this,"Enable Bluetooth First !!!",Toast.LENGTH_LONG).show();
                return;
            }
            BluetoothConfiguration.PROTOCOL = "FILE_PROTOCOL";
            FilePickerBuilder.getInstance().setMaxCount(5).pickFile(HomeActivity.this);
        };
        AppCompatButton fileSender = findViewById(R.id.file_send_button);
        fileSender.setOnClickListener(fileSendClickListener);

        View.OnClickListener chatSendClickListener = param->{

            if(!isBluetoothEnabled()){
                Toast.makeText(this,"Enable Bluetooth First !!!",Toast.LENGTH_LONG).show();
                return;
            }

            BluetoothConfiguration.PROTOCOL = "CHAT_PROTOCOL";
            Intent intent = new Intent(HomeActivity.this,DeviceSelectActivity.class);
            startActivity(intent);
        };
        AppCompatButton chatSender = findViewById(R.id.chat_send_button);
        chatSender.setOnClickListener(chatSendClickListener);

    }

    private void setEnable(boolean enable) {

        if(enable == true){
            btAdapter.enable();
        }else {
            btAdapter.disable();
        }
    }

    private boolean isBluetoothEnabled() {

        return btAdapter.isEnabled();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case FilePickerConst.REQUEST_CODE_DOC:
                if(resultCode== Activity.RESULT_OK && data!=null) {
                    ArrayList paths = new ArrayList<>();
                    paths.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS));

                    Intent intent = new Intent(this,DeviceSelectActivity.class);
                    intent.putStringArrayListExtra("files",paths);
                    startActivity(intent);
                }
                break;
        }
    }

    class IntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            ViewPager pager = findViewById(R.id.view_pager);
            View parent = pager.getChildAt(1);

            String message = null;
            String action = intent.getAction();

            switch (action){
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    message = "DISCOVERY STARTED";

                    break;
                case BluetoothDevice.ACTION_FOUND:
                    message = "PAIRED DEVICE FOUND";

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    RecyclerView list = parent.findViewById(R.id.recycler_view);
                    DeviceAdapter deviceAdapter = (DeviceAdapter) list.getAdapter();
                    if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                        deviceAdapter.addChild(device);
                        message = "NEW DEVICE FOUND";
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    message = "DISCOVERY FINISHED";

                    SwipeRefreshLayout layout = parent.findViewById(R.id.swipe_refresh_layout);
                    if(layout.isRefreshing()){
                        layout.setRefreshing(false);
                    }
                    break;
                default:
            }
            Snackbar.make(pager,message,Snackbar.LENGTH_LONG).show();
        }
    }
}
