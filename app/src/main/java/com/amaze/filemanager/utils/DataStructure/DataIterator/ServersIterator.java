package com.amaze.filemanager.utils.DataStructure.DataIterator;

import com.amaze.filemanager.utils.DataStructure.ServerShelf;

public class ServersIterator implements Iterator {

    private ServerShelf serverShelf;
    private int index ;

    public ServersIterator(ServerShelf serverShelf) {
        this.serverShelf = serverShelf;
        this.index = 0 ;
    }

    @Override
    public boolean hasNext() {
        if ( index < serverShelf.getLength()) {
            return true ;
        } else {
            return false;
        }
    }

    @Override
    public Object next() {
        Object aServer = serverShelf.getServerAt(index) ;
        index++ ;
        return aServer;
    }
}
