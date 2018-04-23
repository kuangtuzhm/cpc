package com.jimi.cpc.test;

import com.jimi.cpc.util.DateUtil;

import java.util.*;

public class Testss {
    public static void main(String[] args) {
        String date=DateUtil.getTimeNow();
        System.out.println(date);
        System.out.println(date.substring(5,7));
        System.out.println(DateUtil.getMonth(date));
        String datebefore=DateUtil.getMonthBefore(1);
        System.out.println(datebefore);
        System.out.println(datebefore.substring(5,7));
        System.out.println(DateUtil.getMonth(datebefore));
        String uuid= UUID.randomUUID().toString();
        System.out.println(uuid.replace("-",""));

    }
}
