package com.example.hrker.cpu_temp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {


    private TextView tvDOS, tvDAPI, tvDNumCore, tvChipSet, tvCPUF, tvTemperature, tvTempType;
    public String deviceOS = Build.VERSION.RELEASE;
    public int deviceAPI = Build.VERSION.SDK_INT;
    private CalcTemp CT;
    private Intent tempIntent;

    private Switch mSwitch;
    private boolean mTempType = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        init();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        //floating button right side
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Scanning Device", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                updateTV();

            }
        });
    }

    private void init() {
        CT = new CalcTemp();
        tvDOS = (TextView) findViewById(R.id.tvDOS);
        tvDAPI = (TextView) findViewById(R.id.tvDAPI);
        tvDNumCore = (TextView) findViewById(R.id.tvDNumCores);
        tvCPUF = (TextView) findViewById(R.id.tvCPUF);
        tvChipSet = (TextView) findViewById(R.id.tvChipSet);

        tvTemperature = (TextView) findViewById(R.id.tvTemperature);
        tvTempType = (TextView) findViewById(R.id.tempType);

        mSwitch = (Switch) findViewById(R.id.sTempType);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mTempType = b;

                if (b) {
                    // fahrenheit
                    tvTempType.setText("\u00B0F");
                } else {
                    // celsius
                    tvTempType.setText("\u00B0C");
                }
            }
        });
        tvDOS.setText("OS: " + deviceOS);
        tvDAPI.setText("API: " + deviceAPI);
        tvDNumCore.setText("Num Cores: " + getNumCores());

        // start the temperature service
        tempIntent = new Intent(this, TempService.class);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTemp(intent);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        startService(tempIntent);
        registerReceiver(broadcastReceiver, new IntentFilter(TempService.BROADCAST_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        stopService(tempIntent);
    }

    private void updateTemp(Intent intent) {
        int value = intent.getIntExtra("temperature", 0);
        if (mTempType) {
            // fahrenheit
            value = (int) (value * 1.8) + 32;
        }
        tvTemperature.setText(value + " ");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateTV() {
        tvChipSet.setText(getInfo());
        CT.printCPUFreq(tvCPUF);
    }

    private String getInfo() {
        StringBuffer sb = new StringBuffer();

        sb.append("abi: ").append(Build.CPU_ABI).append("n");
        if (new File("/proc/cpuinfo").exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
                String aLine;
                while ((aLine = br.readLine()) != null) {
                    sb.append(aLine + "n");
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }


    private int getNumCores() {
        if(Build.VERSION.SDK_INT >= 17) {
            return Runtime.getRuntime().availableProcessors();
        }
        else {
            return getNumCoresOld();
        }
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     * @return The number of cores, or 1 if failed to get result
     */
    private int getNumCoresOld() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if(Pattern.matches("cpu[0-9]+", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch(Exception e) {
            //Default to return 1 core
            return 1;
        }
    }
}
