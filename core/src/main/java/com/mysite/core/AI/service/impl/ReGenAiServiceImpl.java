package com.mysite.core.AI.service.impl;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mysite.core.AI.service.ReGenAIService;
import com.mysite.core.AI.service.RequestParser;
import com.mysite.core.AI.service.config.GenericConfigService;
import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

@Component(service = ReGenAIService.class,immediate = true)
public class ReGenAiServiceImpl implements ReGenAIService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    ResourceHelper resourceHelper;

    @Reference
    GenericConfigService genericConfigService;

    @Reference
    RequestParser requestParser;
    @Override
    public void getGenAi() throws IOException, RepositoryException {
        ResourceResolver resourceResolver = null;
        CloseableHttpClient client = null;
        Session session;
        try{
            resourceResolver = resourceHelper.getResourceResolver();
            session = resourceResolver.adaptTo(Session.class);
            String [] months=new String[ ]{"June"};
            String[] countrymaster=new String[0];
            String[] travellerfolder=new String[]{"single"};
            String[] cfarr=new String[]{"trivia","besttime","currency","food","culture","places"};
            String resp="";

            Resource cfmaster=resourceResolver.getResource("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/ai-country-master");
            ContentFragment cf = cfmaster.adaptTo(ContentFragment.class);
            Iterator<ContentElement> elements = cf.getElements();
            while (elements.hasNext()){
                ContentElement element = elements.next();

                if(element.getValue().getDataType().isMultiValue()==true){
                    countrymaster= (String[]) element.getValue().getValue();
                    logger.error("Contry master :" ,countrymaster);
                }
            }

            for(int i=0;i< countrymaster.length;i++)
            {
                countrymaster[i]= countrymaster[i].replace(" ","").trim();
                String cfpath="/content/dam/futuregeneraliindiainsurancecoltd/ai-model";
                Node countryNode=resourceResolver.getResource(cfpath).adaptTo(Node.class);
                boolean countrynodeExists = doesNodeExist(countryNode, countrymaster[i]);
                if (!countrynodeExists) {
                    Node countryFolder= countryNode.addNode(countrymaster[i],"sling:Folder");
                    countryFolder.setProperty("jcr:title",countrymaster[i]);
                    session.save();
                    logger.error("CountryNode {}",countryFolder.getName());
                    //createImage(countrymaster[i],months[0],session,resourceResolver);
                    cfCreation(countrymaster[i],resourceResolver,session);
                }
                else{
                    // createImage(countrymaster[i],months[0],session,resourceResolver);
                    for(int j=0;j<months.length;j++)
                    {
                        Node monthNode= resourceResolver.getResource(cfpath+"/"+countrymaster[i]).adaptTo(Node.class);
                        boolean monthnodeExists = doesNodeExist(monthNode, months[j]);
                        if (!monthnodeExists) {
                            Node monthFolder = monthNode.addNode(months[j], "sling:Folder");
                            monthFolder.setProperty("jcr:title", months[j]);
                            session.save();

                            for(int k=0;k< travellerfolder.length;k++)
                            {
                                Node travellertypeNode= resourceResolver.getResource(cfpath+"/"+countrymaster[i]+"/"+months[j]).adaptTo(Node.class);
                                boolean travellernodeExists= doesNodeExist(travellertypeNode, travellerfolder[k]);
                                if (!travellernodeExists) {
                                    Node travellernodeFolder = travellertypeNode.addNode(travellerfolder[k], "sling:Folder");
                                    travellernodeFolder.setProperty("jcr:title", travellerfolder[k]);
                                    session.save();

                                    for(int c=0;c<cfarr.length;c++)
                                    {
                                        Node cfNode=resourceResolver.getResource(cfpath+"/"+countrymaster[i]+"/"+months[j]+"/"+travellerfolder[k]).adaptTo(Node.class);
                                        boolean cfnodeExists= doesNodeExist(cfNode, cfarr[c]);
                                        if (!cfnodeExists) {
                                            String travellerpath="/content/dam/futuregeneraliindiainsurancecoltd/ai-model/travellertype/"+travellerfolder[k]+"/"+cfarr[c]+"";
                                            Node travellernode=resourceResolver.getResource(travellerpath).adaptTo(Node.class);

                                            JsonObject requestObject = new JsonObject();
                                            String besttime="";
                                            JsonArray messages = new JsonArray();
                                            if(cfarr[c].equals("besttime"))
                                            {
                                                besttime=cfarr[c];
                                                besttime="bestTime";
                                            }
                                            String gptprompt = travellernode.getNode("jcr:content").getNode("data").getNode("master").getProperty(cfarr[c].equals("besttime")?besttime:cfarr[c]).getString();
                                            // String travellertype= travellertypeNode.getParent().getName();
                                            //travellertype=travellertype.split("-")[0];
                                            if (gptprompt.contains("{country}")) {
                                                gptprompt = gptprompt.replace("{country}", countrymaster[i]);
                                                // gptprompt = gptprompt.replace("{month}", months[j]);
                                            }
                                            //prompt =   gptprompt + " " +"with 2 subheading and 5 paragraph in div element." ;
                                            String prompt = gptprompt;

                                            //prompt =   gptprompt  ;
                                            ChatGPTRequest chatGPTRequest = (ChatGPTRequest) requestParser.parse(prompt, "chat-completion", "user");

                                            requestObject.addProperty("model", chatGPTRequest.getModel());
                                            requestObject.addProperty("temperature", chatGPTRequest.getTemperature());
                                            requestObject.addProperty("format", chatGPTRequest.getFormatType());
                                            requestObject.addProperty("max_tokens", chatGPTRequest.getMax_tokens());
                                            JsonObject message = new JsonObject();
                                            message.addProperty("role", chatGPTRequest.getRole());
                                            message.addProperty("content", chatGPTRequest.getPrompt());
                                            messages.add(message);
                                            requestObject.add("messages", messages);
                                            client = HttpClients.createDefault();
                                            /*ChatGPT API Call*/
                                            HttpPost request = new HttpPost(genericConfigService.getConfig().get("set-chatGptApiEndpoint"));
                                            request.addHeader("api-key",genericConfigService.getConfig().get("set-ChatGpt-Key"));
                                            request.addHeader("Content-Type", "application/json");
                                            String requestBodys = requestObject.toString();
                                            logger.debug("Chatgpt Prompt : " + requestBodys);
                                            request.setEntity(new StringEntity(requestBodys));

                                            CloseableHttpResponse response = client.execute(request);
                                            int statusCode = response.getStatusLine().getStatusCode();
                                            if (statusCode == 200) {
                                                BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                                                String output;
                                                String responseJsonString = "";

                                                while ((output = br.readLine()) != null) {
                                                    responseJsonString = responseJsonString + output;
                                                }
                                                JsonArray result = new JsonArray();

                                                JsonObject convertedObject = new Gson().fromJson(responseJsonString, JsonObject.class);
                                                if (convertedObject.has("choices")) {
                                                    result = convertedObject.get("choices").getAsJsonArray();
                                                    String content = result.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content")
                                                            .getAsString();
                                                    logger.debug("Chatgpt Content Prompt Response: " + content);
                                                    /*Content Fragment Creation*/
                                                    Resource templateResc = resourceResolver.resolve("/conf/futuregeneraliindiainsurancecoltd/settings/dam/cfm/models/ai-country-details");
                                                    Resource cfParentResc = resourceResolver.resolve("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/" + countrymaster[i] + "/" + months[j] + "/" + travellerfolder[k] + "");
                                                    FragmentTemplate fragmentTemplate = templateResc.adaptTo(FragmentTemplate.class);
                                                    Node node = resourceResolver.getResource(cfParentResc.getPath()).adaptTo(Node.class);
                                                    if (node.hasNode(travellernode.getName())) {
                                                        Node cfNodes = node.getNode(travellernode.getName());
                                                        Node jcrNode = cfNodes.hasNode(JcrConstants.JCR_CONTENT) ? cfNodes.getNode(JcrConstants.JCR_CONTENT) : null;
                                                        Node datas = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                                        Node masters = datas.hasNode("master") ? datas.getNode("master") : null;
                                                        //String question=master.getProperty("question").getString();
                                                        masters.setProperty("question", gptprompt);
                                                        masters.setProperty("answer", content.replace("/n", ""));
                                                        masters.setProperty("country", countrymaster[i]);
                                                        masters.setProperty("month", months[j]);
                                                        String imagePrompt = "light-colored background image that captures the ambiance of " + countrymaster[i] + " in the month of " + months[j] + ".";
                                                        logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                                        //masters.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                                        resp += "\n Updated : " + countrymaster[i] + " " + months[j] + " " + travellernode.getName();
                                                        logger.error("Updated :{}",countrymaster[i] + " " + months[j] + " "+travellerfolder[k]+  " " + travellernode.getName());

                                                        session.save();
                                                        resourceResolver.commit();
                                                    } else {
                                                        ContentFragment contentFragment = fragmentTemplate.createFragment(cfParentResc, travellernode.getName(), String.valueOf(travellernode.getName()));
                                                        resourceResolver.commit();
                                                        Resource cfResource = contentFragment.adaptTo(Resource.class);
                                                        Node cfcreateNode = cfResource.adaptTo(Node.class);
                                                        Node jcrNode = cfcreateNode.hasNode(JcrConstants.JCR_CONTENT) ? cfcreateNode.getNode(JcrConstants.JCR_CONTENT) : null;
                                                        Node data = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                                        Node master = data.hasNode("master") ? data.getNode("master") : null;
                                                        //String question=master.getProperty("question").getString();
                                                        master.setProperty("question", gptprompt);
                                                        master.setProperty("answer", content.replace("/n", ""));
                                                        master.setProperty("country", countrymaster[i]);
                                                        master.setProperty("month", months[j]);
                                                        String imagePrompt = "light-colored background image that captures the ambiance of " + countrymaster[i] + " in the month of " + months[j] + ".";
                                                        logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                                        //master.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                                        resp += "\n Created:" + countrymaster[i] + " " + months[j] + " "+ travellernode.getName();
                                                        logger.error("Created :{}",countrymaster[i] + " " + months[j] + " "+travellerfolder[k]+  " " + travellernode.getName());

                                                        session.save();
                                                        resourceResolver.commit();
                                                    }

                                                }

                                            }

                                        }
                                    }
                                }
                                else {
                                    for(int c=0;c<cfarr.length;c++)
                                    {
                                        Node cfNode=resourceResolver.getResource(cfpath+"/"+countrymaster[i]+"/"+months[j]+"/"+travellerfolder[k]).adaptTo(Node.class);
                                        boolean cfnodeExists= doesNodeExist(cfNode, cfarr[c]);
                                        if (!cfnodeExists) {
                                            String travellerpath="/content/dam/futuregeneraliindiainsurancecoltd/ai-model/travellertype/"+travellerfolder[k]+"/"+cfarr[c]+"";
                                            Node travellernode=resourceResolver.getResource(travellerpath).adaptTo(Node.class);

                                            JsonObject requestObject = new JsonObject();
                                            String besttime="";
                                            JsonArray messages = new JsonArray();
                                            if(cfarr[c].equals("besttime"))
                                            {
                                                besttime=cfarr[c];
                                                besttime="bestTime";
                                            }
                                            String gptprompt = travellernode.getNode("jcr:content").getNode("data").getNode("master").getProperty(cfarr[c].equals("besttime")?besttime:cfarr[c]).getString();
                                            // String travellertype= travellertypeNode.getParent().getName();
                                            //travellertype=travellertype.split("-")[0];
                                            if (gptprompt.contains("{country}")) {
                                                gptprompt = gptprompt.replace("{country}", countrymaster[i]);
                                                // gptprompt = gptprompt.replace("{month}", months[j]);
                                            }
                                            //prompt =   gptprompt + " " +"with 2 subheading and 5 paragraph in div element." ;
                                            String prompt = gptprompt;

                                            //prompt =   gptprompt  ;
                                            ChatGPTRequest chatGPTRequest = (ChatGPTRequest) requestParser.parse(prompt, "chat-completion", "user");

                                            requestObject.addProperty("model", chatGPTRequest.getModel());
                                            requestObject.addProperty("temperature", chatGPTRequest.getTemperature());
                                            requestObject.addProperty("format", chatGPTRequest.getFormatType());
                                            requestObject.addProperty("max_tokens", chatGPTRequest.getMax_tokens());
                                            JsonObject message = new JsonObject();
                                            message.addProperty("role", chatGPTRequest.getRole());
                                            message.addProperty("content", chatGPTRequest.getPrompt());
                                            messages.add(message);
                                            requestObject.add("messages", messages);
                                            client = HttpClients.createDefault();
                                            /*ChatGPT API Call*/
                                            HttpPost request = new HttpPost(genericConfigService.getConfig().get("set-chatGptApiEndpoint"));
                                            request.addHeader("api-key",genericConfigService.getConfig().get("set-ChatGpt-Key"));
                                            request.addHeader("Content-Type", "application/json");
                                            String requestBodys = requestObject.toString();
                                            logger.debug("Chatgpt Prompt : " + requestBodys);
                                            request.setEntity(new StringEntity(requestBodys));

                                            CloseableHttpResponse response = client.execute(request);
                                            int statusCode = response.getStatusLine().getStatusCode();
                                            if (statusCode == 200) {
                                                BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                                                String output;
                                                String responseJsonString = "";

                                                while ((output = br.readLine()) != null) {
                                                    responseJsonString = responseJsonString + output;
                                                }
                                                JsonArray result = new JsonArray();

                                                JsonObject convertedObject = new Gson().fromJson(responseJsonString, JsonObject.class);
                                                if (convertedObject.has("choices")) {
                                                    result = convertedObject.get("choices").getAsJsonArray();
                                                    String content = result.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content")
                                                            .getAsString();
                                                    logger.debug("Chatgpt Content Prompt Response: " + content);
                                                    /*Content Fragment Creation*/
                                                    Resource templateResc = resourceResolver.resolve("/conf/futuregeneraliindiainsurancecoltd/settings/dam/cfm/models/ai-country-details");
                                                    Resource cfParentResc = resourceResolver.resolve("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/" + countrymaster[i] + "/" + months[j] + "/" + travellerfolder[k] + "");
                                                    FragmentTemplate fragmentTemplate = templateResc.adaptTo(FragmentTemplate.class);
                                                    Node node = resourceResolver.getResource(cfParentResc.getPath()).adaptTo(Node.class);
                                                    if (node.hasNode(travellernode.getName())) {
                                                        Node cfNodes = node.getNode(travellernode.getName());
                                                        Node jcrNode = cfNodes.hasNode(JcrConstants.JCR_CONTENT) ? cfNodes.getNode(JcrConstants.JCR_CONTENT) : null;
                                                        Node datas = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                                        Node masters = datas.hasNode("master") ? datas.getNode("master") : null;
                                                        //String question=master.getProperty("question").getString();
                                                        masters.setProperty("question", gptprompt);
                                                        masters.setProperty("answer", content.replace("/n", ""));
                                                        masters.setProperty("country", countrymaster[i]);
                                                        masters.setProperty("month", months[j]);
                                                        String imagePrompt = "light-colored background image that captures the ambiance of " + countrymaster[i] + " in the month of " + months[j] + ".";
                                                        logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                                        //masters.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                                        resp += "\n Updated : " + countrymaster[i] + " " + months[j] + " " + travellernode.getName();
                                                        logger.error("Updated :{}",countrymaster[i] + " " + months[j] + " "+travellerfolder[k]+  " " + travellernode.getName());

                                                        session.save();
                                                        resourceResolver.commit();
                                                    } else {
                                                        ContentFragment contentFragment = fragmentTemplate.createFragment(cfParentResc, travellernode.getName(), String.valueOf(travellernode.getName()));
                                                        resourceResolver.commit();
                                                        Resource cfResource = contentFragment.adaptTo(Resource.class);
                                                        Node cfcreateNode = cfResource.adaptTo(Node.class);
                                                        Node jcrNode = cfcreateNode.hasNode(JcrConstants.JCR_CONTENT) ? cfcreateNode.getNode(JcrConstants.JCR_CONTENT) : null;
                                                        Node data = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                                        Node master = data.hasNode("master") ? data.getNode("master") : null;
                                                        //String question=master.getProperty("question").getString();
                                                        master.setProperty("question", gptprompt);
                                                        master.setProperty("answer", content.replace("/n", ""));
                                                        master.setProperty("country", countrymaster[i]);
                                                        master.setProperty("month", months[j]);
                                                        String imagePrompt = "light-colored background image that captures the ambiance of " + countrymaster[i] + " in the month of " + months[j] + ".";
                                                        logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                                        //master.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                                        resp += "\n Created:" + countrymaster[i] + " " + months[j] + " "+ travellernode.getName();
                                                        logger.error("Created :{}",countrymaster[i] + " " + months[j] + " "+travellerfolder[k]+  " " + travellernode.getName());

                                                        session.save();
                                                        resourceResolver.commit();
                                                    }

                                                }

                                            }

                                        }
                                    }

                                }
                            }
                        }
                        else{
                            for(int k=0;k< travellerfolder.length;k++)
                            {
                                Node travellertypeNode= resourceResolver.getResource(cfpath+"/"+countrymaster[i]+"/"+months[j]).adaptTo(Node.class);
                                boolean travellernodeExists= doesNodeExist(travellertypeNode, travellerfolder[k]);
                                if (!travellernodeExists) {
                                    Node travellernodeFolder = travellertypeNode.addNode(travellerfolder[k], "sling:Folder");
                                    travellernodeFolder.setProperty("jcr:title", travellerfolder[k]);
                                    session.save();

                                    for(int c=0;c<cfarr.length;c++)
                                    {
                                        Node cfNode=resourceResolver.getResource(cfpath+"/"+countrymaster[i]+"/"+months[j]+"/"+travellerfolder[k]).adaptTo(Node.class);
                                        boolean cfnodeExists= doesNodeExist(cfNode, cfarr[c]);
                                        if (!cfnodeExists) {
                                            String travellerpath="/content/dam/futuregeneraliindiainsurancecoltd/ai-model/travellertype/"+travellerfolder[k]+"/"+cfarr[c]+"";
                                            Node travellernode=resourceResolver.getResource(travellerpath).adaptTo(Node.class);

                                            JsonObject requestObject = new JsonObject();
                                            String besttime="";
                                            JsonArray messages = new JsonArray();
                                            if(cfarr[c].equals("besttime"))
                                            {
                                                besttime=cfarr[c];
                                                besttime="bestTime";
                                            }
                                            String gptprompt = travellernode.getNode("jcr:content").getNode("data").getNode("master").getProperty(cfarr[c].equals("besttime")?besttime:cfarr[c]).getString();
                                            // String travellertype= travellertypeNode.getParent().getName();
                                            //travellertype=travellertype.split("-")[0];
                                            if (gptprompt.contains("{country}")) {
                                                gptprompt = gptprompt.replace("{country}", countrymaster[i]);
                                                //gptprompt = gptprompt.replace("{month}", months[j]);
                                            }
                                            //prompt =   gptprompt + " " +"with 2 subheading and 5 paragraph in div element." ;
                                            String prompt = gptprompt;

                                            //prompt =   gptprompt  ;
                                            ChatGPTRequest chatGPTRequest = (ChatGPTRequest) requestParser.parse(prompt, "chat-completion", "user");

                                            requestObject.addProperty("model", chatGPTRequest.getModel());
                                            requestObject.addProperty("temperature", chatGPTRequest.getTemperature());
                                            requestObject.addProperty("format", chatGPTRequest.getFormatType());
                                            requestObject.addProperty("max_tokens", chatGPTRequest.getMax_tokens());
                                            JsonObject message = new JsonObject();
                                            message.addProperty("role", chatGPTRequest.getRole());
                                            message.addProperty("content", chatGPTRequest.getPrompt());
                                            messages.add(message);
                                            requestObject.add("messages", messages);
                                            client = HttpClients.createDefault();
                                            /*ChatGPT API Call*/
                                            HttpPost request = new HttpPost(genericConfigService.getConfig().get("set-chatGptApiEndpoint"));
                                            request.addHeader("api-key",genericConfigService.getConfig().get("set-ChatGpt-Key"));
                                            request.addHeader("Content-Type", "application/json");
                                            String requestBodys = requestObject.toString();
                                            logger.debug("Chatgpt Prompt : " + requestBodys);
                                            request.setEntity(new StringEntity(requestBodys));

                                            CloseableHttpResponse response = client.execute(request);
                                            int statusCode = response.getStatusLine().getStatusCode();
                                            if (statusCode == 200) {
                                                BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                                                String output;
                                                String responseJsonString = "";

                                                while ((output = br.readLine()) != null) {
                                                    responseJsonString = responseJsonString + output;
                                                }
                                                JsonArray result = new JsonArray();

                                                JsonObject convertedObject = new Gson().fromJson(responseJsonString, JsonObject.class);
                                                if (convertedObject.has("choices")) {
                                                    result = convertedObject.get("choices").getAsJsonArray();
                                                    String content = result.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content")
                                                            .getAsString();
                                                    logger.debug("Chatgpt Content Prompt Response: " + content);
                                                    /*Content Fragment Creation*/
                                                    Resource templateResc = resourceResolver.resolve("/conf/futuregeneraliindiainsurancecoltd/settings/dam/cfm/models/ai-country-details");
                                                    Resource cfParentResc = resourceResolver.resolve("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/" + countrymaster[i] + "/" + months[j] + "/" + travellerfolder[k] + "");
                                                    FragmentTemplate fragmentTemplate = templateResc.adaptTo(FragmentTemplate.class);
                                                    Node node = resourceResolver.getResource(cfParentResc.getPath()).adaptTo(Node.class);
                                                    if (node.hasNode(travellernode.getName())) {
                                                        Node cfNodes = node.getNode(travellernode.getName());
                                                        Node jcrNode = cfNodes.hasNode(JcrConstants.JCR_CONTENT) ? cfNodes.getNode(JcrConstants.JCR_CONTENT) : null;
                                                        Node datas = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                                        Node masters = datas.hasNode("master") ? datas.getNode("master") : null;
                                                        //String question=master.getProperty("question").getString();
                                                        masters.setProperty("question", gptprompt);
                                                        masters.setProperty("answer", content.replace("/n", ""));
                                                        masters.setProperty("country", countrymaster[i]);
                                                        masters.setProperty("month", months[j]);
                                                        String imagePrompt = "light-colored background image that captures the ambiance of " + countrymaster[i] + " in the month of " + months[j] + ".";
                                                        logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                                        //masters.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                                        resp += "\n Updated : " + countrymaster[i] + " " + months[j] + " " + travellernode.getName();
                                                        logger.error("Updated :{}",countrymaster[i] + " " + months[j] + " "+travellerfolder[k]+  " " + travellernode.getName());
                                                        session.save();
                                                        resourceResolver.commit();
                                                    } else {
                                                        ContentFragment contentFragment = fragmentTemplate.createFragment(cfParentResc, travellernode.getName(), String.valueOf(travellernode.getName()));
                                                        resourceResolver.commit();
                                                        Resource cfResource = contentFragment.adaptTo(Resource.class);
                                                        Node cfcreateNode = cfResource.adaptTo(Node.class);
                                                        Node jcrNode = cfcreateNode.hasNode(JcrConstants.JCR_CONTENT) ? cfcreateNode.getNode(JcrConstants.JCR_CONTENT) : null;
                                                        Node data = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                                        Node master = data.hasNode("master") ? data.getNode("master") : null;
                                                        //String question=master.getProperty("question").getString();
                                                        master.setProperty("question", gptprompt);
                                                        master.setProperty("answer", content.replace("/n", ""));
                                                        master.setProperty("country", countrymaster[i]);
                                                        master.setProperty("month", months[j]);
                                                        String imagePrompt = "light-colored background image that captures the ambiance of " + countrymaster[i] + " in the month of " + months[j] + ".";
                                                        logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                                        //master.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                                        resp += "\n Created:" + countrymaster[i] + " " + months[j] + " "+ travellernode.getName();
                                                        logger.error("Created :{}",countrymaster[i] + " " + months[j] + " "+travellerfolder[k]+  " " + travellernode.getName());
                                                        session.save();
                                                        resourceResolver.commit();
                                                    }

                                                }

                                            }

                                        }
                                    }
                                }
                                else {
                                    for(int c=0;c<cfarr.length;c++)
                                    {
                                        Node cfNode=resourceResolver.getResource(cfpath+"/"+countrymaster[i]+"/"+months[j]+"/"+travellerfolder[k]).adaptTo(Node.class);
                                        boolean cfnodeExists= doesNodeExist(cfNode, cfarr[c]);
                                        if (!cfnodeExists) {
                                            String travellerpath="/content/dam/futuregeneraliindiainsurancecoltd/ai-model/travellertype/"+travellerfolder[k]+"/"+cfarr[c]+"";
                                            Node travellernode=resourceResolver.getResource(travellerpath).adaptTo(Node.class);

                                            JsonObject requestObject = new JsonObject();
                                            String besttime="";
                                            JsonArray messages = new JsonArray();
                                            if(cfarr[c].equals("besttime"))
                                            {
                                                besttime=cfarr[c];
                                                besttime="bestTime";
                                            }
                                            String gptprompt = travellernode.getNode("jcr:content").getNode("data").getNode("master").getProperty(cfarr[c].equals("besttime")?besttime:cfarr[c]).getString();
                                            // String travellertype= travellertypeNode.getParent().getName();
                                            //travellertype=travellertype.split("-")[0];
                                            if (gptprompt.contains("{country}")) {
                                                gptprompt = gptprompt.replace("{country}", countrymaster[i]);
                                                //gptprompt = gptprompt.replace("{month}", months[j]);
                                            }
                                            //prompt =   gptprompt + " " +"with 2 subheading and 5 paragraph in div element." ;
                                            String prompt = gptprompt;

                                            //prompt =   gptprompt  ;
                                            ChatGPTRequest chatGPTRequest = (ChatGPTRequest) requestParser.parse(prompt, "chat-completion", "user");

                                            requestObject.addProperty("model", chatGPTRequest.getModel());
                                            requestObject.addProperty("temperature", chatGPTRequest.getTemperature());
                                            requestObject.addProperty("format", chatGPTRequest.getFormatType());
                                            requestObject.addProperty("max_tokens", chatGPTRequest.getMax_tokens());
                                            JsonObject message = new JsonObject();
                                            message.addProperty("role", chatGPTRequest.getRole());
                                            message.addProperty("content", chatGPTRequest.getPrompt());
                                            messages.add(message);
                                            requestObject.add("messages", messages);
                                            client = HttpClients.createDefault();
                                            /*ChatGPT API Call*/
                                            HttpPost request = new HttpPost(genericConfigService.getConfig().get("set-chatGptApiEndpoint"));
                                            request.addHeader("api-key", genericConfigService.getConfig().get("set-ChatGpt-Key"));
                                            request.addHeader("Content-Type", "application/json");
                                            String requestBodys = requestObject.toString();
                                            logger.debug("Chatgpt Prompt : " + requestBodys);
                                            request.setEntity(new StringEntity(requestBodys));

                                            CloseableHttpResponse response = client.execute(request);
                                            int statusCode = response.getStatusLine().getStatusCode();
                                            if (statusCode == 200) {
                                                BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                                                String output;
                                                String responseJsonString = "";

                                                while ((output = br.readLine()) != null) {
                                                    responseJsonString = responseJsonString + output;
                                                }
                                                JsonArray result = new JsonArray();

                                                JsonObject convertedObject = new Gson().fromJson(responseJsonString, JsonObject.class);
                                                if (convertedObject.has("choices")) {
                                                    result = convertedObject.get("choices").getAsJsonArray();
                                                    String content = result.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content")
                                                            .getAsString();
                                                    logger.debug("Chatgpt Content Prompt Response: " + content);
                                                    /*Content Fragment Creation*/
                                                    Resource templateResc = resourceResolver.resolve("/conf/futuregeneraliindiainsurancecoltd/settings/dam/cfm/models/ai-country-details");
                                                    Resource cfParentResc = resourceResolver.resolve("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/" + countrymaster[i] + "/" + months[j] + "/" + travellerfolder[k] + "");
                                                    FragmentTemplate fragmentTemplate = templateResc.adaptTo(FragmentTemplate.class);
                                                    Node node = resourceResolver.getResource(cfParentResc.getPath()).adaptTo(Node.class);
                                                    if (node.hasNode(travellernode.getName())) {
                                                        Node cfNodes = node.getNode(travellernode.getName());
                                                        Node jcrNode = cfNodes.hasNode(JcrConstants.JCR_CONTENT) ? cfNodes.getNode(JcrConstants.JCR_CONTENT) : null;
                                                        Node datas = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                                        Node masters = datas.hasNode("master") ? datas.getNode("master") : null;
                                                        //String question=master.getProperty("question").getString();
                                                        masters.setProperty("question", gptprompt);
                                                        masters.setProperty("answer", content.replace("/n", ""));
                                                        masters.setProperty("country", countrymaster[i]);
                                                        masters.setProperty("month", months[j]);
                                                        String imagePrompt = "light-colored background image that captures the ambiance of " + countrymaster[i] + " in the month of " + months[j] + ".";
                                                        logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                                        //masters.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                                        resp += "\n Updated : " + countrymaster[i] + " " + months[j] + " " + travellernode.getName();
                                                        logger.error("Updated :{}",countrymaster[i] + " " + months[j] + " "+travellerfolder[k]+  " " + travellernode.getName());
                                                        session.save();
                                                        resourceResolver.commit();
                                                    } else {
                                                        ContentFragment contentFragment = fragmentTemplate.createFragment(cfParentResc, travellernode.getName(), String.valueOf(travellernode.getName()));
                                                        resourceResolver.commit();
                                                        Resource cfResource = contentFragment.adaptTo(Resource.class);
                                                        Node cfcreateNode = cfResource.adaptTo(Node.class);
                                                        Node jcrNode = cfcreateNode.hasNode(JcrConstants.JCR_CONTENT) ? cfcreateNode.getNode(JcrConstants.JCR_CONTENT) : null;
                                                        Node data = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                                        Node master = data.hasNode("master") ? data.getNode("master") : null;
                                                        //String question=master.getProperty("question").getString();
                                                        master.setProperty("question", gptprompt);
                                                        master.setProperty("answer", content.replace("/n", ""));
                                                        master.setProperty("country", countrymaster[i]);
                                                        master.setProperty("month", months[j]);
                                                        String imagePrompt = "light-colored background image that captures the ambiance of " + countrymaster[i] + " in the month of " + months[j] + ".";
                                                        logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                                        //master.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                                        resp += "\n Created:" + countrymaster[i] + " " + months[j] + " "+ travellernode.getName();
                                                        logger.error("Created :{}",countrymaster[i] + " " + months[j] + " "+travellerfolder[k]+  " " + travellernode.getName());
                                                        session.save();
                                                        resourceResolver.commit();
                                                    }

                                                }

                                            }

                                        }
                                    }

                                }
                            }
                        }
                    }
                }

            }
        }
        catch (Exception e)
        {
            logger.error("Exception {}",e);
        }

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

    public void cfCreation(String country,ResourceResolver resourceResolver,Session session) throws RepositoryException, IOException, ContentFragmentException {
        String resp="";
        String [] months=new String[ ]{"June"};
        //createImage(country,months[0],session,resourceResolver);
        for (int j=0;j<months.length;j++) {

            JsonObject requestObject = new JsonObject();

            JsonArray messages = new JsonArray();
            String travellerpath="/content/dam/futuregeneraliindiainsurancecoltd/ai-model/travellertype";
            Node travellernode=resourceResolver.getResource(travellerpath).adaptTo(Node.class);
            NodeIterator iterator = travellernode.getNodes();
//					if(iterator.hasNext())
//					{
//						travellertypenode= iterator.nextNode();
//						String gptprompt = travellertypenode.getNode("jcr:content").getNode("data").getNode("master").getProperty("trivia").getString()+" "+travellertypenode.getNode("jcr:content").getNode("data").getNode("master").getProperty("bestTime").getString()+" "+travellertypenode.getNode("jcr:content").getNode("data").getNode("master").getProperty("currency").getString()+" "+travellertypenode.getNode("jcr:content").getNode("data").getNode("master").getProperty("food").getString()+" "+travellertypenode.getNode("jcr:content").getNode("data").getNode("master").getProperty("culture").getString()+" "+travellertypenode.getNode("jcr:content").getNode("data").getNode("master").getProperty("places").getString();
//						if (gptprompt.contains("{country}") && gptprompt.contains("{month}")) {
//							gptprompt = gptprompt.replace("{country}", countrymaster[i]);
//							gptprompt = gptprompt.replace("{month}", months[j]);
//						}
//						prompt = gptprompt +" " +"with 2 subheading and 5 paragraph in div element.";
//						for(int c=0;c<imageSize.length;c++)
//						{
//							imageCreation(prompt,countrymaster[i],imageSize[c],session);
//						}
//					}

            while(iterator.hasNext())
            {
                travellernode= iterator.nextNode();
                if(travellernode.getName().equals("single"))
                {
                    NodeIterator travelleritr=travellernode.getNodes();
                    while(travelleritr.hasNext())
                    {
                        Node travellertypeNode= travelleritr.nextNode();
                        if(checkJcrNode(travellertypeNode) && checkpolicyNode(travellertypeNode))
                        {
                            CloseableHttpClient client;
                            if(travellertypeNode.getName().equals("trivia"))
                            {
                                /*Prompt Request Generation*/
//					String gptprompt="What is the Safety index of"+country+", weather during travel month of "+month+", Things to carry , Tourist attraction for kids adults and old age";
                                String gptprompt = travellertypeNode.getNode("jcr:content").getNode("data").getNode("master").getProperty("trivia").getString();
                                String travellertype= travellertypeNode.getParent().getName();
                                //travellertype=travellertype.split("-")[0];
                                if (gptprompt.contains("{country}")) {
                                    gptprompt = gptprompt.replace("{country}", country);
                                    //gptprompt = gptprompt.replace("{month}", months[j]);
                                }
                                //prompt =   gptprompt + " " +"with 2 subheading and 5 paragraph in div element." ;
                                String prompt = gptprompt;

                                //prompt =   gptprompt  ;
                                ChatGPTRequest chatGPTRequest = (ChatGPTRequest) requestParser.parse(prompt, "chat-completion", "user");

                                requestObject.addProperty("model", chatGPTRequest.getModel());
                                requestObject.addProperty("temperature", chatGPTRequest.getTemperature());
                                requestObject.addProperty("format", chatGPTRequest.getFormatType());
                                requestObject.addProperty("max_tokens", chatGPTRequest.getMax_tokens());
                                JsonObject message = new JsonObject();
                                message.addProperty("role", chatGPTRequest.getRole());
                                message.addProperty("content", chatGPTRequest.getPrompt());
                                messages.add(message);
                                requestObject.add("messages", messages);
                                client = HttpClients.createDefault();
                                /*ChatGPT API Call*/
                                HttpPost request = new HttpPost(genericConfigService.getConfig().get("set-chatGptApiEndpoint"));
                                request.addHeader("api-key",genericConfigService.getConfig().get("set-ChatGpt-Key"));
                                request.addHeader("Content-Type", "application/json");
                                String requestBodys = requestObject.toString();
                                logger.debug("Chatgpt Prompt : " + requestBodys);
                                request.setEntity(new StringEntity(requestBodys));

                                CloseableHttpResponse response = client.execute(request);
                                int statusCode = response.getStatusLine().getStatusCode();
                                if (statusCode == 200) {
                                    BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                                    String output;
                                    String responseJsonString = "";

                                    while ((output = br.readLine()) != null) {
                                        responseJsonString = responseJsonString + output;
                                    }
                                    JsonArray result = new JsonArray();

                                    JsonObject convertedObject = new Gson().fromJson(responseJsonString, JsonObject.class);
                                    if (convertedObject.has("choices")) {
                                        result = convertedObject.get("choices").getAsJsonArray();
                                        String content = result.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content")
                                                .getAsString();
                                        logger.debug("Chatgpt Content Prompt Response: " + content);
                                        String cfPath = "/content/dam/futuregeneraliindiainsurancecoltd/ai-model";
                                        Node countryNode = resourceResolver.getResource(cfPath).adaptTo(Node.class);

                                        boolean countrynodeExists = doesNodeExist(countryNode, country);
                                        Node countryFolder = null;
                                        Node monthFolder = null;
                                        if (!countrynodeExists) {
                                            countryFolder = countryNode.addNode(country, "sling:Folder");
                                            countryFolder.setProperty("jcr:title", country);
                                            session.save();
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                            }
                                            Node createmonth = countryNode.getNode(months[j]);
                                            boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                            if (!travellertypenodeExists) {
                                                Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                travllertypeFolder.setProperty("jcr:title", travellertype);
                                                session.save();
                                            }
                                        } else {
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                                logger.error("Month name {}",monthFolder.getName());
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                    logger.error("traveller folder name {}",travllertypeFolder.getName());
                                                }
                                            } else {
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                    logger.error("traveller folder name {}",travllertypeFolder.getName());
                                                }
                                            }
                                        }


                                        // Save the changes

                                        /*Content Fragment Creation*/
                                        Resource templateResc = resourceResolver.resolve("/conf/futuregeneraliindiainsurancecoltd/settings/dam/cfm/models/ai-country-details");
                                        Resource cfParentResc = resourceResolver.resolve("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/" + country + "/" + months[j] + "/" + travellertype + "");
                                        FragmentTemplate fragmentTemplate = templateResc.adaptTo(FragmentTemplate.class);
                                        Node node = resourceResolver.getResource(cfParentResc.getPath()).adaptTo(Node.class);
                                        if (node.hasNode(travellertypeNode.getName())) {
                                            Node cfNodes = node.getNode(travellertypeNode.getName());
                                            Node jcrNode = cfNodes.hasNode(JcrConstants.JCR_CONTENT) ? cfNodes.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node datas = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node masters = datas.hasNode("master") ? datas.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            masters.setProperty("question", gptprompt);
                                            masters.setProperty("answer", content.replace("/n", ""));
                                            masters.setProperty("country", country);
                                            masters.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //masters.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Updated : " + country + " " + months[j] + " " + travellertypeNode.getName();
                                            logger.error("Updated :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());
                                            session.save();
                                            resourceResolver.commit();
                                        } else {
                                            ContentFragment contentFragment = fragmentTemplate.createFragment(cfParentResc, travellertypeNode.getName(), String.valueOf(travellertypeNode.getName()));
                                            resourceResolver.commit();
                                            Resource cfResource = contentFragment.adaptTo(Resource.class);
                                            Node cfNode = cfResource.adaptTo(Node.class);
                                            Node jcrNode = cfNode.hasNode(JcrConstants.JCR_CONTENT) ? cfNode.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node data = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node master = data.hasNode("master") ? data.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            master.setProperty("question", gptprompt);
                                            master.setProperty("answer", content.replace("/n", ""));
                                            master.setProperty("country", country);
                                            master.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //master.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Created:" + country + " " + months[j] + " "+ travellertypeNode.getName();
                                            logger.error("Created :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());

                                            session.save();
                                            resourceResolver.commit();
                                        }

                                    }

                                }
                            }
                            else if (travellertypeNode.getName().equals("besttime")) {
                                /*Prompt Request Generation*/
//					String gptprompt="What is the Safety index of"+country+", weather during travel month of "+month+", Things to carry , Tourist attraction for kids adults and old age";
                                String gptprompt = travellertypeNode.getNode("jcr:content").getNode("data").getNode("master").getProperty("bestTime").getString();
                                String travellertype= travellertypeNode.getParent().getName();
                                //travellertype=travellertype.split("-")[0];
                                if (gptprompt.contains("{country}")) {
                                    gptprompt = gptprompt.replace("{country}", country);
                                    //gptprompt = gptprompt.replace("{month}", months[j]);
                                }
                                //prompt =   gptprompt + " " +"with 2 subheading and 5 paragraph in div element." ;
                                String prompt = gptprompt;

                                //prompt =   gptprompt  ;
                                ChatGPTRequest chatGPTRequest = (ChatGPTRequest) requestParser.parse(prompt, "chat-completion", "user");

                                requestObject.addProperty("model", chatGPTRequest.getModel());
                                requestObject.addProperty("temperature", chatGPTRequest.getTemperature());
                                requestObject.addProperty("format", chatGPTRequest.getFormatType());
                                requestObject.addProperty("max_tokens", chatGPTRequest.getMax_tokens());
                                JsonObject message = new JsonObject();
                                message.addProperty("role", chatGPTRequest.getRole());
                                message.addProperty("content", chatGPTRequest.getPrompt());
                                messages.add(message);
                                requestObject.add("messages", messages);
                                client = HttpClients.createDefault();
                                /*ChatGPT API Call*/
                                HttpPost request = new HttpPost(genericConfigService.getConfig().get("set-chatGptApiEndpoint"));
                                request.addHeader("api-key", genericConfigService.getConfig().get("set-ChatGpt-Key"));
                                request.addHeader("Content-Type", "application/json");
                                String requestBodys = requestObject.toString();
                                logger.debug("Chatgpt Prompt : " + requestBodys);
                                request.setEntity(new StringEntity(requestBodys));

                                CloseableHttpResponse response = client.execute(request);
                                int statusCode = response.getStatusLine().getStatusCode();
                                if (statusCode == 200) {
                                    BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                                    String output;
                                    String responseJsonString = "";

                                    while ((output = br.readLine()) != null) {
                                        responseJsonString = responseJsonString + output;
                                    }
                                    JsonArray result = new JsonArray();

                                    JsonObject convertedObject = new Gson().fromJson(responseJsonString, JsonObject.class);
                                    if (convertedObject.has("choices")) {
                                        result = convertedObject.get("choices").getAsJsonArray();
                                        String content = result.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content")
                                                .getAsString();
                                        logger.debug("Chatgpt Content Prompt Response: " + content);
                                        String cfPath = "/content/dam/futuregeneraliindiainsurancecoltd/ai-model";
                                        Node countryNode = resourceResolver.getResource(cfPath).adaptTo(Node.class);

                                        boolean countrynodeExists = doesNodeExist(countryNode, country);
                                        Node countryFolder = null;
                                        Node monthFolder = null;
                                        if (!countrynodeExists) {
                                            countryFolder = countryNode.addNode(country, "sling:Folder");
                                            countryFolder.setProperty("jcr:title", country);
                                            session.save();
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                            }
                                            Node createmonth = countryNode.getNode(months[j]);
                                            boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                            if (!travellertypenodeExists) {
                                                Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                travllertypeFolder.setProperty("jcr:title", travellertype);
                                                session.save();
                                            }
                                        } else {
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                }
                                            } else {
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                }
                                            }
                                        }


                                        // Save the changes

                                        /*Content Fragment Creation*/
                                        Resource templateResc = resourceResolver.resolve("/conf/futuregeneraliindiainsurancecoltd/settings/dam/cfm/models/ai-country-details");
                                        Resource cfParentResc = resourceResolver.resolve("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/" + country + "/" + months[j] + "/" + travellertype + "");
                                        FragmentTemplate fragmentTemplate = templateResc.adaptTo(FragmentTemplate.class);
                                        Node node = resourceResolver.getResource(cfParentResc.getPath()).adaptTo(Node.class);
                                        if (node.hasNode(travellertypeNode.getName())) {
                                            Node cfNodes = node.getNode(travellertypeNode.getName());
                                            Node jcrNode = cfNodes.hasNode(JcrConstants.JCR_CONTENT) ? cfNodes.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node datas = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node masters = datas.hasNode("master") ? datas.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            masters.setProperty("question", gptprompt);
                                            masters.setProperty("answer", content.replace("/n", ""));
                                            masters.setProperty("country", country);
                                            masters.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //masters.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Updated : " + country + " " + months[j] + " "+travellertypeNode.getName();
                                            logger.error("Updated :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());

                                            session.save();
                                            resourceResolver.commit();
                                        } else {
                                            ContentFragment contentFragment = fragmentTemplate.createFragment(cfParentResc, travellertypeNode.getName(), String.valueOf(travellertypeNode.getName()));
                                            resourceResolver.commit();
                                            Resource cfResource = contentFragment.adaptTo(Resource.class);
                                            Node cfNode = cfResource.adaptTo(Node.class);
                                            Node jcrNode = cfNode.hasNode(JcrConstants.JCR_CONTENT) ? cfNode.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node data = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node master = data.hasNode("master") ? data.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            master.setProperty("question", gptprompt);
                                            master.setProperty("answer", content.replace("/n", ""));
                                            master.setProperty("country", country);
                                            master.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //master.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Created:" + country + " " + months[j] + " "+travellertypeNode.getName();
                                            logger.error("Created :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());

                                            session.save();
                                            resourceResolver.commit();
                                        }

                                    }

                                }

                            }
                            else if (travellertypeNode.getName().equals("currency")) {
                                /*Prompt Request Generation*/
//					String gptprompt="What is the Safety index of"+country+", weather during travel month of "+month+", Things to carry , Tourist attraction for kids adults and old age";
                                String gptprompt = travellertypeNode.getNode("jcr:content").getNode("data").getNode("master").getProperty("currency").getString();
                                String travellertype= travellertypeNode.getParent().getName();
                                //travellertype=travellertype.split("-")[0];
                                if (gptprompt.contains("{country}")) {
                                    gptprompt = gptprompt.replace("{country}", country);
                                    //gptprompt = gptprompt.replace("{month}", months[j]);
                                }
                                //prompt =   gptprompt + " " +"with 2 subheading and 5 paragraph in div element." ;
                                String prompt = gptprompt;

                                //prompt =   gptprompt  ;
                                ChatGPTRequest chatGPTRequest = (ChatGPTRequest) requestParser.parse(prompt, "chat-completion", "user");

                                requestObject.addProperty("model", chatGPTRequest.getModel());
                                requestObject.addProperty("temperature", chatGPTRequest.getTemperature());
                                requestObject.addProperty("format", chatGPTRequest.getFormatType());
                                requestObject.addProperty("max_tokens", chatGPTRequest.getMax_tokens());
                                JsonObject message = new JsonObject();
                                message.addProperty("role", chatGPTRequest.getRole());
                                message.addProperty("content", chatGPTRequest.getPrompt());
                                messages.add(message);
                                requestObject.add("messages", messages);
                                client = HttpClients.createDefault();
                                /*ChatGPT API Call*/
                                HttpPost request = new HttpPost(genericConfigService.getConfig().get("set-chatGptApiEndpoint"));
                                request.addHeader("api-key", genericConfigService.getConfig().get("set-ChatGpt-Key"));
                                request.addHeader("Content-Type", "application/json");
                                String requestBodys = requestObject.toString();
                                logger.debug("Chatgpt Prompt : " + requestBodys);
                                request.setEntity(new StringEntity(requestBodys));

                                CloseableHttpResponse response = client.execute(request);
                                int statusCode = response.getStatusLine().getStatusCode();
                                if (statusCode == 200) {
                                    BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                                    String output;
                                    String responseJsonString = "";

                                    while ((output = br.readLine()) != null) {
                                        responseJsonString = responseJsonString + output;
                                    }
                                    JsonArray result = new JsonArray();

                                    JsonObject convertedObject = new Gson().fromJson(responseJsonString, JsonObject.class);
                                    if (convertedObject.has("choices")) {
                                        result = convertedObject.get("choices").getAsJsonArray();
                                        String content = result.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content")
                                                .getAsString();
                                        logger.debug("Chatgpt Content Prompt Response: " + content);
                                        String cfPath = "/content/dam/futuregeneraliindiainsurancecoltd/ai-model";
                                        Node countryNode = resourceResolver.getResource(cfPath).adaptTo(Node.class);

                                        boolean countrynodeExists = doesNodeExist(countryNode, country);
                                        Node countryFolder = null;
                                        Node monthFolder = null;
                                        if (!countrynodeExists) {
                                            countryFolder = countryNode.addNode(country, "sling:Folder");
                                            countryFolder.setProperty("jcr:title", country);
                                            session.save();
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                            }
                                            Node createmonth = countryNode.getNode(months[j]);
                                            boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                            if (!travellertypenodeExists) {
                                                Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                travllertypeFolder.setProperty("jcr:title", travellertype);
                                                session.save();
                                            }
                                        } else {
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                }
                                            } else {
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                }
                                            }
                                        }


                                        // Save the changes

                                        /*Content Fragment Creation*/
                                        Resource templateResc = resourceResolver.resolve("/conf/futuregeneraliindiainsurancecoltd/settings/dam/cfm/models/ai-country-details");
                                        Resource cfParentResc = resourceResolver.resolve("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/" + country + "/" + months[j] + "/" + travellertype + "");
                                        FragmentTemplate fragmentTemplate = templateResc.adaptTo(FragmentTemplate.class);
                                        Node node = resourceResolver.getResource(cfParentResc.getPath()).adaptTo(Node.class);
                                        if (node.hasNode(travellertypeNode.getName())) {
                                            Node cfNodes = node.getNode(travellertypeNode.getName());
                                            Node jcrNode = cfNodes.hasNode(JcrConstants.JCR_CONTENT) ? cfNodes.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node datas = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node masters = datas.hasNode("master") ? datas.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            masters.setProperty("question", gptprompt);
                                            masters.setProperty("answer", content.replace("/n", ""));
                                            masters.setProperty("country", country);
                                            masters.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //masters.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Updated : " + country + " " + months[j] + " "+travellertypeNode.getName();
                                            logger.error("Updated :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());

                                            session.save();
                                            resourceResolver.commit();
                                        } else {
                                            ContentFragment contentFragment = fragmentTemplate.createFragment(cfParentResc, travellertypeNode.getName(), String.valueOf(travellertypeNode.getName()));
                                            resourceResolver.commit();
                                            Resource cfResource = contentFragment.adaptTo(Resource.class);
                                            Node cfNode = cfResource.adaptTo(Node.class);
                                            Node jcrNode = cfNode.hasNode(JcrConstants.JCR_CONTENT) ? cfNode.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node data = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node master = data.hasNode("master") ? data.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            master.setProperty("question", gptprompt);
                                            master.setProperty("answer", content.replace("/n", ""));
                                            master.setProperty("country", country);
                                            master.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //master.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Created:" + country + " " + months[j] + " "+travellertypeNode.getName();
                                            logger.error("Created :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());

                                            session.save();
                                            resourceResolver.commit();
                                        }

                                    }

                                }

                            }
                            else if (travellertypeNode.getName().equals("food")) {
                                /*Prompt Request Generation*/
//					String gptprompt="What is the Safety index of"+country+", weather during travel month of "+month+", Things to carry , Tourist attraction for kids adults and old age";
                                String gptprompt = travellertypeNode.getNode("jcr:content").getNode("data").getNode("master").getProperty("food").getString();
                                String travellertype= travellertypeNode.getParent().getName();
                                //travellertype=travellertype.split("-")[0];
                                if (gptprompt.contains("{country}")) {
                                    gptprompt = gptprompt.replace("{country}", country);
                                    //gptprompt = gptprompt.replace("{month}", months[j]);
                                }
                                //prompt =   gptprompt + " " +"with 2 subheading and 5 paragraph in div element." ;
                                String prompt = gptprompt;

                                //prompt =   gptprompt  ;
                                ChatGPTRequest chatGPTRequest = (ChatGPTRequest) requestParser.parse(prompt, "chat-completion", "user");

                                requestObject.addProperty("model", chatGPTRequest.getModel());
                                requestObject.addProperty("temperature", chatGPTRequest.getTemperature());
                                requestObject.addProperty("format", chatGPTRequest.getFormatType());
                                requestObject.addProperty("max_tokens", chatGPTRequest.getMax_tokens());
                                JsonObject message = new JsonObject();
                                message.addProperty("role", chatGPTRequest.getRole());
                                message.addProperty("content", chatGPTRequest.getPrompt());
                                messages.add(message);
                                requestObject.add("messages", messages);
                                client = HttpClients.createDefault();
                                /*ChatGPT API Call*/
                                HttpPost request = new HttpPost(genericConfigService.getConfig().get("set-chatGptApiEndpoint"));
                                request.addHeader("api-key", genericConfigService.getConfig().get("set-ChatGpt-Key"));
                                request.addHeader("Content-Type", "application/json");
                                String requestBodys = requestObject.toString();
                                logger.debug("Chatgpt Prompt : " + requestBodys);
                                request.setEntity(new StringEntity(requestBodys));

                                CloseableHttpResponse response = client.execute(request);
                                int statusCode = response.getStatusLine().getStatusCode();
                                if (statusCode == 200) {
                                    BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                                    String output;
                                    String responseJsonString = "";

                                    while ((output = br.readLine()) != null) {
                                        responseJsonString = responseJsonString + output;
                                    }
                                    JsonArray result = new JsonArray();

                                    JsonObject convertedObject = new Gson().fromJson(responseJsonString, JsonObject.class);
                                    if (convertedObject.has("choices")) {
                                        result = convertedObject.get("choices").getAsJsonArray();
                                        String content = result.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content")
                                                .getAsString();
                                        logger.debug("Chatgpt Content Prompt Response: " + content);
                                        String cfPath = "/content/dam/futuregeneraliindiainsurancecoltd/ai-model";
                                        Node countryNode = resourceResolver.getResource(cfPath).adaptTo(Node.class);

                                        boolean countrynodeExists = doesNodeExist(countryNode, country);
                                        Node countryFolder = null;
                                        Node monthFolder = null;
                                        if (!countrynodeExists) {
                                            countryFolder = countryNode.addNode(country, "sling:Folder");
                                            countryFolder.setProperty("jcr:title", country);
                                            session.save();
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                            }
                                            Node createmonth = countryNode.getNode(months[j]);
                                            boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                            if (!travellertypenodeExists) {
                                                Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                travllertypeFolder.setProperty("jcr:title", travellertype);
                                                session.save();
                                            }
                                        } else {
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                }
                                            } else {
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                }
                                            }
                                        }


                                        // Save the changes

                                        /*Content Fragment Creation*/
                                        Resource templateResc = resourceResolver.resolve("/conf/futuregeneraliindiainsurancecoltd/settings/dam/cfm/models/ai-country-details");
                                        Resource cfParentResc = resourceResolver.resolve("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/" + country + "/" + months[j] + "/" + travellertype + "");
                                        FragmentTemplate fragmentTemplate = templateResc.adaptTo(FragmentTemplate.class);
                                        Node node = resourceResolver.getResource(cfParentResc.getPath()).adaptTo(Node.class);
                                        if (node.hasNode(travellertypeNode.getName())) {
                                            Node cfNodes = node.getNode(travellertypeNode.getName());
                                            Node jcrNode = cfNodes.hasNode(JcrConstants.JCR_CONTENT) ? cfNodes.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node datas = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node masters = datas.hasNode("master") ? datas.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            masters.setProperty("question", gptprompt);
                                            masters.setProperty("answer", content.replace("/n", ""));
                                            masters.setProperty("country", country);
                                            masters.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //masters.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Updated : " + country + " " + months[j] + " "+travellertypeNode.getName();
                                            logger.error("Updated :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());
                                            session.save();
                                            resourceResolver.commit();
                                        } else {
                                            ContentFragment contentFragment = fragmentTemplate.createFragment(cfParentResc, travellertypeNode.getName(), String.valueOf(travellertypeNode.getName()));
                                            resourceResolver.commit();
                                            Resource cfResource = contentFragment.adaptTo(Resource.class);
                                            Node cfNode = cfResource.adaptTo(Node.class);
                                            Node jcrNode = cfNode.hasNode(JcrConstants.JCR_CONTENT) ? cfNode.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node data = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node master = data.hasNode("master") ? data.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            master.setProperty("question", gptprompt);
                                            master.setProperty("answer", content.replace("/n", ""));
                                            master.setProperty("country", country);
                                            master.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //master.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Created:" + country + " " + months[j] + " "+travellertypeNode.getName();
                                            logger.error("Created :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());
                                            session.save();
                                            resourceResolver.commit();
                                        }

                                    }

                                }

                            }
                            else if (travellertypeNode.getName().equals("places")) {
                                /*Prompt Request Generation*/
//					String gptprompt="What is the Safety index of"+country+", weather during travel month of "+month+", Things to carry , Tourist attraction for kids adults and old age";
                                String gptprompt = travellertypeNode.getNode("jcr:content").getNode("data").getNode("master").getProperty("places").getString();
                                String travellertype= travellertypeNode.getParent().getName();
                                //travellertype=travellertype.split("-")[0];
                                if (gptprompt.contains("{country}")) {
                                    gptprompt = gptprompt.replace("{country}", country);
                                    //gptprompt = gptprompt.replace("{month}", months[j]);
                                }
                                //prompt =   gptprompt + " " +"with 2 subheading and 5 paragraph in div element." ;
                                String prompt = gptprompt;

                                //prompt =   gptprompt  ;
                                ChatGPTRequest chatGPTRequest = (ChatGPTRequest) requestParser.parse(prompt, "chat-completion", "user");

                                requestObject.addProperty("model", chatGPTRequest.getModel());
                                requestObject.addProperty("temperature", chatGPTRequest.getTemperature());
                                requestObject.addProperty("format", chatGPTRequest.getFormatType());
                                requestObject.addProperty("max_tokens", chatGPTRequest.getMax_tokens());
                                JsonObject message = new JsonObject();
                                message.addProperty("role", chatGPTRequest.getRole());
                                message.addProperty("content", chatGPTRequest.getPrompt());
                                messages.add(message);
                                requestObject.add("messages", messages);
                                client = HttpClients.createDefault();
                                /*ChatGPT API Call*/
                                HttpPost request = new HttpPost(genericConfigService.getConfig().get("set-chatGptApiEndpoint"));
                                request.addHeader("api-key", genericConfigService.getConfig().get("set-ChatGpt-Key"));
                                request.addHeader("Content-Type", "application/json");
                                String requestBodys = requestObject.toString();
                                logger.debug("Chatgpt Prompt : " + requestBodys);
                                request.setEntity(new StringEntity(requestBodys));

                                CloseableHttpResponse response = client.execute(request);
                                int statusCode = response.getStatusLine().getStatusCode();
                                if (statusCode == 200) {
                                    BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                                    String output;
                                    String responseJsonString = "";

                                    while ((output = br.readLine()) != null) {
                                        responseJsonString = responseJsonString + output;
                                    }
                                    JsonArray result = new JsonArray();

                                    JsonObject convertedObject = new Gson().fromJson(responseJsonString, JsonObject.class);
                                    if (convertedObject.has("choices")) {
                                        result = convertedObject.get("choices").getAsJsonArray();
                                        String content = result.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content")
                                                .getAsString();
                                        logger.debug("Chatgpt Content Prompt Response: " + content);
                                        String cfPath = "/content/dam/futuregeneraliindiainsurancecoltd/ai-model";
                                        Node countryNode = resourceResolver.getResource(cfPath).adaptTo(Node.class);

                                        boolean countrynodeExists = doesNodeExist(countryNode, country);
                                        Node countryFolder = null;
                                        Node monthFolder = null;
                                        if (!countrynodeExists) {
                                            countryFolder = countryNode.addNode(country, "sling:Folder");
                                            countryFolder.setProperty("jcr:title", country);
                                            session.save();
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                            }
                                            Node createmonth = countryNode.getNode(months[j]);
                                            boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                            if (!travellertypenodeExists) {
                                                Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                travllertypeFolder.setProperty("jcr:title", travellertype);
                                                session.save();
                                            }
                                        } else {
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                }
                                            } else {
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                }
                                            }
                                        }


                                        // Save the changes

                                        /*Content Fragment Creation*/
                                        Resource templateResc = resourceResolver.resolve("/conf/futuregeneraliindiainsurancecoltd/settings/dam/cfm/models/ai-country-details");
                                        Resource cfParentResc = resourceResolver.resolve("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/" + country + "/" + months[j] + "/" + travellertype + "");
                                        FragmentTemplate fragmentTemplate = templateResc.adaptTo(FragmentTemplate.class);
                                        Node node = resourceResolver.getResource(cfParentResc.getPath()).adaptTo(Node.class);
                                        if (node.hasNode(travellertypeNode.getName())) {
                                            Node cfNodes = node.getNode(travellertypeNode.getName());
                                            Node jcrNode = cfNodes.hasNode(JcrConstants.JCR_CONTENT) ? cfNodes.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node datas = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node masters = datas.hasNode("master") ? datas.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            masters.setProperty("question", gptprompt);
                                            masters.setProperty("answer", content.replace("/n", ""));
                                            masters.setProperty("country", country);
                                            masters.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //masters.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Updated : " + country + " " + months[j] + " "+travellertypeNode.getName();
                                            logger.error("Updated :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());
                                            session.save();
                                            resourceResolver.commit();
                                        } else {
                                            ContentFragment contentFragment = fragmentTemplate.createFragment(cfParentResc, travellertypeNode.getName(), String.valueOf(travellertypeNode.getName()));
                                            resourceResolver.commit();
                                            Resource cfResource = contentFragment.adaptTo(Resource.class);
                                            Node cfNode = cfResource.adaptTo(Node.class);
                                            Node jcrNode = cfNode.hasNode(JcrConstants.JCR_CONTENT) ? cfNode.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node data = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node master = data.hasNode("master") ? data.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            master.setProperty("question", gptprompt);
                                            master.setProperty("answer", content.replace("/n", ""));
                                            master.setProperty("country", country);
                                            master.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //master.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Created:" + country + " " + months[j] + " "+travellertypeNode.getName();
                                            logger.error("Created :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());
                                            session.save();
                                            resourceResolver.commit();
                                        }

                                    }

                                }

                            }
                            else if (travellertypeNode.getName().equals("culture")) {
                                /*Prompt Request Generation*/
//					String gptprompt="What is the Safety index of"+country+", weather during travel month of "+month+", Things to carry , Tourist attraction for kids adults and old age";
                                String gptprompt = travellertypeNode.getNode("jcr:content").getNode("data").getNode("master").getProperty("culture").getString();
                                String travellertype= travellertypeNode.getParent().getName();
                                //travellertype=travellertype.split("-")[0];
                                if (gptprompt.contains("{country}")) {
                                    gptprompt = gptprompt.replace("{country}", country);
                                    // gptprompt = gptprompt.replace("{month}", months[j]);
                                }
                                //prompt =   gptprompt + " " +"with 2 subheading and 5 paragraph in div element." ;
                                String prompt = gptprompt;

                                //prompt =   gptprompt  ;
                                ChatGPTRequest chatGPTRequest = (ChatGPTRequest) requestParser.parse(prompt, "chat-completion", "user");

                                requestObject.addProperty("model", chatGPTRequest.getModel());
                                requestObject.addProperty("temperature", chatGPTRequest.getTemperature());
                                requestObject.addProperty("format", chatGPTRequest.getFormatType());
                                requestObject.addProperty("max_tokens", chatGPTRequest.getMax_tokens());
                                JsonObject message = new JsonObject();
                                message.addProperty("role", chatGPTRequest.getRole());
                                message.addProperty("content", chatGPTRequest.getPrompt());
                                messages.add(message);
                                requestObject.add("messages", messages);
                                client = HttpClients.createDefault();
                                /*ChatGPT API Call*/
                                HttpPost request = new HttpPost(genericConfigService.getConfig().get("set-chatGptApiEndpoint"));
                                request.addHeader("api-key", genericConfigService.getConfig().get("set-ChatGpt-Key"));
                                request.addHeader("Content-Type", "application/json");
                                String requestBodys = requestObject.toString();
                                logger.debug("Chatgpt Prompt : " + requestBodys);
                                request.setEntity(new StringEntity(requestBodys));

                                CloseableHttpResponse response = client.execute(request);
                                int statusCode = response.getStatusLine().getStatusCode();
                                if (statusCode == 200) {
                                    BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                                    String output;
                                    String responseJsonString = "";

                                    while ((output = br.readLine()) != null) {
                                        responseJsonString = responseJsonString + output;
                                    }
                                    JsonArray result = new JsonArray();

                                    JsonObject convertedObject = new Gson().fromJson(responseJsonString, JsonObject.class);
                                    if (convertedObject.has("choices")) {
                                        result = convertedObject.get("choices").getAsJsonArray();
                                        String content = result.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content")
                                                .getAsString();
                                        logger.debug("Chatgpt Content Prompt Response: " + content);
                                        String cfPath = "/content/dam/futuregeneraliindiainsurancecoltd/ai-model";
                                        Node countryNode = resourceResolver.getResource(cfPath).adaptTo(Node.class);

                                        boolean countrynodeExists = doesNodeExist(countryNode, country);
                                        Node countryFolder = null;
                                        Node monthFolder = null;
                                        if (!countrynodeExists) {
                                            countryFolder = countryNode.addNode(country, "sling:Folder");
                                            countryFolder.setProperty("jcr:title", country);
                                            session.save();
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                            }
                                            Node createmonth = countryNode.getNode(months[j]);
                                            boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                            if (!travellertypenodeExists) {
                                                Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                travllertypeFolder.setProperty("jcr:title", travellertype);
                                                session.save();
                                            }
                                        } else {
                                            countryNode = countryNode.getNode(country);
                                            boolean monthnodeExists = doesNodeExist(countryNode, months[j]);
                                            if (!monthnodeExists) {
                                                monthFolder = countryNode.addNode(months[j], "sling:Folder");
                                                monthFolder.setProperty("jcr:title", months[j]);
                                                session.save();
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                }
                                            } else {
                                                Node createmonth = countryNode.getNode(months[j]);
                                                boolean travellertypenodeExists = doesNodeExist(createmonth, travellertype);
                                                if (!travellertypenodeExists) {
                                                    Node travllertypeFolder = createmonth.addNode(travellertype, "sling:Folder");
                                                    travllertypeFolder.setProperty("jcr:title", travellertype);
                                                    session.save();
                                                }
                                            }
                                        }


                                        // Save the changes

                                        /*Content Fragment Creation*/
                                        Resource templateResc = resourceResolver.resolve("/conf/futuregeneraliindiainsurancecoltd/settings/dam/cfm/models/ai-country-details");
                                        Resource cfParentResc = resourceResolver.resolve("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/" + country + "/" + months[j] + "/" + travellertype + "");
                                        FragmentTemplate fragmentTemplate = templateResc.adaptTo(FragmentTemplate.class);
                                        Node node = resourceResolver.getResource(cfParentResc.getPath()).adaptTo(Node.class);
                                        if (node.hasNode(travellertypeNode.getName())) {
                                            Node cfNodes = node.getNode(travellertypeNode.getName());
                                            Node jcrNode = cfNodes.hasNode(JcrConstants.JCR_CONTENT) ? cfNodes.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node datas = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node masters = datas.hasNode("master") ? datas.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            masters.setProperty("question", gptprompt);
                                            masters.setProperty("answer", content.replace("/n", ""));
                                            masters.setProperty("country", country);
                                            masters.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //masters.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Updated : " + country + " " + months[j] + " "+travellertypeNode.getName();
                                            logger.error("Updated :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());
                                            session.save();
                                            resourceResolver.commit();
                                        } else {
                                            ContentFragment contentFragment = fragmentTemplate.createFragment(cfParentResc, travellertypeNode.getName(), String.valueOf(travellertypeNode.getName()));
                                            resourceResolver.commit();
                                            Resource cfResource = contentFragment.adaptTo(Resource.class);
                                            Node cfNode = cfResource.adaptTo(Node.class);
                                            Node jcrNode = cfNode.hasNode(JcrConstants.JCR_CONTENT) ? cfNode.getNode(JcrConstants.JCR_CONTENT) : null;
                                            Node data = jcrNode.hasNode("data") ? jcrNode.getNode("data") : null;
                                            Node master = data.hasNode("master") ? data.getNode("master") : null;
                                            //String question=master.getProperty("question").getString();
                                            master.setProperty("question", gptprompt);
                                            master.setProperty("answer", content.replace("/n", ""));
                                            master.setProperty("country", country);
                                            master.setProperty("month", months[j]);
                                            String imagePrompt = "light-colored background image that captures the ambiance of " + country + " in the month of " + months[j] + ".";
                                            logger.debug("Chatgpt Image Prompt : " + imagePrompt);
                                            //master.setProperty("aiimage", imageCreation(imagePrompt, countrymaster[i], months[j], travellertype, session));
                                            resp += "\n Created:" + country + " " + months[j] + " "+travellertypeNode.getName();
                                            logger.error("Created :{}",country + " " + months[j] + " "+travellertype+  " " + travellertypeNode.getName());
                                            session.save();
                                            resourceResolver.commit();
                                        }

                                    }

                                }

                            }
                        }
                    }
                }

            }

        }
    }
    public Boolean checkJcrNode (Node node) throws RepositoryException {
        if (node.getName().contains("jcr")) {
            return false;
        }
        return true;
    }

    public Boolean checkpolicyNode (Node node) throws RepositoryException {
        if (node.getName().contains("rep:policy")) {
            return false;
        }
        return true;
    }

    public void createImage(String country,String month,Session session,ResourceResolver resourceResolver) throws RepositoryException {
        String travellerpath="/content/dam/futuregeneraliindiainsurancecoltd/ai-model/travellertype/single";
        String[] imageSize=new String[]{"1792x1024"};
        try {
            Node travellernode = resourceResolver.getResource(travellerpath).adaptTo(Node.class);
            NodeIterator iterator = travellernode.getNodes();
            if (iterator.hasNext()) {


                Node travellertypeNode = iterator.nextNode();
                if(travellertypeNode.getName().equals("trivia") ||travellertypeNode.getName().equals("places") ||travellertypeNode.getName().equals("food")||travellertypeNode.getName().equals("culture")||travellertypeNode.getName().equals("currency")|| travellertypeNode.getName().equals("besttime")) {
                    /*Prompt Request Generation*/
                    String propertyName= travellertypeNode.getName();
                    if(propertyName.equals("besttime"))
                    {
                        propertyName="bestTime";
                    }
//					String gptprompt="What is the Safety index of"+country+", weather during travel month of "+month+", Things to carry , Tourist attraction for kids adults and old age";
                    //String gptprompt = travellertypeNode.getNode("jcr:content").getNode("data").getNode("master").getProperty(propertyName).getString();
                    String gptprompt = "Banner, Ultra-Wide Angle, 5D, Full-HD, Rectangular, Happy, Image of "+country+" ";
                    //String travellertype = travellertypeNode.getParent().getName();
                    //travellertype=travellertype.split("-")[0];
                    if (gptprompt.contains("{country}") && gptprompt.contains("{month}")) {
                        gptprompt = gptprompt.replace("{country}", country);
                        gptprompt = gptprompt.replace("{month}", month);
                    }
                    //prompt =   gptprompt + " " +"with 2 subheading and 5 paragraph in div element." ;
                    //String gptprompt="What is the Safety index of "+country+", weather during travel month of "+month+", Things to carry , Tourist attraction for kids adults and old age";
                    String prompt = gptprompt;
                    for (int c = 0; c < imageSize.length; c++) {
                        imageCreation(prompt, country, imageSize[c], session);
                    }
                }





            }
        }
        catch (Exception e)
        {
            logger.error("Exception {}",e);
        }

    }

    private String imageCreation( String prompt,String country,String imgsize,Session session) throws IOException {
        CloseableHttpClient client = null;
        try {
            String imageurl="";
            client = HttpClients.createDefault();
            JsonObject requestObject = new JsonObject();

            requestObject.addProperty("prompt",prompt);
            requestObject.addProperty("n",1);
            requestObject.addProperty("size",imgsize);
            requestObject.addProperty("model","dall-e-3");
            requestObject.addProperty("quality","hd");
            HttpPost request = new HttpPost(genericConfigService.getConfig().get("set-chatGptApiImageEndpoint"));
            request.addHeader("api-key",genericConfigService.getConfig().get("set-chatGptImage-key"));
            request.addHeader("Content-Type", "application/json");
            String requestBodys = requestObject.toString();
            request.setEntity(new StringEntity(requestBodys));

            CloseableHttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
                String output;
                String responseJsonString = "";

                while ((output = br.readLine()) != null) {
                    responseJsonString = responseJsonString + output;
                }
                JsonArray result = new JsonArray();

                JsonObject convertedObject = new Gson().fromJson(responseJsonString, JsonObject.class);
                if (convertedObject.has("data")) {

                    //imageurl=convertedObject.get("data").getAsJsonArray().getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                    imageurl=imageCreationInDAM(convertedObject.get("data").getAsJsonArray().getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString(),country,imgsize,session);
                    logger.debug("Chatgpt Image Prompt : "+imageurl);
                }
                return  imageurl;
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.close();
        }

        return null;

    }

    private String imageCreationInDAM(String reimagepath,String country,String imgsize,Session session)  {
        InputStream is = null;
        try {

            ResourceResolver resourceResolver = null;
            Asset imageAsset = null;
            resourceResolver = resourceHelper.getResourceResolver();
            String imagepath = "";

            String mimeType = "";
            URL Url = new URL(reimagepath);
            URLConnection uCon = null;

            uCon = Url.openConnection();

            is = uCon.getInputStream();
            mimeType = uCon.getContentType();
            String fileExt = StringUtils.EMPTY;

            fileExt = mimeType.replaceAll("image/", "");

            String dampath= "/content/dam/futuregeneraliindiainsurancecoltd/ai-model/"+country+"";
            Node imageNode = resourceResolver.getResource(dampath).adaptTo(Node.class);

            boolean imagenodeExists = doesImageExist(imageNode, "dam:Asset");

            if (!imagenodeExists) {
                AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);

                if(imgsize.contains("1024")) {
                    imageAsset = assetManager.createAsset("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/"+country+"/"+country.toLowerCase()+"-"+"desktop"+"."+ fileExt, is, mimeType, true);
                }
                else{
                    imageAsset = assetManager.createAsset("/content/dam/futuregeneraliindiainsurancecoltd/ai-model/"+country+"/"+country.toLowerCase()+"-"+"mobileview"+"."+ fileExt, is, mimeType, true);

                }
                imagepath= imageAsset.getPath();
            }





            return imagepath;
        }catch (IOException e) {
            throw new RuntimeException(e);
        } catch (LoginException e) {
            throw new RuntimeException(e);
        } catch (LockException e) {
            throw new RuntimeException(e);
        } catch (ItemExistsException e) {
            throw new RuntimeException(e);
        } catch (ConstraintViolationException e) {
            throw new RuntimeException(e);
        } catch (ValueFormatException e) {
            throw new RuntimeException(e);
        } catch (PathNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchNodeTypeException e) {
            throw new RuntimeException(e);
        } catch (VersionException e) {
            throw new RuntimeException(e);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    private boolean doesImageExist(Node parentNode, String nodeName) throws RepositoryException {
        NodeIterator nodeIterator = parentNode.getNodes();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            if (node.getProperty("jcr:primaryType").getString().equals(nodeName)) {
                return true; // Node with the given name already exists
            }
        }
        return false; // Node with the given name does not exist
    }
}
