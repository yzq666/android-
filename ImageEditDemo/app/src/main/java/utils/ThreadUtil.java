//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.MessageQueue;
import android.os.MessageQueue.IdleHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadUtil {
    private static final String ASYNC_THREAD_NAME = "single-async-thread";
    private static ThreadUtil sInstance;
    private ThreadPoolExecutor mExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(3);
    private HandlerThread mSingleAsyncThread = new HandlerThread("single-async-thread");
    private Handler mSingleAsyncHandler;
    private Handler mMainHandler;
    private MessageQueue mMsgQueue;

    private ThreadUtil() {
        this.mSingleAsyncThread.start();
        this.mSingleAsyncHandler = new Handler(this.mSingleAsyncThread.getLooper());
        this.mMainHandler = new Handler(Looper.getMainLooper());
        if(Looper.getMainLooper() == Looper.myLooper()) {
            this.mMsgQueue = Looper.myQueue();
        } else {
            Object queue = null;

            try {
                queue = ReflectUtil.getValue(Looper.getMainLooper(), "mQueue");
            } catch (Throwable var3) {
                var3.printStackTrace();
            }

            if(queue instanceof MessageQueue) {
                this.mMsgQueue = (MessageQueue)queue;
            } else {
                this.runOnMainThread(new Runnable() {
                    public void run() {
                        ThreadUtil.this.mMsgQueue = Looper.myQueue();
                    }
                });
            }
        }

    }

    public static ThreadUtil getInstance() {
        if(sInstance == null) {
            sInstance = new ThreadUtil();
        }

        return sInstance;
    }

    public void execute(Runnable task) {
        this.mExecutor.execute(task);
    }

    public void cancel(Runnable task) {
        this.mExecutor.remove(task);
        this.mSingleAsyncHandler.removeCallbacks(task);
        this.mMainHandler.removeCallbacks(task);
    }

    public void destroy() {
        this.mExecutor.shutdownNow();
        this.mSingleAsyncHandler.removeCallbacksAndMessages((Object)null);
        this.mMainHandler.removeCallbacksAndMessages((Object)null);
    }

    public void runOnAsyncThread(Runnable r) {
        this.mSingleAsyncHandler.post(r);
    }

    public void runOnAsyncThread(Runnable r, long delay) {
        this.mSingleAsyncHandler.postDelayed(r, delay);
    }

    public void runOnMainThread(Runnable r) {
        this.mMainHandler.post(r);
    }

    public void runOnMainThread(Runnable r, long delay) {
        this.mMainHandler.postDelayed(r, delay);
    }

    public void runOnIdleTime(final Runnable r) {
        IdleHandler handler = new IdleHandler() {
            public boolean queueIdle() {
                r.run();
                return false;
            }
        };
        this.mMsgQueue.addIdleHandler(handler);
    }
}
