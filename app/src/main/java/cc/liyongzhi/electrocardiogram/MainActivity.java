package cc.liyongzhi.electrocardiogram;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import cc.liyongzhi.ecgview.ECGView;

public class MainActivity extends AppCompatActivity {

    private ECGView view;
    private LinkedBlockingQueue queue = new LinkedBlockingQueue();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        new DataGeneratingThread(queue).start();



    }

    @Override
    protected void onResume() {
        super.onResume();
        view = (ECGView) findViewById(R.id.ecg_view);
        view.setSubViewNum(12);
        view.setColumnSubViewNum(2);
        ArrayList<String> text = new ArrayList<>();
        String[] s = new String[]{"I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"};
        Collections.addAll(text, s);
        view.setText(text);
        view.setInputChannelNum(12);
        view.setChannel(queue);
        view.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        view = null;
    }

}
