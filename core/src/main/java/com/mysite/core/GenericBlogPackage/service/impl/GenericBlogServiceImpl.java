package com.mysite.core.GenericBlogPackage.service.impl;


import com.mysite.core.GenericBlogPackage.service.GenericBlogService;
import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.*;

@Component(service = GenericBlogService.class,immediate = true)
public class GenericBlogServiceImpl implements GenericBlogService {
    Logger logger= LoggerFactory.getLogger(GenericBlogServiceImpl.class);

    @Reference
    ResourceHelper resourceHelper;



    @Override
    public List<Map<String, Object>> getBlogs(String path,String limit,List<String> tags) {
        Session session = null;
        ResourceResolver resourceResolver = null;
        List<Map<String,Object>> rootlst=new ArrayList<>();

        try{
            resourceResolver= resourceHelper.getResourceResolver();
            session = resourceResolver.adaptTo(Session.class);
            //limit="0";
           // List<String> tags = Arrays.asList("we-retail:gender/men", "we-retail:season/winter", "we-retail:activity/others");
            QueryManager queryManager= session.getWorkspace().getQueryManager();
            // path="/content/we-retail/us/en/products/women";

            StringBuilder query;
            if(tags.size()==0 || tags.isEmpty())
            {
                query= new StringBuilder("SELECT * FROM [cq:PageContent] AS s WHERE ISDESCENDANTNODE(["+path+"]) order by [cq:lastModified]");
            }
            else {
                 query = new StringBuilder("SELECT * FROM [cq:PageContent] AS s WHERE ISDESCENDANTNODE(["+path+"]) AND (");

                for (int i = 0; i < tags.size(); i++) {
                    if (i > 0) {
                        query.append(" OR ");
                    }
                    query.append("s.[cq:tags] LIKE '%").append(tags.get(i)).append("%'");
                }
                query.append(") order by [cq:lastModified] desc");
            }

            String finalQuery = query.toString();
            Query queryexec = queryManager.createQuery(finalQuery, Query.JCR_SQL2);
            logger.error("Query-Log", query);

            if (Long.parseLong(limit) > 0) {
                queryexec.setLimit(Long.parseLong(limit));
            }
            QueryResult queryResult;
            NodeIterator nodeIterator = null;

            queryResult = queryexec.execute();
            nodeIterator = queryResult.getNodes();
            if (nodeIterator.hasNext() == false) {
                return null;
            }
            else {
                while(nodeIterator.hasNext())
                {

                    Node categoryNode= nodeIterator.nextNode();
                    String title= categoryNode.hasProperty("jcr:title")?categoryNode.getProperty("jcr:title").getString():"";
                    String description=categoryNode.hasProperty("jcr:description")?categoryNode.getProperty("jcr:description").getString():"";
                    String pageTitle=categoryNode.hasProperty("pageTitle")?categoryNode.getProperty("pageTitle").getString():"";
                    String lastmodifieddate= categoryNode.hasProperty("cq:lastModified")?categoryNode.getProperty("cq:lastModified").getString():"";
                    Value[] values = new Value[0];
                    String[] cqtags=null;
                            if(categoryNode.hasProperty("cq:tags")){
                                values=  categoryNode.getProperty("cq:tags").getValues();
                            }
                    String image="";

                    if(categoryNode.hasNode("image")) {
                         image = categoryNode.getNode("image").hasProperty("fileReference") ? categoryNode.getNode("image").getProperty("fileReference").getString() : "";
                    }
                    Map<String,Object> datamap=new HashMap<>();
                    datamap.put("title",title);
                    datamap.put("navTitle",categoryNode.hasProperty("navTitle")?categoryNode.getProperty("navTitle").getString():"");
                    datamap.put("description",description);
                    datamap.put("pageTitle",pageTitle);
                    datamap.put("image",image);
                    datamap.put("lastModifiedDate",lastmodifieddate);
                    List<String> datalst=new ArrayList<>();

                    if(values.length!=0) {
                        for (int i = 0; i < values.length; i++) {
                            cqtags = new String[values.length];
                            cqtags[i] = values[i].getString();
                            datalst.add(cqtags[i]);
                            datamap.put("cq:tags", datalst);
                        }

                        rootlst.add(datamap);
                        logger.error("rootlst: {}", rootlst);
                    }

                }

            }



        }
        catch (Exception e)
        {
            logger.error("Exception in generic Blogs {}",e);
        }


        return rootlst;
    }
}
