package com.jimi.cpc.dbscan;

/**
 * 聚类点
 * @author  yuanshao
 */
public class Point {
    private String imei;
    private String macs;
    private double x;
    private double y;
    private boolean isVisit;
    private int cluster;
    private boolean isCore;

    private int hour ; 
    
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        this.isVisit = false;
        this.cluster = 0;
    }
    public Point(String imei,String macs,double x, double y) {
        this.imei=imei;
        this.macs=macs;
        this.x = x;
        this.y = y;
        this.isVisit = false;
        this.cluster = 0;
    }
    
    
    public Point(String imei, String macs, double x, double y, int hour) {
		this.imei = imei;
		this.macs = macs;
		this.x = x;
		this.y = y;
		this.isVisit = false;
	    this.cluster = 0;
	    this.hour = hour;
	}
	public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getMacs() {
        return macs;
    }

    public void setMacs(String macs) {
        this.macs = macs;
    }

    public boolean isCore() {
        return isCore;
    }

    public void setCore(boolean core) {
        isCore = core;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getX() {
        return x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getY() {
        return y;
    }

    public void setVisit(boolean isVisit) {
        this.isVisit = isVisit;
    }

    public boolean getVisit() {
        return isVisit;
    }

    public int getCluster() {
        return cluster;
    }

    public void setCluster(int cluster) {
        this.cluster = cluster;
    }

    
    public int getHour() {
		return hour;
	}
	public void setHour(int hour) {
		this.hour = hour;
	}
	@Override
    public String toString() {
        return x+" "+y+" "+cluster+" "+(isCore?1:0);
    }

}
