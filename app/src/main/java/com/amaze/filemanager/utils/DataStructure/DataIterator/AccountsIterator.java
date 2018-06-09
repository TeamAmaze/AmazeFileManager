package com.amaze.filemanager.utils.DataStructure.DataIterator;

import com.amaze.filemanager.utils.DataStructure.AccountShelf;

public class AccountsIterator implements Iterator {

    private AccountShelf accountShelf;
    private int index ;

    public AccountsIterator(AccountShelf accountShelf) {
        this.accountShelf = accountShelf;
        this.index = 0 ;
    }

    @Override
    public boolean hasNext() {
        if ( index < accountShelf.getLength()) {
            return true ;
        } else {
            return false;
        }
    }

    @Override
    public Object next() {
        Object aAccount = accountShelf.getAccountAt(index) ;
        index++ ;
        return aAccount;
    }
}