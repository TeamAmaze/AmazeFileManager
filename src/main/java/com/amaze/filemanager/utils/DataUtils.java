package com.amaze.filemanager.utils;

import com.amaze.filemanager.ui.drawer.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by arpitkh996 on 20-01-2016.
 */

//Central data being used across activity,fragments and classes
public class DataUtils {

    public static final int DELETE = 0, COPY = 1, MOVE = 2, NEW_FOLDER = 3,
            RENAME = 4, NEW_FILE = 5, EXTRACT = 6, COMPRESS = 7;
    public static final String DRIVE = "drive", SMB = "smb", BOOKS = "books",
            HISTORY = "Table1", HIDDEN = "Table2", LIST = "list", GRID = "grid";

    private ArrayList<String> hiddenfiles = new ArrayList<>(), gridfiles = new ArrayList<>(),
            listfiles = new ArrayList<>(), history = new ArrayList<>(), storages = new ArrayList<>();

    private ArrayList<Item> list = new ArrayList<>();
    private ArrayList<String[]> servers = new ArrayList<>(), books = new ArrayList<>(),
            accounts = new ArrayList<>();

    private DataChangeListener dataChangeListener;

    public DataUtils() {

    }

    public int containsServer(String[] a) {
        return contains(a, servers);
    }

    public int containsServer(String path) {
        if (servers == null) return -1;
        int i = 0;
        for (String[] x : servers) {
            if (x[1].equals(path)) return i;
            i++;

        }
        return -1;
    }

    public int containsBooks(String[] a) {
        return contains(a, books);
    }

    public int containsAccounts(String[] a) {
        return contains(a, accounts);
    }

    public int containsAccounts(String a) {
        return contains(a, accounts);
    }

    public void clear() {
        hiddenfiles = new ArrayList<>();
        gridfiles = new ArrayList<>();
        listfiles = new ArrayList<>();
        history = new ArrayList<>();
        storages = new ArrayList<>();
        servers = new ArrayList<>();
        books = new ArrayList<>();
        accounts = new ArrayList<>();
    }

    public void registerOnDataChangedListener(DataChangeListener l) {
        dataChangeListener = l;
    }

    int contains(String a, ArrayList<String[]> b) {
        int i = 0;
        for (String[] x : b) {
            if (x[1].equals(a)) return i;
            i++;

        }
        return -1;
    }

    int contains(String[] a, ArrayList<String[]> b) {
        if (b == null) return -1;
        int i = 0;
        for (String[] x : b) {
            if (x[0].equals(a[0]) && x[1].equals(a[1])) return i;
            i++;

        }
        return -1;
    }

    public void removeBook(int i) {
        if (books.size() > i)
            books.remove(i);
    }

    public void removeAcc(int i) {
        if (accounts.size() > i)
            accounts.remove(i);
    }

    public void removeServer(int i) {
        if (servers.size() > i)
            servers.remove(i);
    }

    public void addBook(String[] i) {
        books.add(i);
    }

    public void addBook(String[] i, boolean refreshdrawer) {
        if (refreshdrawer && dataChangeListener != null) dataChangeListener.onBookAdded(i, true);
        books.add(i);
    }

    public void addAcc(String[] i) {
        accounts.add(i);
    }

    public void addServer(String[] i) {
        servers.add(i);
    }

    public void addHiddenFile(String i) {
        hiddenfiles.add(i);
        if (dataChangeListener != null)
            dataChangeListener.onHiddenFileAdded(i);
    }

    public void removeHiddenFile(String i) {
        hiddenfiles.remove(i);
        if (dataChangeListener != null)
            dataChangeListener.onHiddenFileRemoved(i);
    }

    public ArrayList<String> getHistory() {
        return history;
    }

    public void addHistoryFile(String i) {
        history.add(i);
        if (dataChangeListener != null)
            dataChangeListener.onHistoryAdded(i);
    }

    public void sortBook() {
        Collections.sort(books, new BookSorter());
    }

    public void setServers(ArrayList<String[]> servers) {
        if (servers != null)
            this.servers = servers;
    }

    public void setBooks(ArrayList<String[]> books) {
        if (books != null)
            this.books = books;
    }

    public void setAccounts(ArrayList<String[]> accounts) {
        if (accounts != null)
            this.accounts = accounts;
    }

    public ArrayList<String[]> getServers() {
        return servers;
    }

    public ArrayList<String[]> getBooks() {
        return books;
    }

    public ArrayList<String[]> getAccounts() {
        return accounts;
    }

    public ArrayList<String> getHiddenfiles() {
        return hiddenfiles;
    }

    public void setHiddenfiles(ArrayList<String> hiddenfiles) {
        if (hiddenfiles != null)
            this.hiddenfiles = hiddenfiles;
    }

    public ArrayList<String> getGridFiles() {
        return gridfiles;
    }

    public void setGridfiles(ArrayList<String> gridfiles) {
        if (gridfiles != null)
            this.gridfiles = gridfiles;
    }

    public ArrayList<String> getListfiles() {
        return listfiles;
    }

    public void setListfiles(ArrayList<String> listfiles) {
        if (listfiles != null)
            this.listfiles = listfiles;
    }

    public void clearHistory() {
        history = new ArrayList<>();
        if (dataChangeListener != null)
            dataChangeListener.onHistoryCleared();
    }

    public List<String> getStorages() {
        return storages;
    }

    public void setStorages(ArrayList<String> storages) {
        this.storages = storages;
    }

    public ArrayList<Item> getList() {
        return list;
    }

    public void setList(ArrayList<Item> list) {
        this.list = list;
    }

    //Callbacks to do original changes in database (and ui if required)
    public interface DataChangeListener {
        void onHiddenFileAdded(String path);

        void onHiddenFileRemoved(String path);

        void onHistoryAdded(String path);

        void onBookAdded(String path[], boolean refreshdrawer);

        void onHistoryCleared();
    }

}
