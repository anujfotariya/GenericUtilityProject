package com.mysite.core.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Collectors;

@Component(service= Servlet.class,
        property={"sling.servlet.methods="+ HttpConstants.METHOD_GET,
                "sling.servlet.methods="+HttpConstants.METHOD_POST,
                "sling.servlet.resourceTypes="+ "/apps/database",
                "sling.servlet.extensions="+"json"})
public class MyDatabaseServiceServlet extends SlingAllMethodsServlet {
    @Reference
    DataSource dataSource;

    Logger logger = LoggerFactory.getLogger(MyDatabaseServiceServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        ResourceResolver resourceResolver = null;
        try {
            PrintWriter out = response.getWriter();
            out.println("welcome to Aem Postgres Connection");

            if (dataSource != null) {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM employee");
                     ResultSet resultSet = preparedStatement.executeQuery()) {

                    while (resultSet.next()) {
                        out.println(resultSet.getString(1) + " " + resultSet.getString(2) + " " + resultSet.getString(3));
                    }
                } catch (SQLException e) {
                    logger.error("SQL Exception: {}", e.getMessage());
                }
            } else {
                out.println("DataSource is null");
                logger.error("DataSource is null");
            }


        } catch (Exception e) {
            logger.error("Exception :{}", e);
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        String req = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        JsonObject requestObj = new JsonParser().parse(req).getAsJsonObject();

        String operation = requestObj.get("operation").getAsString();
        String name = requestObj.get("name").getAsString();
        String dept = requestObj.get("dept").getAsString();
        int salary = requestObj.get("salary").getAsInt();

        if (dataSource != null) {

            switch (operation) {
                case "insert":
                    handleInsert(requestObj, out, response);
                    break;
                case "update":
                    handleUpdate(requestObj, out, response);
                    break;
                case "delete":
                    handleDelete(requestObj, out, response);
                    break;
                default:
                    out.println("Invalid operation");

            }

        } else {
            out.println("datasource is null");
        }


    }

    private void handleInsert(JsonObject requestObj, PrintWriter out, SlingHttpServletResponse response) {
        String name = requestObj.get("name").getAsString();
        String dept = requestObj.get("dept").getAsString();
        int salary = requestObj.get("salary").getAsInt();

        String insertSQL = "INSERT INTO employee (name, dept, salary) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {

            preparedStatement.setString(1, name);
            preparedStatement.setString(2, dept);
            preparedStatement.setInt(3, salary);

            int rowsInserted = preparedStatement.executeUpdate();
            if (rowsInserted > 0) {
                out.println("A new row was inserted successfully!");
            } else {
                out.println("No rows were inserted.");
            }
        } catch (SQLException e) {
            logger.error("SQL Exception: {}", e.getMessage());
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("SQL Exception: " + e.getMessage());
        }
    }

    private void handleUpdate(JsonObject requestObj, PrintWriter out, SlingHttpServletResponse response) {
        try {
            String name = requestObj.get("name").getAsString();
            String dept = requestObj.get("dept").getAsString();
            int salary = requestObj.get("salary").getAsInt();

            String updateSQL = "UPDATE employee SET dept = ?, salary = ? WHERE name = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(updateSQL)) {

                preparedStatement.setString(1, dept);
                preparedStatement.setInt(2, salary);
                preparedStatement.setString(3, name);

                int rowsUpdated = preparedStatement.executeUpdate();
                if (rowsUpdated > 0) {
                    out.println("The row was updated successfully!");
                } else {
                    out.println("No rows were updated.");
                }
            } catch (SQLException e) {
                logger.error("SQL Exception: {}", e.getMessage());
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("SQL Exception: " + e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Exception {}", e);
        }
    }

    private void handleDelete(JsonObject requestObj, PrintWriter out, SlingHttpServletResponse response) {
        try {
            String name = requestObj.get("name").getAsString();

            String deleteSQL = "DELETE FROM employee WHERE name = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(deleteSQL)) {

                preparedStatement.setString(1, name);

                int rowsDeleted = preparedStatement.executeUpdate();
               if(rowsDeleted>0)
               {
                   logger.info("rows deleted successfully",rowsDeleted);
                   out.println("Rows deleted Successfully");
               }
               else{
                   out.println("Rows not deleted");
               }
            } catch (SQLException e) {
                logger.error("SQL Exception: {}", e.getMessage());
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("SQL Exception: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("JSON Parsing Exception: {}", e.getMessage());
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            out.println("Invalid JSON data");
        }
    }
}

