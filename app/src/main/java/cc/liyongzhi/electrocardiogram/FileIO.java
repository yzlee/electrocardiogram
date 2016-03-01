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

    public void readFileSdcardFile(String fileName, SendValueInterface send) throws IOException {
            InputStreamReader read = new InputStreamReader(
                    new FileInputStream(fileName), "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(read);
            String c;
            while ((c = bufferedReader.readLine()) != null) {
                send.sendValue(Integer.parseInt(c));
                Log.d("File IO :    ", c + "");
            }
    }

    public void readFileSdcardFileBinary(String fileName, SendValueInterface send) throws IOException {

    }
}
