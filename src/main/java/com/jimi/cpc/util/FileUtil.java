package com.jimi.cpc.util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    public static void write(String path, String data) {
        BufferedWriter writer = null;
        try {
            File file = new File(path);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "utf-8"));
            writer.write(data + "\n");
        } catch (Exception e) {
            log.warn("写文件异常：", e.getMessage());
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.warn("写文件资源释放异常：", e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public static void write(String path, List<String> datalist, boolean append) {
        BufferedWriter writer = null;
        try {
            if (datalist == null || datalist.size() == 0) {
                return;
            }
            File file = new File(path);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            if (append) {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "utf-8"));
            } else {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
            }

            for (String data : datalist) {
                writer.write(data + "\n");
            }
            datalist.clear();
        } catch (Exception e) {
            log.warn("写文件异常：", e.getMessage());
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.warn("写文件资源释放异常：", e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<String> read(File file) {
        List<String> list = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String s = null;
            while ((s = reader.readLine()) != null) {
                list.add(s.trim());
            }

        } catch (Exception e) {
            log.warn("写文件异常：", e.getMessage());
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("写文件资源释放异常：", e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return list;
    }


}
