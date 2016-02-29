package cc.liyongzhi.electrocardiogram;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Created by lee on 2/29/16.
 */
public class FileIO {

    public interface SendValueInterface {
        void sendValue(int c);
    }

    public String readFileSdcardFile(String fileName, SendValueInterface send) throws IOException {
        String res="";
        try
        {

            InputStreamReader read = new InputStreamReader(
                    new FileInputStream(fileName),"UTF-8");
            BufferedReader bufferedReader = new BufferedReader(read);


            FileInputStream stream=new FileInputStream(fileName);
            String c;
            while((c=bufferedReader.readLine())!= null)
            {
                send.sendValue(Integer.parseInt(c));
                try {




                    FileOutputStream fout = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/data11111.txt");

                    Log.d("File IO :    ", c + "");
                    fout.write(Integer.parseInt(c));
                    fout.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return res;
    }
}
