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

public class MainActivity extends AppCompatActivity {

    private static final int MSG_DATA_CHANGE = 0x11;
    private ElectrocardiogramView mElectrocardiogram;
    private Handler mHandler;
    private int mX = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);




/*
        mElectrocardiogram = (ElectrocardiogramView) findViewById(R.id.electrocardiogram);
        mElectrocardiogram.setMaxPointAmount(900);
        mElectrocardiogram.setRemovedPointNum(10);
        mElectrocardiogram.setEveryNPoint(10, 50);
        mElectrocardiogram.setEveryNPointRefresh(10);
     //   mElectrocardiogram.setYPosOffset(600);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        mElectrocardiogram.setLinePoint((int)(msg.arg2 * 0.6));
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };

        new Thread() {
            public void run() {

                try {
                    FileIO fileIO = new FileIO();

                    fileIO.readFileSdcardFileBinary(Environment.getExternalStorageDirectory().getAbsolutePath() + "/data111", new FileIO.SendValueInterface() {
                        int i = 0;

                        @Override
                        public void sendValue(int c) {
                            Message message = new Message();
                            message.what = MSG_DATA_CHANGE;
                            try {
                                sleep(4);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            message.arg2 = c;
                            mHandler.sendMessage(message);

                        }
                    }, 2);

*/
/*                    fileIO.readFileSdcardFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/data11.txt", new FileIO.SendValueInterface() {
                        int i = 0;

                        @Override
                        public void sendValue(int c) {
                            Message message = new Message();
                            message.what = MSG_DATA_CHANGE;
                            message.arg2 = c;
                            try {
                                sleep(4);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                            mHandler.sendMessage(message);

                        }
                    });*//*

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            ;
        }.start();
*/
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
}
