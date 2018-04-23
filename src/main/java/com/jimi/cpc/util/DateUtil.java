package com.jimi.cpc.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class DateUtil {

    private final static int[] hour_array = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    private final static int[] minute_array = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59};
    private static Random random = new Random();

    /**
     * 返回今天日期 yyyyMddHH
     *
     * @return
     */
    public static String getDateNow() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(new Date());
    }

    public static String getDateNow(String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(new Date());
    }

    /**
     * 返回今天日期的前num 天yyyyMMdd
     *
     * @return
     */
    public static String getDateBefore(String format,int beforeNum) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, -beforeNum);
        return simpleDateFormat.format(cal.getTime());
    }
    /**
     * 返回今天日期的前num 月yyyyMMdd
     *
     * @return
     */
    public static String getMonthBefore(int beforeNum) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, -beforeNum);
        return simpleDateFormat.format(cal.getTime());
    }
    public static int getMonth(String strDate) {
        int month=0;
        try {
            Date date=new SimpleDateFormat("yyyy-MM-dd").parse(strDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            month = cal.get(Calendar.MONTH);
        }catch (Exception e){
            e.printStackTrace();
        }
        return month;
    }


    /**
     * 返回静态日期的前一个yyyyMMddHHmmss
     *
     * @param date
     * @return
     */
    public static String getTimeBefore(String date) {
        int hourRandom = hour_array[random.nextInt(hour_array.length)];
        String hour = hourRandom < 10 ? "0" + hourRandom : "" + hourRandom;
        int minuteRandom = minute_array[random.nextInt(minute_array.length)];
        String minute = minuteRandom < 10 ? "0" + minuteRandom : "" + minuteRandom;
        int secondRandom = minute_array[random.nextInt(minute_array.length)];
        String second = secondRandom < 10 ? "0" + secondRandom : "" + secondRandom;
        String dateStr = date + hour + minute + second;
        return dateStr;
    }

    /**
     * 随机时间
     */
    public static String getRandomTime(String startTime, String endTime) throws Exception {
        Calendar calendar = Calendar.getInstance();
        long stime = getTime(startTime);
        long etime = getTime(endTime);
        //得到大于等于min小于max的double值
        double randomDate = Math.random() * (etime - stime) + stime;
        //将double值舍入为整数，转化成long类型
        calendar.setTimeInMillis(Math.round(randomDate));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(calendar.getTime());
    }

    /**
     * 返回当前时间
     *
     * @return
     */
    public static String getTimeNow() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date());
    }

    public static String getTimeNow(String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(new Date());
    }

    /**
     * 返回特定时间，前m分钟
     *
     * @return
     */
    public static String getEarlyMinuteTime(int earlyMinute) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.MINUTE, -earlyMinute);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(c.getTime());
    }

    public static long getTime(String dateTime) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = format.parse(dateTime);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.getTimeInMillis();
    }

    /**
     * 返回毫秒
     *
     * @param dateTime1
     * @param dateTime2
     * @return
     * @throws Exception
     */
    public static long compareTime(String dateTime1, String dateTime2) throws Exception {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c1 = Calendar.getInstance();
        c1.setTime(format.parse(dateTime1));
        Calendar c2 = Calendar.getInstance();
        c2.setTime(format.parse(dateTime2));
        return c1.getTimeInMillis() - c2.getTimeInMillis();
    }

    /***
     * 获取当天的最后一毫秒
     * @return
     */
    public static long getTodayLastMillis() {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(new Date());
        c1.set(Calendar.HOUR, 24);
        c1.set(Calendar.MINUTE, 0);
        c1.set(Calendar.SECOND, 0);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(new Date());
        return c1.getTimeInMillis() - c2.getTimeInMillis();
    }

    public static int getTodayYear() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy");
        String year = format.format(new Date());
        return Integer.parseInt(year);
    }

    public static void main(String[] args) throws Exception {
       /* SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, -30);
        System.out.println(simpleDateFormat.format(cal.getTime()));
*/

    	System.out.println(Integer.parseInt("2018-03-30 09:12:11:0".substring(11, 13)));
    }
}
