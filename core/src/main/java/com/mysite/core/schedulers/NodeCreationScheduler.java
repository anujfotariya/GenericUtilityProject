package com.mysite.core.schedulers;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(service = Runnable.class,immediate = true)
public class NodeCreationScheduler implements Runnable{
    private static final Logger log = LoggerFactory.getLogger(NodeCreationScheduler.class);
    private static final String JOB_TOPIC = "com/mysite/core/schedulers/NodeCreationScheduler";

    @Reference
    private Scheduler scheduler;

    @Reference
    private JobManager jobManager;

    @Activate
    protected void activate() {
        try {
            ScheduleOptions scheduleOptions = scheduler.EXPR("0 */5 * * * ?").name("nodeCreationJob").canRunConcurrently(false);
            scheduler.schedule(this, scheduleOptions);
            log.info("Node creation job scheduled successfully to run every 5 minutes");
        } catch (Exception e) {
            log.error("Error scheduling node creation job", e);
        }
    }

    @Override
    public void run() {
        try {
            Map<String, Object> jobProperties = new HashMap<>();
           // jobProperties.put("anyKey", "anyValue"); // Add properties if needed

            jobManager.createJob(JOB_TOPIC).properties(jobProperties).add();
            log.info("Node creation job triggered");
        } catch (Exception e) {
            log.error("Error triggering node creation job", e);
        }

    }
}
