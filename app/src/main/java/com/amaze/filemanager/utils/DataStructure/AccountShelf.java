package com.amaze.filemanager.utils.DataStructure;

import com.amaze.filemanager.utils.DataStructure.DataIterator.AccountsIterator;
import com.amaze.filemanager.utils.DataStructure.DataIterator.Iterator;
import com.cloudrail.si.interfaces.CloudStorage;
import java.util.ArrayList;

public class AccountShelf implements Aggregate {

    private ArrayList<CloudStorage> accounts;

    public AccountShelf() {
        //this.books = DataUtils.getInstance().getBooks() ;
        this.accounts = new ArrayList<>() ;
    }

    public void addAccount(CloudStorage storage) {
        accounts.add(storage) ;
    }

    public void removeAccount(CloudStorage storage) {
        accounts.remove(storage) ;
    }

    public ArrayList<CloudStorage> getAccountList() {
        return accounts;
    }

    public void setAccounts(ArrayList<CloudStorage> accounts) {
        this.accounts = this.accounts;
    }

    public CloudStorage getAccountAt(int index) {
        return accounts.get(index) ;
    }

    public int getLength() {
        return accounts.size() ;
    }

    public void clearAccounts() {
        accounts.clear();
    }

    @Override
    public Iterator createIterator() {
        return new AccountsIterator(this);
    }
}
