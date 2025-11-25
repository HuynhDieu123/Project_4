package com.restaurant.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("menuPackagesBean")
@ViewScoped
public class MenuPackagesBean implements Serializable {

    private String activeTab; // "items", "packages", "categories"

    private List<MenuItem> items;
    private List<MenuPackage> packages;
    private List<MenuCategory> categories;

    @PostConstruct
    public void init() {
        activeTab = "items";

        items = new ArrayList<>();
        items.add(new MenuItem("Grilled Salmon", "Atlantic salmon fillet grilled to perfection.",
                28.50));
        items.add(new MenuItem("Filet Mignon", "Center-cut tenderloin with red wine sauce.",
                42.00));
        items.add(new MenuItem("Lobster Risotto", "Creamy risotto with fresh lobster.",
                35.00));
        items.add(new MenuItem("Truffle Pasta", "Tagliatelle with truffle cream sauce.",
                25.00));

        packages = new ArrayList<>();
        packages.add(new MenuPackage("The Grand Wedding Feast", "Perfect for weddings, includes appetizers, mains & dessert bar.",
                1250.00));
        packages.add(new MenuPackage("Executive Corporate Lunch", "Quick, delicious and convenient for meetings.",
                450.00));
        packages.add(new MenuPackage("Holiday Celebration Feast", "Classic holiday dishes from turkey to pies.",
                800.00));

        categories = new ArrayList<>();
        categories.add(new MenuCategory("Main Courses", 12));
        categories.add(new MenuCategory("Appetizers", 8));
        categories.add(new MenuCategory("Desserts", 6));
        categories.add(new MenuCategory("Beverages", 15));
    }

    // ====== Actions for tabs ======

    public String getActiveTab() {
        return activeTab;
    }

    public void showItems() { activeTab = "items"; }
    public void showPackages() { activeTab = "packages"; }
    public void showCategories() { activeTab = "categories"; }

    // ====== Getters ======

    public List<MenuItem> getItems() { return items; }

    public List<MenuPackage> getPackages() { return packages; }

    public List<MenuCategory> getCategories() { return categories; }

    // ====== Inner classes ======

    public static class MenuItem implements Serializable {
        private String name;
        private String description;
        private double price;

        public MenuItem(String name, String description, double price) {
            this.name = name;
            this.description = description;
            this.price = price;
        }

        public String getName() { return name; }

        public String getDescription() { return description; }

        public double getPrice() { return price; }
    }

    public static class MenuPackage implements Serializable {
        private String name;
        private String description;
        private double price;

        public MenuPackage(String name, String description, double price) {
            this.name = name;
            this.description = description;
            this.price = price;
        }

        public String getName() { return name; }

        public String getDescription() { return description; }

        public double getPrice() { return price; }
    }

    public static class MenuCategory implements Serializable {
        private String name;
        private int itemCount;

        public MenuCategory(String name, int itemCount) {
            this.name = name;
            this.itemCount = itemCount;
        }

        public String getName() { return name; }

        public int getItemCount() { return itemCount; }
    }
}
