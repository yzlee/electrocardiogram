package cc.liyongzhi.ecgview;

/**
 * Created by lee on 7/9/16.
 */
public class DataChannel<E> {

    private int maxCacheLength = 2000; // max cache data length
    private int maxThreshold = 1000; // when cached data number is overtake this parameter
    private int minThreshold = 0;
    private int currentDataLength = 0;
    private Object[] cachedData;


    private int queueHead = 0;
    private int queueTail = 0;

    public DataChannel () {
        init();
    }

    public DataChannel (int maxCacheLength, int maxThreshold, int minThreshold) {
        this.maxCacheLength = maxCacheLength;
        this.maxThreshold = maxThreshold;
        this.minThreshold = minThreshold;
        init();
    }

    private void init() {
        cachedData = new Object[this.maxCacheLength];

    }

    public void putData(E e) {

        cachedData[queueTail] = e;

        if (queueTailPlus1() == queueHead) {
            queueHead = queueHeadPlus1();
            queueTail = queueTailPlus1();
        } else {
            queueTail = queueTailPlus1();
        }

    }

    @SuppressWarnings("unchecked") public E getData() {

        Object o;

        if (queueHead == queueTail) {
            o = null;
        } else {
            o = cachedData[queueHead];
            queueHead = queueHeadPlus1();
        }

        return (E) o;
    }

    private int queueTailPlus1() {
        int tail = queueTail;
        tail++;
        if (tail == maxCacheLength) {
            tail = 0;
        }
        return tail;
    }

    private int queueHeadPlus1() {
        int head = queueHead;
        head++;
        if (head == maxCacheLength) {
            head = 0;
        }
        return head;
    }



    public boolean highCacheLengthAlert() {
        return currentDataLength > maxThreshold;
    }

    public boolean lowCacheLengthAlert() {
        return currentDataLength < minThreshold;
    }

    public int getCurrentDataLength() {
        return currentDataLength;
    }

}
