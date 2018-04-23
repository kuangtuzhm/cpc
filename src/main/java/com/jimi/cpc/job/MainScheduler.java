package com.jimi.cpc.job;

import com.jimi.cpc.util.PropertiesUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.quartz.CronScheduleBuilder.cronSchedule;

import java.net.URL;
public class MainScheduler {
	
	private static final Logger log = LoggerFactory.getLogger(MainScheduler.class);
    //创建调度器
    public static Scheduler getScheduler() throws SchedulerException {
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        return schedulerFactory.getScheduler();
    }


    public static void schedulerJob(String path) throws SchedulerException{
        PropertiesUtils propertiesUtils = new PropertiesUtils(path);
        
        String dbscan_cron_conifg=propertiesUtils.get("dbscan.scheduler.cron.config");
        log.info("dbscan_cron_conifg:" + dbscan_cron_conifg);
        
        String cpc_cron_conifg=propertiesUtils.get("cpc.scheduler.cron.config");
        log.info("cpc_cron_conifg:" + cpc_cron_conifg);
        
        JobDataMap jobDataMap =new JobDataMap();
        jobDataMap.put("config",path);
        //创建任务
        JobDetail jobDetail1 = JobBuilder.newJob(DbScanJob.class).setJobData(jobDataMap).withIdentity("job1", "group1").build();
        JobDetail jobDetail2 = JobBuilder.newJob(CpcJob.class).setJobData(jobDataMap).withIdentity("job2", "group1").build();
        //创建触发器 每3秒钟执行一次
        Trigger trigger1 = TriggerBuilder.newTrigger().withIdentity("trigger1", "group3")
                .withSchedule(cronSchedule(dbscan_cron_conifg))
                .build();
        Trigger trigger2 = TriggerBuilder.newTrigger().withIdentity("trigger2", "group3")
                .withSchedule(cronSchedule(cpc_cron_conifg))
                .build();
        Scheduler scheduler = getScheduler();
        //将任务及其触发器放入调度器
        scheduler.scheduleJob(jobDetail1, trigger1);
        scheduler.scheduleJob(jobDetail2, trigger2);
        //调度器开始调度任务
        scheduler.start();

    }

    public static void main(String[] args) throws SchedulerException {
    	/* ClassLoader classLoader = MainScheduler.class.getClassLoader();  
         URL resource = classLoader.getResource("param.properties");  
         String path = resource.getPath(); */ 
         
        MainScheduler mainScheduler = new MainScheduler();
        mainScheduler.schedulerJob(args[0]);
    }
}
