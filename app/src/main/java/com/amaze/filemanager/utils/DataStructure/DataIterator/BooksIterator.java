package com.amaze.filemanager.utils.DataStructure.DataIterator;

import com.amaze.filemanager.utils.DataStructure.BookShelf;

public class BooksIterator implements Iterator {

    private BookShelf bookShelf;
    private int index ;

    public BooksIterator(BookShelf bookShelf) {
        this.bookShelf = bookShelf;
        this.index = 0 ;
    }

    @Override
    public boolean hasNext() {
        if ( index < bookShelf.getLength()) {
            return true ;
        } else {
            return false;
        }
    }

    @Override
    public Object next() {
        Object aBook = bookShelf.getBookAt(index) ;
        index++ ;
        return aBook;
    }
}
