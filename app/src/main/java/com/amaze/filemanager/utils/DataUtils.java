package com.amaze.filemanager.utils;

import com.amaze.filemanager.ui.drawer.Item;
import com.amaze.filemanager.utils.application.AppConfig;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Box;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radix.node.concrete.voidvalue.VoidValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by arpitkh996 on 20-01-2016.
 *
 * Singleton class to handle data for various services
 */

//Central data being used across activity,fragments and classes
public class DataUtils {

    public static final int DELETE = 0, COPY = 1, MOVE = 2, NEW_FOLDER = 3,
            RENAME = 4, NEW_FILE = 5, EXTRACT = 6, COMPRESS = 7;

    private ConcurrentRadixTree<VoidValue> hiddenfiles = new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());
    private ArrayList<String> gridfiles = new ArrayList<>(),
            listfiles = new ArrayList<>(), history = new ArrayList<>(), storages = new ArrayList<>();

    private ArrayList<Item> list = new ArrayList<>();
    private ArrayList<String[]> servers = new ArrayList<>(), books = new ArrayList<>();

    private ArrayList<CloudStorage> accounts = new ArrayList<>(4);

    private DataChangeListener dataChangeListener;

    private static DataUtils sDataUtils;

    public static DataUtils getInstance() {
        if (sDataUtils == null) {
            sDataUtils = new DataUtils();
        }
        return sDataUtils;
    }

    public int containsServer(String[] a) {
        return contains(a, servers);
    }

    public int containsServer(String path) {

        synchronized (servers) {

            if (servers == null) return -1;
            int i = 0;
            for (String[] x : servers) {
                if (x[1].equals(path)) return i;
                i++;

            }
        }
        return -1;
    }

    public int containsBooks(String[] a) {
        return contains(a, books);
    }

    /*public int containsAccounts(CloudEntry cloudEntry) {
        return contains(a, accounts);
    }*/

    /**
     * Checks whether cloud account of certain type is present or not
     * @param serviceType the {@link OpenMode} of account to check
     * @return the index of account, -1 if not found
     */
    public synchronized int containsAccounts(OpenMode serviceType) {
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

    public void clear() {
        hiddenfiles = new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());
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
        clear();
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
        synchronized (books) {

            if (books.size() > i)
                books.remove(i);
        }
    }

    public synchronized void removeAccount(OpenMode serviceType) {
        for (CloudStorage storage : accounts) {
            switch (serviceType) {
                case BOX:
                    if (storage instanceof Box) {
                        accounts.remove(storage);
                        return;
                    }
                    break;
                case DROPBOX:
                    if (storage instanceof Dropbox) {
                        accounts.remove(storage);
                        return;
                    }
                    break;
                case GDRIVE:
                    if (storage instanceof GoogleDrive) {
                        accounts.remove(storage);
                        return;
                    }
                    break;
                case ONEDRIVE:
                    if (storage instanceof OneDrive) {
                        accounts.remove(storage);
                        return;
                    }
                    break;
                default:
                    return;
            }
        }
    }

    public void removeServer(int i) {
        synchronized (servers) {

            if (servers.size() > i)
                servers.remove(i);
        }
    }

    public void addBook(String[] i) {
        synchronized (books) {

            books.add(i);
        }
    }

    public void addBook(final String[] i, boolean refreshdrawer) {
        synchronized (books) {

            books.add(i);
        }
        if (refreshdrawer && dataChangeListener != null) {
            AppConfig.runInBackground(() -> dataChangeListener.onBookAdded(i, true));
        }
    }

    public void addAccount(CloudStorage storage) {
        accounts.add(storage);
    }

    public void addServer(String[] i) {
        servers.add(i);
    }

    public void addHiddenFile(final String i) {

        synchronized (hiddenfiles) {

            hiddenfiles.put(i, VoidValue.SINGLETON);
        }
        if (dataChangeListener != null) {
            AppConfig.runInBackground(() -> dataChangeListener.onHiddenFileAdded(i));
        }
    }

    public void removeHiddenFile(final String i) {

        synchronized (hiddenfiles) {

            hiddenfiles.remove(i);
        }
        if (dataChangeListener != null) {
            AppConfig.runInBackground(() -> dataChangeListener.onHiddenFileRemoved(i));
        }
    }

    public ArrayList<String> getHistory() {
        return history;
    }

    public void addHistoryFile(final String i) {

        synchronized (history) {

            history.add(i);
        }
        if (dataChangeListener != null) {
            AppConfig.runInBackground(() -> dataChangeListener.onHistoryAdded(i));
        }
    }

    public void sortBook() {
        Collections.sort(books, new BookSorter());
    }

    public synchronized void setServers(ArrayList<String[]> servers) {
        if (servers != null)
            this.servers = servers;
    }

    public synchronized void setBooks(ArrayList<String[]> books) {
        if (books != null)
            this.books = books;
    }

    public synchronized void setAccounts(ArrayList<CloudStorage> accounts) {
        if (accounts != null)
            this.accounts = accounts;
    }

    public synchronized ArrayList<String[]> getServers() {
        return servers;
    }

    public synchronized ArrayList<String[]> getBooks() {
        return books;
    }

    public synchronized ArrayList<CloudStorage> getAccounts() {
        return accounts;
    }

    public synchronized CloudStorage getAccount(OpenMode serviceType) {
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

    public boolean isFileHidden(String path) {
        return getHiddenFiles().getValueForExactKey(path) != null;
    }

    public ConcurrentRadixTree<VoidValue> getHiddenFiles() {
        return hiddenfiles;
    }

    public synchronized void setHiddenFiles(ConcurrentRadixTree<VoidValue> hiddenfiles) {
        if (hiddenfiles != null) this.hiddenfiles = hiddenfiles;
    }

    public ArrayList<String> getGridFiles() {
        return gridfiles;
    }

    public synchronized void setGridfiles(ArrayList<String> gridfiles) {
        if (gridfiles != null)
            this.gridfiles = gridfiles;
    }

    public ArrayList<String> getListfiles() {
        return listfiles;
    }

    public synchronized void setListfiles(ArrayList<String> listfiles) {
        if (listfiles != null)
            this.listfiles = listfiles;
    }

    public void clearHistory() {
        history = new ArrayList<>();
        if (dataChangeListener != null) {
            AppConfig.runInBackground(new Runnable() {
                @Override
                public void run() {
                    dataChangeListener.onHistoryCleared();
                }
            });
        }
    }

    public synchronized List<String> getStorages() {
        return storages;
    }

    public synchronized void setStorages(ArrayList<String> storages) {
        this.storages = storages;
    }

    public ArrayList<Item> getList() {
        return list;
    }

    public synchronized void setList(ArrayList<Item> list) {
        this.list = list;
    }

    /**
     * Callbacks to do original changes in database (and ui if required)
     * The callbacks are called in a background thread
     */
    public interface DataChangeListener {
        void onHiddenFileAdded(String path);

        void onHiddenFileRemoved(String path);

        void onHistoryAdded(String path);

        void onBookAdded(String path[], boolean refreshdrawer);

        void onHistoryCleared();
    }

}
