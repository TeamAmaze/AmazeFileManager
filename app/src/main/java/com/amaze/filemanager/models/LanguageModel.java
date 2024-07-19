package com.amaze.filemanager.models;

// DataModel.java
public class LanguageModel {
    private String title;
    private String description;

    public LanguageModel(String text,String description) {
        this.title = text;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String text) {
        this.title = text;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String text) {
        this.description = text;
    }
}
