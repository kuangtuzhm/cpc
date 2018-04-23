package com.jimi.cpc.job;

import com.jimi.cpc.service.CpcService;
import com.jimi.cpc.service.DbScanService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpcJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(CpcJob.class);
    @Override
    public void execute(JobExecutionContext context){
        try {
            log.info("-----job start----");
            JobDataMap data = context.getJobDetail().getJobDataMap();
            String path=data.get("config").toString();
            CpcService cpcService=new CpcService(path);
            cpcService.task();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}

