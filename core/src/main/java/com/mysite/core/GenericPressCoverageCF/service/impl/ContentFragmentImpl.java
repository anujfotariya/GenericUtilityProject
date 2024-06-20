package com.mysite.core.GenericPressCoverageCF.service.impl;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.replication.Replicator;
import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
import com.mysite.core.GenericPressCoverageCF.service.ContentFragmentService;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(service = ContentFragmentService.class,immediate = true)
public class ContentFragmentImpl implements ContentFragmentService {

    @Reference
    ResourceHelper resourceHelper;

    @Reference
    Replicator replicator;

    Logger logger = LoggerFactory.getLogger(ContentFragmentImpl.class);

    @Override
    public void createContentFragment() throws ContentFragmentException, ReplicationException {

        Session session = null;
        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceHelper.getResourceResolver();
            session = resourceResolver.adaptTo(Session.class);

            String Path = "/content/futuregeneraliindiainsurancecoltd/us/en/about-us/media-center/jcr:content/root/responsivegrid/tabsformediacenter/yearmultiField";
            Resource cfmresource = resourceResolver.getResource("/conf/futuregeneraliindiainsurancecoltd/settings/dam/cfm/models/presscoverage");


            if (session.itemExists(Path)) {
                NodeIterator nodeIterator = session.getNode(Path).getNodes();

                while (nodeIterator.hasNext()) {
                    Node outerNode = nodeIterator.nextNode();
                    logger.info("Outer Node :{}", outerNode.getName());

                    if (outerNode.hasProperty("year")) {
                        Resource CFPath = resourceResolver.getResource("/content/dam/futuregeneraliindiainsurancecoltd/fg-contentfragment/press-coverage");
                        Node yearFolder = null;
                        Node yearNode = CFPath.adaptTo(Node.class);

                        boolean nodeExists = doesNodeExist(yearNode, outerNode.getProperty("year").getString());

                        if (!nodeExists) {
                            yearFolder = yearNode.addNode(outerNode.getProperty("year").getString(), "sling:Folder");
                            yearFolder.setProperty("jcr:title", outerNode.getProperty("year").getString());
                            session.save();
                        }



                        NodeIterator monthmultifielditerator = outerNode.getNodes();

                        while (monthmultifielditerator.hasNext()) {
                            Node monthmultifieled = monthmultifielditerator.nextNode();
                            NodeIterator monthIterator = monthmultifieled.getNodes();

                            while (monthIterator.hasNext()) {
                                Node month = monthIterator.nextNode();
                                logger.info("Month Node", month);
                                if (month.hasProperty("month")) {
                                    Node monthFolder = null;
                                    String cfPath = CFPath.getPath() + "/" + outerNode.getProperty("year").getString();
                                    Node monthNode = resourceResolver.getResource(cfPath).adaptTo(Node.class);

                                    boolean monthnodeExists = doesNodeExist(monthNode, month.getProperty("month").getString());

                                    if (!monthnodeExists) {
                                        monthFolder = monthNode.addNode(month.getProperty("month").getString(), "sling:Folder");
                                        monthFolder.setProperty("jcr:title", month.getProperty("month").getString());
                                        // Set properties or perform other operations on the new node if needed
                                        session.save(); // Save the changes
                                    }



                                    NodeIterator tabmultifieldIterator = month.getNodes();
                                    while (tabmultifieldIterator.hasNext()) {
                                        Node tabmultifield = tabmultifieldIterator.nextNode();
                                        NodeIterator cfItr = tabmultifield.getNodes();
                                        ContentFragment contentFragment = null;
                                        while (cfItr.hasNext()) {
                                            Node cfNode = cfItr.nextNode();
                                            logger.info("cf Node", cfNode);
                                            String date = cfNode.hasProperty("date") ? cfNode.getProperty("date").getString() : "";
                                            String link = cfNode.hasProperty("linkredirection") ? cfNode.getProperty("linkredirection").getString() : "";
                                            String tabName = cfNode.hasProperty("tabname") ? cfNode.getProperty("tabname").getString() : "";
                                            String tabTitle = cfNode.hasProperty("tabtitle") ? cfNode.getProperty("tabtitle").getString() : "";
                                            String regexTabName = tabName.replaceAll("[/.:\\[\\]*|\"?{}\\n\\r\\t -]", "_");

                                            FragmentTemplate fragmentTemplate = cfmresource.adaptTo(FragmentTemplate.class);
                                            Resource CfPath = resourceResolver.getResource("/content/dam/futuregeneraliindiainsurancecoltd/fg-contentfragment/press-coverage/" + outerNode.getProperty("year").getString() + "/" + month.getProperty("month").getString() + "");
                                            Node parentCfnode = CfPath.adaptTo(Node.class);
                                            //NodeIterator parentcfIterator= parentCfnode.getNodes();
                                            //create content fragment for first time
//                                                if(!parentcfIterator.hasNext())
//                                                {
//                                                    contentFragment = fragmentTemplate.createFragment(CfPath, tabName, "");
//
//                                                    ContentElement contentElement = contentFragment.getElement("date");
//                                                    contentElement.setContent(date, "");
//
//                                                    contentElement = contentFragment.getElement("linkredirection");
//                                                    contentElement.setContent(link, "");
//
//                                                    contentElement = contentFragment.getElement("tabname");
//                                                    contentElement.setContent(tabName, "");
//
//                                                    contentElement = contentFragment.getElement("tabtitle");
//                                                    contentElement.setContent(tabTitle, "");
//
//                                                    replicator.replicate(session, ReplicationActionType.ACTIVATE, CfPath.getPath());
//                                                    ReplicationStatus replicationStatus = replicator.getReplicationStatus(session, CfPath.getPath());
//
//                                                    this.logger.info("Is Activated: {}", replicationStatus.isActivated() + ", Path: " + CfPath.getPath());
//                                                    this.logger.info("Is Pending: {}", replicationStatus.isPending() + ", Path: " + CfPath.getPath());
//                                                    this.logger.info("Is Delivered: {}", replicationStatus.isDelivered() + ", Path: " + CfPath.getPath());
//                                                }
                                            // check if content fragment is same or not if  not same then create

                                            // boolean cfnodeExists = doesNodeExist(parentCfnode, tabName);

//                                                if (!cfnodeExists) {

                                            contentFragment = fragmentTemplate.createFragment(CfPath, regexTabName, "");

                                            ContentElement contentElement = contentFragment.getElement("date");
                                            contentElement.setContent(date, "");

                                            contentElement = contentFragment.getElement("linkredirection");
                                            contentElement.setContent(link, "");

                                            contentElement = contentFragment.getElement("tabname");
                                            contentElement.setContent(tabName, "");

                                            contentElement = contentFragment.getElement("tabtitle");
                                            contentElement.setContent(tabTitle, "");

                                            replicator.replicate(session, ReplicationActionType.ACTIVATE, CfPath.getPath());
                                            ReplicationStatus replicationStatus = replicator.getReplicationStatus(session, CfPath.getPath());

                                            this.logger.info("Is Activated: {}", replicationStatus.isActivated() + ", Path: " + CfPath.getPath());
                                            this.logger.info("Is Pending: {}", replicationStatus.isPending() + ", Path: " + CfPath.getPath());
                                            this.logger.info("Is Delivered: {}", replicationStatus.isDelivered() + ", Path: " + CfPath.getPath());

                                            session.save(); // Save the changes
                                            //}


                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }

        } catch (LoginException e) {
            throw new RuntimeException(e);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error("Exception: {}", e);
        }


    }

    public Boolean checkJcrNode(Node node) throws RepositoryException {
        if (node.getName().contains("jcr")) {
            return false;
        }
        return true;
    }

    public Boolean checkRepNode(Node node) throws RepositoryException {
        if (node.getName().contains("rep:policy")) {
            return false;
        }
        return true;
    }

    private boolean doesNodeExist(Node parentNode, String nodeName) throws RepositoryException {
        NodeIterator nodeIterator = parentNode.getNodes();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            if (node.getName().equals(nodeName)) {
                return true; // Node with the given name already exists
            }
        }
        return false; // Node with the given name does not exist
    }
}
