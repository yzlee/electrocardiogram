package cc.liyongzhi.electrocardiogram;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

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

        mElectrocardiogram = (ElectrocardiogramView) findViewById(R.id.electrocardiogram);
        mElectrocardiogram.setMaxPointAmount(260);
        mElectrocardiogram.setRemovedPointNum(10);
        mElectrocardiogram.setEveryNPoint(10,100);

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        mElectrocardiogram.setLinePoint(msg.arg1, msg.arg2);
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };

        new Thread(){
            public void run() {
                for (int index=0; index<100000; index++)
                {
                    Message message = new Message();
                    message.what = MSG_DATA_CHANGE;
                    message.arg1 = mX;
                    message.arg2 = (int)(Math.random()*200);;

                    try {
                        sleep(30);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mHandler.sendMessage(message);
                    mX += 10;
                }
            };
        }.start();
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
