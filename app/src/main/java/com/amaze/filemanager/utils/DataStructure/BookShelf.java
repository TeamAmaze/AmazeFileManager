package com.amaze.filemanager.utils.DataStructure;

import com.amaze.filemanager.utils.DataStructure.DataIterator.BooksIterator;
import com.amaze.filemanager.utils.DataStructure.DataIterator.Iterator;

import java.util.ArrayList;

public class BookShelf implements Aggregate {

    private ArrayList<String[]> books;

    public BookShelf() {
        //this.books = DataUtils.getInstance().getBooks() ;
        this.books = new ArrayList<>() ;
    }

    public void addBook(String[] book) {
        books.add(book) ;
    }

    public void removeBook(int index) {
        books.remove(index) ;
    }

    public String[] getBookAt(int index) {
        return books.get(index) ;
    }

    public int getLength() {
        return books.size() ;
    }

    public ArrayList<String[]> getBooks() {
        return books;
    }

    public void setBooks(ArrayList<String[]> books) {
        this.books = books;
    }

    public void clearBooks() {
        books.clear();
    }

    @Override
    public Iterator createIterator() {
        return new BooksIterator(this);
    }
}
