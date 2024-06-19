package com.mysite.core.ValidationFramework.service.impl;

import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
import com.mysite.core.ValidationFramework.service.ValidationFrameworkService;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = ValidationFrameworkService.class,immediate = true)
public class ValidationFrameworkServiceImpl implements ValidationFrameworkService {
    @Reference
    ResourceHelper resourceHelper;
    Logger logger= LoggerFactory.getLogger(this.getClass());
    @Override
    public List<Map<String,String>> getSingleFieldValues() {
        Session session = null;
        ResourceResolver resourceResolver = null;
        List<Map<String,String>>rootlst=new ArrayList<>();

        try{
            resourceResolver= resourceHelper.getResourceResolver();
            session = resourceResolver.adaptTo(Session.class);

            String cfpath="/content/dam/tataaialifeinsurancecompanylimited/validation-framework-without-multifield";
            Map<String,String>datamap=new HashMap<>();
            if(session.itemExists(cfpath))
            {
                NodeIterator iterator=session.getNode(cfpath).getNodes();
                while(iterator.hasNext())
                {
                    Node cfdatanode= iterator.nextNode();
                    if(checkRepPolicyNode(cfdatanode) && checkJcrContentNode(cfdatanode) && checkCheckBoxNode(cfdatanode) && checkRadioNode(cfdatanode) && checkSelectDropdownNode(cfdatanode))
                    {
                        if(cfdatanode.getName().equals("dob"))
                        {
                            datamap.put("dob","<div data-valid-name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("dataValidName").getString()+"\">\n" +
                                    "<label for=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("for").getString()+"\">"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("for").getString()+"</label>\n" +
                                    "<input id=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("id").getString()+"\" type=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("type").getString()+"\" placeholder=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("placeholder").getString()+"\" name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("name").getString()+"\" data-pattern=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("dataPattern").getString()+"\">\n" +
                                    "<p class=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errorclass").getString()+"\" >"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errorMsg").getString()+"</p>\n" +
                                    "</div>");
                        } else if (cfdatanode.getName().equals("email")) {
                            datamap.put("email","<div data-valid-name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("dataValidName").getString()+"\">\n" +
                                    "<label for=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("for").getString()+"\">"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("for").getString()+"</label>\n" +
                                    "<input id=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("id").getString()+"\" type=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("type").getString()+"\" placeholder=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("placeholder").getString()+"\" name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("name").getString()+"\" data-pattern=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("dataPattern").getString()+"\">\n" +
                                    "<p class=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errorclass").getString()+"\" >"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errorMsg").getString()+"</p>\n" +
                                    "</div>");
                        } else if (cfdatanode.getName().equals("fullname")) {
                            datamap.put("fullname","<div data-valid-name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("dataValidName").getString()+"\">\n" +
                                    "<label for=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("for").getString()+"\">"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("for").getString()+"</label>\n" +
                                    "<input id=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("id").getString()+"\" type=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("type").getString()+"\" placeholder=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("placeholder").getString()+"\" name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("name").getString()+"\" data-pattern=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("dataPattern").getString()+"\">\n" +
                                    "<p class=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errorclass").getString()+"\" >"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errorMsg").getString()+"</p>\n" +
                                    "</div>");
                        }
                        else if (cfdatanode.getName().equals("mobile")) {
                            datamap.put("mobile","<div data-valid-name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("dataValidName").getString()+"\">\n" +
                                    "<label for=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("for").getString()+"\">"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("for").getString()+"</label>\n" +
                                    "<input id=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("id").getString()+"\" type=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("type").getString()+"\" placeholder=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("placeholder").getString()+"\" name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("name").getString()+"\" data-pattern=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("dataPattern").getString()+"\">\n" +
                                    "<p class=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errorclass").getString()+"\" >"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errorMsg").getString()+"</p>\n" +
                                    "</div>");
                        }
                    }

                }
                rootlst.add(datamap);
            }
        }
        catch (Exception e)
        {
            logger.error("Exception in Validation Framework sefvice {}",e);
        }

        return rootlst;
    }

    @Override
    public List<Map<String, String>> getMultiFieldValues() {

        Session session = null;
        ResourceResolver resourceResolver = null;
        List<Map<String,String>>rootmultilst=new ArrayList<>();
        try{
            resourceResolver= resourceHelper.getResourceResolver();
            session = resourceResolver.adaptTo(Session.class);

            String cfpath="/content/dam/tataaialifeinsurancecompanylimited/validation-framework-multifield";
            Map<String,String>datamap=new HashMap<>();
            if(session.itemExists(cfpath))
            {
                NodeIterator iterator=session.getNode(cfpath).getNodes();
                while (iterator.hasNext())
                {
                    Node cfdatanode= iterator.nextNode();
                    if(checkRepPolicyNode(cfdatanode) && checkJcrContentNode(cfdatanode))
                    {
                        if(cfdatanode.getName().equals("radio"))
                        {
                            StringBuilder stringBuilder= new StringBuilder("<div data-valid-name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("dataValidName").getString()+"\">\n" +
                                    "<label class=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("labelclass").getString()+"\">"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("labeltext").getString()+"</label>\n");
                            Value[] cfarray=cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("id").getValues();
                            Value[] cfarrayvalue=cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("value").getValues();
                            Value[] cfarraylabelinner=cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("innerlabeltext").getValues();
                            boolean isCheckedSet = false;
                            for(int i=0;i<cfarray.length;i++)
                            {
                                boolean shouldCheck = !isCheckedSet;
//                                stringBuilder.append(" <input type=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("type").getString()+"\" id=\""+cfarray[i]+"\" name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("name").getString()+"\" value=\""+cfarrayvalue[i]+"\" "+shouldCheck ?+"checked="+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("deafultnumber").getString()+">":"\"> \n" +
//                                        "<label class=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("labelinnerclass").getString()+"\" for=\""+cfarrayvalue[i]+"\">"+cfarraylabelinner[i]+"</label>\n");
                                stringBuilder.append(" <input type=\"")
                                        .append(cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("type").getString())
                                        .append("\" id=\"").append(cfarray[i])
                                        .append("\" name=\"").append(cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("name").getString())
                                        .append("\" value=\"").append(cfarrayvalue[i]).append("\" ")
                                        .append(shouldCheck ? "checked="+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("deafultnumber").getString()+"" : "")
                                        .append("> \n")
                                        .append("<label class=\"")
                                        .append(cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("labelinnerclass").getString())
                                        .append("\" for=\"").append(cfarrayvalue[i])
                                        .append("\">").append(cfarraylabelinner[i])
                                        .append("</label>\n");
                                if (shouldCheck) {
                                    //stringBuilder.append(" checked="+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("deafultnumber").getString()+">");
                                    isCheckedSet = true;  // Set the flag to true since the checked attribute has been added
                                }
                            }
                            stringBuilder.append("<p class=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errorclass").getString()+"\">"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errormsg").getString()+"</p>\n" +
                                    "</div>");
                            datamap.put("radio", String.valueOf(stringBuilder));
                        } else if (cfdatanode.getName().equals("select")) {

                            StringBuilder stringBuilder= new StringBuilder("<div data-valid-name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("dataValidName").getString()+"\">\n" +
                                    "<label class=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("labelclass").getString()+"\">"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("labeltext").getString()+"</label>\n");
                            // Value[] cfarray=cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("id").getValues();
                            Value[] cfarrayvalue=cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("value").getValues();
                            Value[] cfarraylabelinner=cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("innerlabeltext").getValues();
                            boolean isCheckedSet = false;
                            for(int i=0;i<cfarrayvalue.length;i++)
                            {
                                boolean shouldCheck = !isCheckedSet;
//                                stringBuilder.append(" <input type=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("type").getString()+"\" name=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("name").getString()+"\" value=\""+cfarrayvalue[i]+"\" checked="+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("deafultnumber").getString()+"> \n" +
//                                        "<label class=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("labelinnerclass").getString()+"\" for=\""+cfarrayvalue[i]+"\">"+cfarraylabelinner[i]+"</label>\n");

                                stringBuilder.append(" <input type=\"")
                                        .append(cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("type").getString())
                                        .append("\" name=\"").append(cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("name").getString())
                                        .append("\" value=\"").append(cfarrayvalue[i]).append("\" ")
                                        .append(shouldCheck ? "checked="+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("deafultnumber").getString()+"" : "")
                                        .append("> \n")
                                        .append("<label class=\"")
                                        .append(cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("labelinnerclass").getString())
                                        .append("\" for=\"").append(cfarrayvalue[i])
                                        .append("\">").append(cfarraylabelinner[i])
                                        .append("</label>\n");

                                if (shouldCheck) {
                                    isCheckedSet = true;  // Set the flag to true since the checked attribute has been added
                                }
                            }
                            stringBuilder.append("<p class=\""+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errorclass").getString()+"\">"+cfdatanode.getNode("jcr:content").getNode("data").getNode("master").getProperty("errormsg").getString()+"</p>\n" +
                                    "</div>");
                            datamap.put("select", String.valueOf(stringBuilder));
                        }

                    }

                }
                rootmultilst.add(datamap);
            }

        }
        catch (Exception e)
        {
            logger.error("Exception in multivalue field {}",e);
        }


        return rootmultilst;
    }

    private boolean checkJcrContentNode(Node node) throws RepositoryException {
        if (node.getName().contains("jcr")) {
            return false;
        }
        return true;
    }

    private boolean checkRepPolicyNode(Node node) throws RepositoryException {
        if (node.getName().contains("rep:policy")) {
            return false;
        }
        return true;
    }
    private boolean checkCheckBoxNode(Node node) throws RepositoryException {
        if (node.getName().contains("checkbox")) {
            return false;
        }
        return true;
    }

    private boolean checkRadioNode(Node node) throws RepositoryException {
        if (node.getName().contains("radio")) {
            return false;
        }
        return true;
    }
    private boolean checkSelectDropdownNode(Node node) throws RepositoryException {
        if (node.getName().contains("select")) {
            return false;
        }
        return true;
    }
}
