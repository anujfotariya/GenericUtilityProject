package com.mysite.core.models;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class NavigationMenuModel {

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private Resource resource;

    private List<NavigationItem> items;

    public String getJsonObject() {
        return jsonObject;
    }

    private String jsonObject;

    @PostConstruct
    protected void init() {
        // Get the PageManager from the ResourceResolver
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        if (pageManager != null) {
            // Fetch the current page
            Page currentPage = pageManager.getContainingPage(resource);
            if (currentPage != null) {
                items = buildNavigation(currentPage);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                jsonObject=gson.toJson(items);
            }
        }
    }

    public List<NavigationItem> getItems() {
        return items;
    }

    // Build the navigation menu items
    private List<NavigationItem> buildNavigation(Page currentPage) {
        List<NavigationItem> navigationItems = new ArrayList<>();
        Iterator<Page> siblings = currentPage.listChildren();

        while (siblings.hasNext()) {
            Page sibling = siblings.next();
            if (sibling.isHideInNav()) {
                continue;  // Skip hidden pages
            }
            navigationItems.add(new NavigationItem(sibling, currentPage));
        }
        return navigationItems;
    }

    // Nested class to represent a navigation item
    public static class NavigationItem {
        private String title;
        private String url;
        private boolean isActive;
        private List<NavigationItem> children;

        public NavigationItem(Page page, Page currentPage) {
            this.title = page.getTitle() != null ? page.getTitle() : page.getName();
            this.url = page.getPath() + ".html";
            this.isActive = page.getPath().equals(currentPage.getPath());
            this.children = buildNavigationChildren(page);
        }

        // Build children of the current page for multi-level navigation
        private List<NavigationItem> buildNavigationChildren(Page page) {
            List<NavigationItem> childItems = new ArrayList<>();
            Iterator<Page> children = page.listChildren();

            while (children.hasNext()) {
                Page childPage = children.next();
                if (!childPage.isHideInNav()) {
                    childItems.add(new NavigationItem(childPage, page));
                }
            }
            return childItems.isEmpty() ? null : childItems;
        }

        // Getters for title, url, isActive, and children
        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public boolean isActive() {
            return isActive;
        }

        public List<NavigationItem> getChildren() {
            return children;
        }
    }
}
