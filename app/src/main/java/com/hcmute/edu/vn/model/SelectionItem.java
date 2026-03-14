package com.hcmute.edu.vn.model;

public class SelectionItem {
    private String name;
    private boolean isSelected;

    public SelectionItem(String name) {
        this.name = name;
        this.isSelected = false;
    }

    public String getName() { return name; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}