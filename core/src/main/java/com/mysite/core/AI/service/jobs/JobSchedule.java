package com.mysite.core.AI.service.jobs;

import com.mysite.core.AI.service.ReGenAIService;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = { JobConsumer.class },
        property = {
                JobConsumer.PROPERTY_TOPICS + "=com/mysite/core/servlets/RegenAiServlet"
        }
)
public class JobSchedule implements JobConsumer{
    Logger logger= LoggerFactory.getLogger(this.getClass());

    @Reference
    ReGenAIService reGenAIService;

    @Reference
    JobManager jobManager;

    @Override
    public JobConsumer.JobResult process(Job job) {
        try
        {

            reGenAIService.getGenAi();
            logger.info("Executed ReGenAI job successfully");
            return JobConsumer.JobResult.OK;
        }
        catch (Exception e)
        {
            logger.error("Error executing ReGenAI job", e);
            return JobConsumer.JobResult.FAILED;
        }
    }

}
