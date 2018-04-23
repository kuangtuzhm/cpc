package com.jimi.cpc.dbscan;

import com.jimi.cpc.dao.MysqlDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jason on 2016/4/17.
 *
 * @author beni
 * dbscan算法实现
 */
public class DBScan {
    private static final Logger log = LoggerFactory.getLogger(DBScan.class);
    private double radius;
    private int minPts;

    public DBScan(double radius, int minPts) {
        this.radius = radius / 100000;
        this.minPts = minPts;
    }

    private List<Point> getAdjacentPoints(Point corePoint, List<Point> points) {
        ArrayList<Point> adjacentPoints = new ArrayList<Point>();
        for (Point p : points) {
            //include centerPoint itself
            double distance = getDistance(corePoint, p);
            if (distance <= radius) {
                adjacentPoints.add(p);
            }
        }
        return adjacentPoints;
    }

    public void process(List<Point> points) {
        long begimTime = System.currentTimeMillis();
        int size = points.size();
        int id = 0;
        int cluster = 0;
        for (Point p : points) {
            if (!p.getVisit()) {
                p.setVisit(true);
                List<Point> neighbors = getAdjacentPoints(p, points);
                if (neighbors.size() >= minPts) { //核心点
                    p.setCore(true);
                    if (p.getCluster() == 0) {
                        p.setCluster(cluster++);
                        expandCluster(points, p, neighbors, cluster);
                    } else {
                        id = p.getCluster(); //如果p核心点已经分类了，那么返回它原本属于的簇序号 进行扩张
                        expandCluster(points, p, neighbors, id);
                    }
                } else {
                    p.setCluster(-1);//边界点和噪音点，统一标注为噪音点，在集群标记时，会统一还原过来
                }
            }
        }
        log.info("dbscan task time:" + (System.currentTimeMillis() - begimTime) + " points size:" + size);
    }

    /**
     * @param points    扩张簇的数据
     * @param corePoint 核心点 需要设置簇ID
     * @param neighbors 核心点p的所有Eps邻域点
     * @param cluster   簇的ID号
     */
    public void expandCluster(List<Point> points, Point corePoint, List<Point> neighbors, int cluster) {
        corePoint.setCluster(cluster);
        for (Point p : neighbors) {
            if (!p.getVisit()) {
                p.setVisit(true);
                List<Point> adjacents = getAdjacentPoints(p, points);
                if (adjacents.size() >= minPts) {
                    p.setCore(true);
                    for (Point pp : adjacents) {
                        if (pp.getCluster() == 0 || p.getCluster() == -1) { //未分配或者为噪音点
                            pp.setCluster(cluster);
                        }
                    }
                }
            }
            if (p.getCluster() == 0 || p.getCluster() == -1) {//未分配或者为噪音点
                p.setCluster(cluster);
            }
        }
    }

    public Point getMean(List<Point> points) {
//        long beginTime = System.currentTimeMillis();
        double x = 0.0;
        double y = 0.0;
        for (Point p : points) {
            x += p.getX();
            y += p.getY();
        }
        double x_mean = x / points.size();
        double y_mean = y / points.size();
//        System.out.println("dbscan getMean task time:" + (System.currentTimeMillis() - beginTime));
        return new Point(x_mean, y_mean);
    }

    public Point getCenter(List<Point> points, Point mean) {
        Point min_p = points.get(0);
        double min = getDistance(min_p, mean);
        for (Point p : points) {
            double distance = getDistance(p, mean);
            if (distance < min) {
                min_p = p;
                min = distance;
            }
        }
        return min_p;
    }

    private double getDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
    }

    public List<Cluster> getCluster(List<Point> points) {
//        long beginTime = System.currentTimeMillis();
        Map<Integer, Cluster> map = new HashMap<Integer, Cluster>();
        for (Point p : points) {
            if (p.getCluster() > 0) {
                int clusterNum = p.getCluster();
                Cluster cluster = map.get(clusterNum);
                if (cluster == null) {
                    cluster = new Cluster();
                    cluster.setNum(clusterNum);
                }
                List<Point> plist = cluster.getPlist();
                if (plist == null) {
                    plist = new ArrayList<Point>();
                }
                plist.add(p);
                cluster.setPlist(plist);
                map.put(clusterNum, cluster);
            }
        }
        List<Cluster> clist = new ArrayList<Cluster>();
        for (int key : map.keySet()) {
            Cluster cluster = map.get(key);
            List<Point> plist = cluster.getPlist();
            Point mean = getMean(plist);
            Point center = getCenter(plist, mean);
            cluster.setMean(mean);
            cluster.setCenter(center);
            clist.add(cluster);
        }
//        System.out.println("dbscan getCluster task: " + (System.currentTimeMillis() - beginTime) + " cluster num:" + clist.size());
        return clist;
    }
}
