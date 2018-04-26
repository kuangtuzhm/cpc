package com.jimi.cpc.dbscan;

import java.util.List;

/**
 * 聚类集群
 * @author  yuanshao
 */
public class Cluster implements Comparable<Cluster>{
    private List<Point> plist;
    private Point center;
    private Point mean;
    private int num;
    private String combineMacs;
    
    private int type ;  //0  0：家  ， 1：学校
    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public List<Point> getPlist() {
        return plist;
    }

    public void setPlist(List<Point> plist) {
        this.plist = plist;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public Point getMean() {
        return mean;
    }

    public void setMean(Point mean) {
        this.mean = mean;
    }

    public String getCombineMacs() {
        return combineMacs;
    }

    public void setCombineMacs(String combineMacs) {
        this.combineMacs = combineMacs;
    }

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public int compareTo(Cluster o) {
		int size = 0;
		int oSize = 0;
		if(this.plist != null)
		{
			size = this.plist.size();
		}
		if(o.plist != null)
		{
			oSize = o.plist.size();
		}
		return (oSize - size);
	}
    
}
