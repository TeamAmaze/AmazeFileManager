package com.amaze.filemanager.utils;

import com.amaze.filemanager.database.CloudEntry;
import com.amaze.filemanager.ui.drawer.Item;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Box;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by arpitkh996 on 20-01-2016.
 */

//Central data being used across activity,fragments and classes
public class DataUtils {

    public static ArrayList<String> hiddenfiles = new ArrayList<>(), gridfiles = new ArrayList<>(),
            listfiles = new ArrayList<>(), history = new ArrayList<>();
    public static List<String> storages = new ArrayList<>();
    public static final String DRIVE = "drive", SMB = "smb", BOOKS = "books",
            HISTORY = "Table1", HIDDEN = "Table2", LIST = "list", GRID = "grid";
    public static final int DELETE = 0, COPY = 1, MOVE = 2, NEW_FOLDER = 3,
            RENAME = 4, NEW_FILE = 5, EXTRACT = 6, COMPRESS = 7;
    public static ArrayList<Item> list = new ArrayList<>();
    public static ArrayList<String[]> servers = new ArrayList<>(), books = new ArrayList<>();

    private static ArrayList<CloudStorage> accounts = new ArrayList<>(4);

    static DataChangeListener dataChangeListener;

    public static int containsServer(String[] a) {
        return contains(a, servers);
    }

    public static int containsServer(String path) {
        if (servers == null) return -1;
        int i = 0;
        for (String[] x : servers) {
            if (x[1].equals(path)) return i;
            i++;

        }
        return -1;
    }
    public static int containsBooks(String[] a) {
        return contains(a, books);
    }

    /*public static int containsAccounts(CloudEntry cloudEntry) {
        return contains(a, accounts);
    }*/

    /**
     * Checks whether cloud account of certain type is present or not
     * @param serviceType the {@link OpenMode} of account to check
     * @return the index of account, -1 if not found
     */
    public static synchronized int containsAccounts(OpenMode serviceType) {
        int i = 0;

        for (CloudStorage storage : accounts) {

            switch (serviceType) {
                case BOX:
                    if (storage instanceof Box)
                        return i;
                    break;
                case DROPBOX:
                    if (storage instanceof Dropbox)
                        return i;
                    break;
                case GDRIVE:
                    if (storage instanceof GoogleDrive)
                        return i;
                    break;
                case ONEDRIVE:
                    if (storage instanceof OneDrive)
                        return i;
                    break;
                default:
                    return -1;
            }
            i++;
        }
        return -1;
    }

    public static void clear() {
        hiddenfiles = new ArrayList<>();
        gridfiles = new ArrayList<>();
        listfiles = new ArrayList<>();
        history = new ArrayList<>();
        storages = new ArrayList<>();
        servers = new ArrayList<>();
        books = new ArrayList<>();
        accounts = new ArrayList<>();
    }

    public static void registerOnDataChangedListener(DataChangeListener dataChangeListener) {
        DataUtils.dataChangeListener = dataChangeListener;
    }

    static int contains(String a, ArrayList<String[]> b) {
        int i = 0;
        for (String[] x : b) {
            if (x[1].equals(a)) return i;
            i++;

        }
        return -1;
    }

    static int contains(String[] a, ArrayList<String[]> b) {
        if (b == null) return -1;
        int i = 0;
        for (String[] x : b) {
            if (x[0].equals(a[0]) && x[1].equals(a[1])) return i;
            i++;

        }
        return -1;
    }

    public static void removeBook(int i) {
        if (books.size() > i)
            books.remove(i);
    }

    public static synchronized void removeAccount(OpenMode serviceType) {

        for (CloudStorage storage : accounts) {
            switch (serviceType) {
                case BOX:
                    if (storage instanceof Box)
                        accounts.remove(storage);
                    break;
                case DROPBOX:
                    if (storage instanceof Dropbox)
                        accounts.remove(storage);
                    break;
                case GDRIVE:
                    if (storage instanceof GoogleDrive)
                        accounts.remove(storage);
                    break;
                case ONEDRIVE:
                    if (storage instanceof OneDrive)
                        accounts.remove(storage);
                    break;
                default:
                    return;
            }
        }
    }

    public static void removeServer(int i) {
        if (servers.size() > i)
            servers.remove(i);
    }

    public static void addBook(String[] i) {
        books.add(i);
    }

    public static void addBook(String[] i, boolean refreshdrawer) {
        if (refreshdrawer && dataChangeListener != null) dataChangeListener.onBookAdded(i, true);
        books.add(i);
    }

    public static synchronized void addAccount(CloudStorage storage) {
        accounts.add(storage);
    }

    public static void addServer(String[] i) {
        servers.add(i);
    }

    public static void addHiddenFile(String i) {
        hiddenfiles.add(i);
        if (dataChangeListener != null)
            dataChangeListener.onHiddenFileAdded(i);
    }

    public static void removeHiddenFile(String i) {
        hiddenfiles.remove(i);
        if (dataChangeListener != null)
            dataChangeListener.onHiddenFileRemoved(i);
    }

    public static void addHistoryFile(String i) {
        history.add(i);
        if (dataChangeListener != null)
            dataChangeListener.onHistoryAdded(i);
    }

    public static void sortBook() {
        Collections.sort(books, new BookSorter());
    }

    public static void setServers(ArrayList<String[]> servers) {
        if (servers != null)
            DataUtils.servers = servers;
    }

    public static void setBooks(ArrayList<String[]> books) {
        if (books != null)
            DataUtils.books = books;
    }

    public static synchronized void setAccounts(ArrayList<CloudStorage> accounts) {
        if (accounts != null)
            DataUtils.accounts = accounts;
    }

    public static synchronized ArrayList<String[]> getServers() {
        return servers;
    }

    public static synchronized ArrayList<CloudStorage> getAccounts() {
        return accounts;
    }

    public static synchronized CloudStorage getAccount(OpenMode serviceType) {

        for (CloudStorage storage : accounts) {

            switch (serviceType) {
                case BOX:
                    if (storage instanceof Box)
                        return storage;
                    break;
                case DROPBOX:
                    if (storage instanceof Dropbox)
                        return storage;
                    break;
                case GDRIVE:
                    if (storage instanceof GoogleDrive)
                        return storage;
                    break;
                case ONEDRIVE:
                    if (storage instanceof OneDrive)
                        return storage;
                    break;
                default:
                    return null;
            }
        }
        return null;
    }

    public static synchronized ArrayList<String[]> getBooks() {
        return books;
    }

    public static ArrayList<String> getHiddenfiles() {
        return hiddenfiles;
    }

    public static void setHiddenfiles(ArrayList<String> hiddenfiles) {
        if (hiddenfiles != null)
            DataUtils.hiddenfiles = hiddenfiles;
    }


    public static void setGridfiles(ArrayList<String> gridfiles) {
        if (gridfiles != null)
            DataUtils.gridfiles = gridfiles;
    }

    public static ArrayList<String> getListfiles() {
        return listfiles;
    }

    public static void setListfiles(ArrayList<String> listfiles) {
        if (listfiles != null)
            DataUtils.listfiles = listfiles;
    }

    public static void clearHistory() {
        history = new ArrayList<>();
        if (dataChangeListener != null)
            dataChangeListener.onHistoryCleared();
    }

    public static synchronized List<String> getStorages() {
        return storages;
    }

    public static void setStorages(List<String> storages) {
        DataUtils.storages = storages;
    }

    public static ArrayList<Item> getList() {
        return list;
    }

    public static synchronized void setList(ArrayList<Item> list) {
        DataUtils.list = list;
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
