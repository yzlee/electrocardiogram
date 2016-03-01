package cc.liyongzhi.electrocardiogram;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;


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
//                Log.d("File IO :    ", c + "");
        }
    }


    /**
     * 解析16位
     *
     * @param fileName
     * @param send
     * @throws IOException
     */
    public void readFileSdcardFileBinary(String fileName, SendValueInterface send, int channel) throws IOException {

        int c = 0;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        fis = new FileInputStream(fileName);
        if (fis != null) {
            bis = new BufferedInputStream(fis);
        }

        if (bis != null) {

            byte[] bs = new byte[2];
            while (bis.available() > 2) {
                bis.read(bs);
                //符号位

                String s = "";
                int i = 0;

                if (Integer.parseInt("" + (byte) ((bs[0] >> 7) & 0x1)) == 0) {
                    s = "+"; //为正
                    s = s
                            + (byte) ((bs[0] >> 6) & 0x1) + (byte) ((bs[0] >> 5) & 0x1)
                            + (byte) ((bs[0] >> 4) & 0x1) + (byte) ((bs[0] >> 3) & 0x1)
                            + (byte) ((bs[0] >> 2) & 0x1) + (byte) ((bs[0] >> 1) & 0x1)
                            + (byte) ((bs[0] & 0x1)) + (byte) ((bs[1] >> 7) & 0x1)
                            + (byte) ((bs[1] >> 6) & 0x1) + (byte) ((bs[1] >> 5) & 0x1)
                            + (byte) ((bs[1] >> 4) & 0x1) + (byte) ((bs[1] >> 3) & 0x1)
                            + (byte) ((bs[1] >> 2) & 0x1) + (byte) ((bs[1] >> 1) & 0x1)
                            + (byte) ((bs[1]) & 0x1);
                    i = Integer.parseInt(s, 2);
                } else {
                    s = s
                            + (byte) ((bs[0] >> 7) & 0x1)
                            + (byte) ((bs[0] >> 6) & 0x1) + (byte) ((bs[0] >> 5) & 0x1)
                            + (byte) ((bs[0] >> 4) & 0x1) + (byte) ((bs[0] >> 3) & 0x1)
                            + (byte) ((bs[0] >> 2) & 0x1) + (byte) ((bs[0] >> 1) & 0x1)
                            + (byte) ((bs[0] & 0x1)) + (byte) ((bs[1] >> 7) & 0x1)
                            + (byte) ((bs[1] >> 6) & 0x1) + (byte) ((bs[1] >> 5) & 0x1)
                            + (byte) ((bs[1] >> 4) & 0x1) + (byte) ((bs[1] >> 3) & 0x1)
                            + (byte) ((bs[1] >> 2) & 0x1) + (byte) ((bs[1] >> 1) & 0x1)
                            + (byte) ((bs[1]) & 0x1);
                    i = Integer.parseInt(s, 2);
                    i = i - 0xffff - 1;
                }

                Log.i("File Binary", s + "    " + i + "");
                send.sendValue(i);

            }
            bis.close();
            fis.close();
        }
    }
}
