//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package utils;

import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;

public class LogUtil {
    public static String LOG_DIR = "ALog";
    public static boolean sIsLog = true;
    public static final String LOG_TAG = "log";

    public LogUtil() {
    }

    public static void v(String msg) {
        v("log", msg);
    }

    public static void d(String msg) {
        d("log", msg);
    }

    public static void i(String msg) {
        i("log", msg);
    }

    public static void w(String msg) {
        w("log", msg);
    }

    public static void e(String msg) {
        e("log", msg);
    }

    public static void v(String tag, String msg) {
        if(sIsLog) {
            Log.v(tag, msg);
        }

    }

    public static void v(String tag, String msg, Throwable tr) {
        if(sIsLog) {
            Log.v(tag, msg, tr);
        }

    }

    public static void d(String tag, String msg) {
        if(sIsLog) {
            Log.d(tag, msg);
        }

    }

    public static void d(String tag, String msg, Throwable tr) {
        if(sIsLog) {
            Log.d(tag, msg, tr);
        }

    }

    public static void i(String tag, String msg) {
        if(sIsLog) {
            Log.i(tag, msg);
        }

    }

    public static void i(String tag, String msg, Throwable tr) {
        if(sIsLog) {
            Log.i(tag, msg, tr);
        }

    }

    public static void w(String tag, String msg) {
        if(sIsLog) {
            Log.w(tag, msg);
        }

    }

    public static void w(String tag, String msg, Throwable tr) {
        if(sIsLog) {
            Log.w(tag, msg, tr);
        }

    }

    public static void w(String tag, Throwable tr) {
        if(sIsLog) {
            Log.w(tag, tr);
        }

    }

    public static void e(String tag, String msg) {
        if(sIsLog) {
            Log.e(tag, msg);
        }

    }

    public static void e(String tag, String msg, Throwable tr) {
        if(sIsLog) {
            Log.e(tag, msg, tr);
        }

    }

    public static boolean writeLog(String log, String dirPath) {
        File sdCardDir;
        if(Environment.getExternalStorageState().equals("mounted")) {
            sdCardDir = Environment.getExternalStorageDirectory();
        } else {
            sdCardDir = new File("/storage/sdcard1");
        }

        if(!sdCardDir.exists()) {
            return false;
        } else {
            try {
                if(dirPath == null) {
                    dirPath = LOG_DIR;
                }

                String e = DateUtil.getDate();
                log = "\r\n" + e + "==>" + log;
                String fileName = "log-" + e + ".txt";
                File dir = new File(sdCardDir, dirPath);
                if(!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(log.getBytes());
                fos.close();
                return true;
            } catch (Exception var8) {
                Log.e("LogUtil", var8.getMessage());
                return false;
            }
        }
    }

    public static boolean writeLog(String log) {
        return writeLog(log, (String)null);
    }
}
