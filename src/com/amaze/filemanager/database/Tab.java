package com.amaze.filemanager.database;

/**
 * Created by Vishal on 9/17/2014.
 */
public class Tab {

    private int _id;
    private int _tab_no;
    private String _label;
    private String _path;

    public Tab() {
        // Empty constructor
    }

    public Tab(int id, int tab_no, String label, String path) {
        this._id = id;
        this._tab_no = tab_no;
        this._label = label;
        this._path = path;

    }

    public Tab(int tab_no, String label, String path) {
        this._tab_no = tab_no;
        this._label = label;
        this._path = path;
    }

    public Tab(int id, int tab_no, String label) {
        this._id = id;
        this._tab_no = tab_no;
        this._label = label;
    }

    public Tab(int id, int tab_no) {
        this._id = id;
        this._tab_no = tab_no;
    }

    public Tab(int id) {
        this._id = id;
    }

    public void setID(int id) {
        this._id = id;
    }

    public int getID() {
        return this._id;
    }

    public void setTab(int tab_no) {
        this._tab_no = tab_no;
    }

    public int getTab() {
        return this._tab_no;
    }

    public void setLabel(String label) {
        this._label = label;
    }

    public String getLabel() {
        return this._label;
    }

    public void setPath(String path) {
        this._path = path;
    }

    public String getPath() {
        return this._path;
    }
}
