package com.amaze.filemanager.utils;

import android.app.Activity;
import android.content.Context;
import android.support.v7.view.menu.ActionMenuItem;
import android.view.MenuItem;

import com.amaze.filemanager.ui.views.drawer.MenuMetadata;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Box;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.Microsoft;
import com.cloudrail.si.services.OneDrive;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.fakes.RoboMenuItem;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by kille on 2018-03-30.
 */


public class DataUtilsTest {

    private DataUtils dataUtils ;
    private Context context ;
    private Activity activity ;

    @Before
    public void setUp() throws Exception {
        dataUtils = DataUtils.getInstance() ;
        context = new Activity() ;

        assertNotNull(context);
    }

    @After
    public void tearDown() throws Exception {
        dataUtils.clear();
    }

    /* Test Generate Instance */
    @Test
    public void testGetInstance() {
        assertNotNull(dataUtils) ;
    }

    @Test
    public void testClear() {
        dataUtils.clear();
        assertTrue(dataUtils.getBooks().isEmpty()) ;
        assertTrue(dataUtils.getServers().isEmpty()) ;
        assertTrue(dataUtils.getAccounts().isEmpty()) ;
        assertTrue(dataUtils.getStorages().isEmpty()) ;
        assertTrue(dataUtils.getHistory().isEmpty()) ;
        assertEquals(0, dataUtils.getHiddenFiles().size()) ;
    }


    /* Books */
    /* Test to add element to array for books */
    @Test
    public void testAddBook() {
        String[] books = {"nameTest", "pathTest" } ;
        dataUtils.addBook(books) ;
        assertTrue(dataUtils.getBooks().contains(books));
    }

    @Test
    public void testAddBookRefresh() {
        String[] books = {"nameTest", "pathTest" } ;
        dataUtils.addBook(books, true);
        assertTrue(dataUtils.getBooks().contains(books));

        String[] anotherBooks = {"nameTest1", "pathTest1" } ;
        dataUtils.addBook(anotherBooks, false);
        assertTrue(dataUtils.getBooks().contains(anotherBooks));
    }

    /* Test Remove Book
     * @testRemoveBook() : Normal
     * @testRemoveBookAtOverIndex() : At over index
     * @testRemoveBookAtNegativeIndex() : At negative index */
    @Test
    public void testRemoveBook() {
        String[] books = {"nameTest", "pathTest" } ;
        dataUtils.addBook(books);
        int targetIndex = dataUtils.getBooks().indexOf(books) ;
        dataUtils.removeBook(targetIndex);

        assertFalse(dataUtils.getBooks().contains(books));
    }

    @Test
    public void testRemoveBookAtOverIndex() {
        String[] books = {"nameTest", "pathTest" } ;
        dataUtils.addBook(books);

        int currentBookSize = dataUtils.getBooks().size() ;
        int overIndex = currentBookSize + 1 ;
        dataUtils.removeBook(overIndex);

        assertEquals(currentBookSize, dataUtils.getBooks().size()) ;
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveBookAtNegativeIndex() throws Exception {
        String[] books = {"nameTest", "pathTest" } ;
        dataUtils.addBook(books);

        int negativeIndex = -1 ;
        dataUtils.removeBook(negativeIndex);
    }

    @Test
    public void testSortBook() {
        String[] book1 = {"abc", "pathTest" } ;
        String[] book2 = {"def", "pathTest" } ;
        String[] book3 = {"abc", "a_pathTest" } ;
        dataUtils.addBook(book1);
        dataUtils.addBook(book2);
        dataUtils.addBook(book3);
        dataUtils.sortBook();

        assertSame(book3, dataUtils.getBooks().get(0));
        assertSame(book1, dataUtils.getBooks().get(1));
        assertSame(book2, dataUtils.getBooks().get(2));
    }

    @Test
    public void testContainsBooksForStringArrParameter() {
        String[] books = {"bookName", "bookPath" } ;
        String[] fakeBooks = {"fakeName", "fakePath"} ;

        int bias = 3 ;
        for (int i = 0 ; i < bias ; i ++) {
            dataUtils.addBook(fakeBooks) ;
        }
        dataUtils.addBook(books);

        assertEquals(bias, dataUtils.containsBooks(books)); // get books at the bias position
    }


    /* Servers */
    /* Test add elements to array for server */
    @Test
    public void testAddServerAboutSize() {
        String[] servers = {"serverName", "serverPath"} ;
        dataUtils.addServer(servers); ;
        assertTrue(dataUtils.getServers().contains(servers));
    }

    /* Test Remove Server
     * @testRemoveServer() : Normal
     * @testRemoveServerAtOverIndex() : At over index
     * @testRemoveServerAtNegativeIndex() : At negative index */
    @Test
    public void testRemoveServer() {
        String[] servers = {"serverName", "serverPath"} ;
        dataUtils.addServer(servers);
        int targetIndex = dataUtils.getServers().indexOf(servers) ;
        dataUtils.removeServer(targetIndex);

        assertFalse(dataUtils.getServers().contains(servers));
    }

    @Test
    public void testRemoveServerAtOverIndex() {
        String[] servers = {"serverName", "serverPath"} ;
        dataUtils.addServer(servers);

        int currentServerSize = dataUtils.getServers().size() ;
        int overIndex = currentServerSize + 1 ;
        dataUtils.removeServer(overIndex);

        assertEquals(currentServerSize, dataUtils.getServers().size()) ;
    }

    @Test
    public void testRemoveServerAtNegativeIndex() {
        String[] servers = {"serverName", "serverPath"} ;
        dataUtils.addServer(servers);

        int currentServerSize = dataUtils.getServers().size() ;
        int overIndex = currentServerSize + 1 ;
        dataUtils.removeServer(overIndex);

        assertEquals(currentServerSize, dataUtils.getServers().size()) ;
    }

    @Test
    public void testContainsServerForStringArrParameter() {
        String[] servers = {"serverName", "serverPath"} ;
        String[] fakeServers = {"fakeServer", "fakePath"} ;

        int bias = 3 ;
        for (int i = 0 ; i < bias ; i ++) {
            dataUtils.addServer(fakeServers);
        }
        dataUtils.addServer(servers);

        assertEquals(bias, dataUtils.containsServer(servers));
    }

    @Test
    public void testContainsServerForStringParameter() {
        String serverPath = "serverPath" ;
        String[] servers = {"serverName", "serverPath"} ; // servers.path == serverPath
        String[] fakeServers = {"serverName", "fakePath"} ; // fakeServers.path != serverPath

        int bias = 2 ;
        for (int i = 0 ; i < bias ; i ++) {
            dataUtils.addServer(fakeServers);
        }
        dataUtils.addServer(servers);

        assertEquals(bias, dataUtils.containsServer(serverPath));
    }

    @Test
    public void testContainsServerForNoOperate() {
        String serverPath = "serverPath" ;
        String[] fakeServers = {"serverName", "fakePath"} ; // fakeServers.path != serverPath

        int bias = 4 ;
        for (int i = 0 ; i < bias ; i ++) {
            dataUtils.addServer(fakeServers);
        }
        // There are no elements which path equals serverPath
        int error = -1 ;
        assertEquals(error, dataUtils.containsServer(serverPath));
    }


    /* Accounts */
    /* Test Add Accounts */
    @Test
    public void testAddAccount() {
        CloudStorage testStorage = new GoogleDrive(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage) ;
        assertTrue(dataUtils.getAccounts().contains(testStorage));
    }

    /* Test Get Accounts */
    @Test
    public void testGetAccountForGoogleDrive() {
        CloudStorage testStorage = new GoogleDrive(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage) ;

        assertSame(testStorage, dataUtils.getAccount(OpenMode.GDRIVE));
        assertNull(dataUtils.getAccount(OpenMode.BOX));
        assertNull(dataUtils.getAccount(OpenMode.ONEDRIVE));
        assertNull(dataUtils.getAccount(OpenMode.DROPBOX));
    }

    @Test
    public void testGetAccountForDropBox() {
        CloudStorage testStorage = new Dropbox(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage) ;

        assertSame(testStorage, dataUtils.getAccount(OpenMode.DROPBOX));
        assertNull(dataUtils.getAccount(OpenMode.BOX));
        assertNull(dataUtils.getAccount(OpenMode.ONEDRIVE));
        assertNull(dataUtils.getAccount(OpenMode.GDRIVE));
    }

    @Test
    public void testGetAccountForBox() {
        CloudStorage testStorage = new Box(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage) ;

        assertSame(testStorage, dataUtils.getAccount(OpenMode.BOX));
        assertNull(dataUtils.getAccount(OpenMode.ONEDRIVE));
        assertNull(dataUtils.getAccount(OpenMode.DROPBOX));
        assertNull(dataUtils.getAccount(OpenMode.GDRIVE));
    }

    @Test
    public void testGetAccountForOneDrive() {
        CloudStorage testStorage = new OneDrive(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage) ;

        assertSame(testStorage, dataUtils.getAccount(OpenMode.ONEDRIVE));
        assertNull(dataUtils.getAccount(OpenMode.BOX));
        assertNull(dataUtils.getAccount(OpenMode.DROPBOX));
        assertNull(dataUtils.getAccount(OpenMode.GDRIVE));
    }

    @Test
    public void testGetAccountForWrongAccount() {
        CloudStorage testStorage = new Microsoft(context, "testID", "testSecret") ; // Microsoft is unsupported
        dataUtils.addAccount(testStorage) ;

        assertNull(dataUtils.getAccount(OpenMode.BOX));
        assertNull(dataUtils.getAccount(OpenMode.DROPBOX));
        assertNull(dataUtils.getAccount(OpenMode.GDRIVE));
        assertNull(dataUtils.getAccount(OpenMode.ONEDRIVE));
    }

    @Test
    public void testGetAccountForInvalidServiceType() {
        CloudStorage testStorage = new OneDrive(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage);

        assertNull(dataUtils.getAccount(OpenMode.UNKNOWN));
    }

    @Test
    public void testGetAccountAboutNoAccount() {
        dataUtils.getAccounts().clear();
        assertNull(dataUtils.getAccount(OpenMode.GDRIVE));
        assertNull(dataUtils.getAccount(OpenMode.DROPBOX));
        assertNull(dataUtils.getAccount(OpenMode.BOX));
        assertNull(dataUtils.getAccount(OpenMode.ONEDRIVE));
    }

    /* Test Remove Account
     * */
    @Test
    public void testRemoveAccountForGoogleDrive() {
        CloudStorage testStorage = new GoogleDrive(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage) ;
        dataUtils.removeAccount(OpenMode.GDRIVE);

        assertFalse(dataUtils.getAccounts().contains(testStorage)); ;
    }

    @Test
    public void testRemoveAccountForDropBox() {
        CloudStorage testStorage = new Dropbox(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage) ;
        dataUtils.removeAccount(OpenMode.DROPBOX);

        assertFalse(dataUtils.getAccounts().contains(testStorage)); ;
    }

    @Test
    public void testRemoveAccountForBox() {
        CloudStorage testStorage = new Box(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage) ;
        dataUtils.removeAccount(OpenMode.BOX);

        assertFalse(dataUtils.getAccounts().contains(testStorage)); ;
    }

    @Test
    public void testRemoveAccountForOneDrive() {
        CloudStorage testStorage = new OneDrive(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage) ;
        dataUtils.removeAccount(OpenMode.ONEDRIVE);

        assertFalse(dataUtils.getAccounts().contains(testStorage)); ;
    }

    @Test
    public void testRemoveAccountForWrongAccount() {
        CloudStorage testStorage = new GoogleDrive(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage) ; // Add google drive account
        dataUtils.removeAccount(OpenMode.DROPBOX); //remove dropbox account

        assertTrue(dataUtils.getAccounts().contains(testStorage)); // must be remained original account
    }

    @Test
    public void testRemoveAccountForInvalidServiceType() {
        CloudStorage testStorage = new GoogleDrive(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage) ; // Add google drive account
        dataUtils.removeAccount(OpenMode.UNKNOWN); // remove invalid account

        assertTrue(dataUtils.getAccounts().contains(testStorage)); // must be remained original account
    }

    @Test
    public void testContainsAccountsForOpenModeParameter() {
        CloudStorage dropBoxStorage = new Dropbox(context, "testID", "testSecret") ;
        dataUtils.addAccount(dropBoxStorage); // index = 0 ;
        CloudStorage googleDriveStorage = new GoogleDrive(context, "testID", "testSecret") ;
        dataUtils.addAccount(googleDriveStorage); // index = 1
        CloudStorage boxStorage = new Box(context, "testID", "testSecret") ;
        dataUtils.addAccount(boxStorage); // index = 2
        CloudStorage oneDriveStorage = new OneDrive(context, "testID", "testSecret") ;
        dataUtils.addAccount(oneDriveStorage); // index = 3

        assertEquals(0, dataUtils.containsAccounts(OpenMode.DROPBOX));
        assertEquals(1, dataUtils.containsAccounts(OpenMode.GDRIVE));
        assertEquals(2, dataUtils.containsAccounts(OpenMode.BOX));
        assertEquals(3, dataUtils.containsAccounts(OpenMode.ONEDRIVE));
    }

    @Test
    public void testContainsAccountsForWrongAccount() {
        CloudStorage testStorage = new GoogleDrive(context, "testID", "testSecret") ;
        dataUtils.addAccount(testStorage);

        assertEquals(-1, dataUtils.containsAccounts(OpenMode.DROPBOX)); // Wrong Account
        assertEquals(-1, dataUtils.containsAccounts(OpenMode.UNKNOWN)); // check default
    }


    /* Hidden files */
    /* Test add hidden files */
    @Test
    public void testAddHiddenFile() {
        int currentSize = dataUtils.getHiddenFiles().size() ;
        String randomKey = "i_test1234" ;
        dataUtils.addHiddenFile(randomKey);

        assertEquals(currentSize + 1, dataUtils.getHiddenFiles().size()) ;
        assertNotNull(dataUtils.getHiddenFiles().getValueForExactKey(randomKey));
    }

    @Test
    public void testRemoveHiddenFile() {
        String randomKey = "i_testR223344" ;
        dataUtils.addHiddenFile(randomKey) ;
        dataUtils.removeHiddenFile(randomKey);

        assertNull(dataUtils.getHiddenFiles().getValueForExactKey(randomKey));
    }

    @Test
    public void testIsFileHiddenForHidden() {
        String path = "testPath" ;
        dataUtils.addHiddenFile(path);

        assertTrue(dataUtils.isFileHidden(path));
    }

    @Test
    public void testIsFileHiddenForNotHidden() {
        String path = "testPath" ;
        String fakePath = "fakePath" ;
        dataUtils.addHiddenFile(path);

        assertFalse(dataUtils.isFileHidden(fakePath));
    }


    /* History files */
    /* Test add history files */
    @Test
    public void testAddHistoryFiles() {
        String testHistory ="test_history" ;
        dataUtils.addHistoryFile(testHistory);

        assertTrue(dataUtils.getHistory().contains(testHistory)) ;
    }

    @Test
    public void testClearHistory() {
        String testHistory ="test_history" ;
        dataUtils.addHistoryFile(testHistory);
        dataUtils.clearHistory();

        assertTrue(dataUtils.getHistory().isEmpty()) ;
    }

    /* Menu Metadata */
    @Test
    public void testPutDrawerMetadata() {
        MenuItem menuItem = new ActionMenuItem(context,0,123,1,1,"testMenu") ;

        MenuMetadata menuMetadata = new MenuMetadata("testPath");
        dataUtils.putDrawerMetadata(menuItem, menuMetadata);

        assertSame(menuMetadata, dataUtils.getDrawerMetadata(menuItem));
    }

    @Test
    public void testFindLongestContainingDrawerItem() {
        int menuItem1_id = 167981 ;
        MenuItem menuItem1 = new ActionMenuItem(context,0, menuItem1_id,1,1,"menuItem1") ;
        String metaDataPath = "testPath" ;
        MenuMetadata menuMetadata = new MenuMetadata(metaDataPath);
        dataUtils.putDrawerMetadata(menuItem1, menuMetadata);

//        int menuItem2_id = 2 ;
//        MenuItem menuItem2 = new ActionMenuItem(context,0, menuItem2_id,1,1,"menuItem2") ;
//        String metaDataPath2 = "testPathsefi" ;
//        MenuMetadata menuMetadata2 = new MenuMetadata(metaDataPath2) ;
//        dataUtils.putDrawerMetadata(menuItem2, menuMetadata2);

        assertEquals(menuItem1_id, dataUtils.findLongestContainingDrawerItem(metaDataPath).intValue());

    }

    @Test
    public void testFindLongestContainingDrawerItemForFakePath() {
        MenuItem menuItem = new RoboMenuItem() ;
        String metaDataPath = "testPath" ;
        String fakePath = "fakePath" ;
        MenuMetadata menuMetadata = new MenuMetadata(metaDataPath);
        dataUtils.putDrawerMetadata(menuItem, menuMetadata);

        assertNull(dataUtils.findLongestContainingDrawerItem(fakePath));
    }


    /* Test Contains method which param is String */
    @Test // Not Used..
    public void testContainsForString() {
        String a = "first" ; // String 'a' will exists in the element of ArrayList.
        ArrayList<String[]> b = new ArrayList<>() ;
        String[] strings = {"zero", "first", "second"} ; // String 'a' must be the second element of strings.
        b.add(strings) ;
        assertEquals(0, dataUtils.contains(a, b));
    }

    /* Test Contains method if it doesn't Operates */
    @Test // Not Used..
    public void testContainsForStringNoOperate() {
        String a = null ;
        ArrayList<String[]> b = new ArrayList<>() ;
        String[] strings = {"zero", "first"} ;
        b.add(strings) ;
        assertEquals(-1, dataUtils.contains(a, b) );
    }

    /* Test Contains method which param is String Array */
    @Test
    public void testContainsForStringArr() {
        String a[] = {"zero", "first"} ;
        ArrayList<String[]> b = new ArrayList<>() ;
        String[] strings = {"zero", "first"} ;
        b.add(strings) ;
        assertEquals(0, dataUtils.contains(a, b));
        b.clear();

        String[] biasStrings = {"aa", "bb"} ;
        int biasSize = 10 ;
        for (int i = 0 ; i < biasSize ; i ++) {
            b.add(biasStrings) ;
        }
        b.add(strings) ;
        assertEquals(biasSize, dataUtils.contains(a, b));
    }

    /* Test Contains method if it doesn't Operates */
    @Test
    public void testContainsForStringArrNoOperate() {
        // case : b == null
        String[] a = {"zero", "first"} ;
        ArrayList<String[]> b = null ;
        assertEquals(-1, dataUtils.contains(a, b));

        // case : b has 3 elements.
        b = new ArrayList<>() ;
        String[] strings1 = {"zero", "zero"} ;
        String[] strings2 = {"first", "first"} ;
        String[] strings3 = {"first", "zero"} ;
        b.add(strings1);
        b.add(strings2);
        b.add(strings3);
        assertEquals(-1, dataUtils.contains(a, b));
    }
}