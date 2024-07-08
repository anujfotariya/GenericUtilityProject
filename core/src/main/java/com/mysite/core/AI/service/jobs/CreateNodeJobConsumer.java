package com.mysite.core.AI.service.jobs;

import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import java.util.Date;

@Component(
        service = JobConsumer.class,
        property = {
                JobConsumer.PROPERTY_TOPICS + "=com/mysite/core/schedulers/NodeCreationScheduler"
        }
)
public class CreateNodeJobConsumer implements JobConsumer {
    private static final Logger log = LoggerFactory.getLogger(CreateNodeJobConsumer.class);

   @Reference
    ResourceHelper resourceHelper;

    @Override
    public JobResult process(Job job) {
        ResourceResolver resourceResolver = null;
        Session session=null;
        try {
            resourceResolver = resourceHelper.getResourceResolver();
            session = resourceResolver.adaptTo(Session.class);
            String path="/content/we-retail/us/en";
            if (session.itemExists(path)) {
                NodeIterator nodes = session.getNode(path).getNodes();

                if(nodes.hasNext())
                {
                    Node rootNode = nodes.nextNode();
                    Node newNode = rootNode.addNode("Date", "nt:unstructured");
                    newNode.setProperty("date", String.valueOf(new Date()));
                    session.save();
                }

            }
            log.info("Node created successfully");
            return JobResult.OK;
        } catch (Exception e) {
            log.error("Error creating node", e);
            return JobResult.FAILED;
        } finally {
            if (resourceResolver != null && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }
}
