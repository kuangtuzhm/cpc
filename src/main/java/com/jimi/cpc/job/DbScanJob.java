package com.jimi.cpc.job;

import com.jimi.cpc.service.DbScanService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbScanJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(DbScanJob.class);
    @Override
    public void execute(JobExecutionContext context){
        try {
            log.info("-----job start----");
            DbScanService dbScanServie=new DbScanService();
            dbScanServie.task();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
