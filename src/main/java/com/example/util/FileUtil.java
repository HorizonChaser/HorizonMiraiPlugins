package com.example.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtil {

    /**
     * 根据 URL 下载文件
     * @param urlString URL
     * @param filename 文件名
     * @throws Exception 下载异常
     */
    public static void download(@NotNull String urlString, @NotNull String filename) throws Exception {
        // 构造URL
        URL url = new URL(urlString);
        // 打开连接
        URLConnection con = url.openConnection();
        InputStream is = con.getInputStream();
        // 1K的数据缓冲
        byte[] bs = new byte[1024];
        int len;
        File file = new File(filename);

        if(!file.exists()){
            file.createNewFile();
        }

        FileOutputStream os = new FileOutputStream(file, true);
        while ((len = is.read(bs)) != -1) {
            os.write(bs, 0, len);
        }
        os.close();
        is.close();
    }

    /**
     * 返回时间戳字符串
     * @return 时间戳字符串 yyyy-MM-dd HH:mm:ss
     */
    public static String getTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    }

}
