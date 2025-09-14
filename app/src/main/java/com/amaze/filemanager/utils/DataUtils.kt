/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.amaze.filemanager.utils

import android.content.Context
import android.text.TextUtils
import android.view.MenuItem
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.models.LanguageModel
import com.amaze.filemanager.utils.omh.OmhCredentialsWrapper
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import com.googlecode.concurrenttrees.radix.node.concrete.voidvalue.VoidValue
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.LinkedList

/** Singleton class to handle data for various services  */
// Central data being used across activity,fragments and classes
object DataUtils {
    @JvmStatic
    private val LOG: Logger = LoggerFactory.getLogger(DataUtils::class.java)

    const val LIST: Int = 0
    const val GRID: Int = 1

    private var hiddenfiles = ConcurrentRadixTree<VoidValue?>(DefaultCharArrayNodeFactory())

    private var filesGridOrList: InvertedRadixTree<Int> =
        ConcurrentInvertedRadixTree(DefaultCharArrayNodeFactory())

    private val history = LinkedList<String>()

    private var tree: InvertedRadixTree<Int> =
        ConcurrentInvertedRadixTree(DefaultCharArrayNodeFactory())

    private var servers: ArrayList<Array<String>> = ArrayList()
    private var books = ArrayList<Array<String>>()

    private var _accounts: MutableList<OmhCredentialsWrapper> = mutableListOf()
    val accounts: List<OmhCredentialsWrapper>
        @Synchronized get() = _accounts

    /** List of checked items to persist when drag and drop from one tab to another  */
    var checkedItemsList: ArrayList<LayoutElementParcelable>? = null

    private var dataChangeListener: DataChangeListener? = null

    /**
     * Returns list of available languages.
     */
    @JvmStatic
    @Suppress("LongMethod")
    fun getLanguages(context: Context): List<LanguageModel> =
        arrayListOf(
            LanguageModel(
                context.getString(R.string.german_translation_title),
                context.getString(R.string.german_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.spanish_translation_title),
                context.getString(R.string.spanish_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.basque_translation_title),
                context.getString(R.string.basque_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.chinese_translation_title),
                context.getString(R.string.chinese_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.serbian_translation_title),
                context.getString(R.string.serbian_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.turkish_translation_title),
                context.getString(R.string.turkish_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.ukrainian_translation_title),
                context.getString(R.string.ukrainian_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.portuguese_translation_title),
                context.getString(R.string.portuguese_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.polish_translation_title),
                context.getString(R.string.polish_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.korean_translation_title),
                context.getString(R.string.korean_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.greek_translation_title),
                context.getString(R.string.greek_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.dutch_translation_title),
                context.getString(R.string.dutch_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.romanian_translation_title),
                context.getString(R.string.romanian_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.vietnamese_translation_title),
                context.getString(R.string.vietnamese_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.japanese_translation_title),
                context.getString(R.string.japanese_translation_summary),
            ),
            LanguageModel(
                context.getString(R.string.tamil_translation_title),
                context.getString(R.string.tamil_translation_summary),
            ),
        )

    /**
     * Returns list of contributors.
     */
    @JvmStatic
    fun getContributors(context: Context): List<LanguageModel> =
        arrayListOf(
            LanguageModel(
                context.getString(R.string.contributor_1_title),
                context.getString(R.string.contributors_1_summary),
            ),
            LanguageModel(
                context.getString(R.string.contributor_2_title),
                context.getString(R.string.contributors_2_summary),
            ),
            LanguageModel(
                context.getString(R.string.contributor_3_title),
                context.getString(R.string.contributors_3_summary),
            ),
            LanguageModel(
                context.getString(R.string.contributor_4_title),
                context.getString(R.string.contributors_4_summary),
            ),
            LanguageModel(
                context.getString(R.string.contributor_5_title),
                context.getString(R.string.contributors_5_summary),
            ),
            LanguageModel(
                context.getString(R.string.contributor_6_title),
                context.getString(R.string.contributors_6_summary),
            ),
            LanguageModel(
                context.getString(R.string.contributor_7_title),
                context.getString(R.string.contributors_7_summary),
            ),
        )

    /**
     * Check if server path(s) exists
     */
    fun containsServer(a: Array<String>): Int {
        return contains(a, servers)
    }

    /**
     * Check if server path exists
     */
    fun containsServer(path: String): Int {
        synchronized(servers) {
            return servers.indexOfFirst {
                it[1] == path
            }
        }
    }

    /**
     * Check if bookmark exists
     */
    fun containsBooks(a: Array<String>): Int {
        return contains(a, books)
    }

    /**
     * Clear all data.
     */
    fun clear() {
        hiddenfiles = ConcurrentRadixTree(DefaultCharArrayNodeFactory())
        filesGridOrList = ConcurrentInvertedRadixTree(DefaultCharArrayNodeFactory())
        history.clear()
        storages = ArrayList()
        tree = ConcurrentInvertedRadixTree(DefaultCharArrayNodeFactory())
        servers = ArrayList()
        books = ArrayList()
        _accounts = mutableListOf()
    }

    /**
     * Register a DataChangeListener, then clear all data.
     */
    fun registerOnDataChangedListener(l: DataChangeListener?) {
        dataChangeListener = l
        clear()
    }

    private fun contains(
        a: String,
        b: ArrayList<Array<String>>,
    ): Int {
        for ((i, x) in b.withIndex()) {
            if (x[1] == a) return i
        }
        return -1
    }

    private fun contains(
        a: Array<String>,
        b: ArrayList<Array<String>>?,
    ): Int {
        if (b == null) return -1
        for ((i, x) in b.withIndex()) {
            if (x[0] == a[0] && x[1] == a[1]) return i
        }
        return -1
    }

    /**
     * Remove bookmark at given index.
     */
    fun removeBook(i: Int) {
        synchronized(books) {
            if (books.size > i) books.removeAt(i)
        }
    }

    /**
     * Remove account for given service type.
     */
    fun removeAccount(serviceType: OpenMode) {
        accounts.let {
            val newValue = it.toMutableList()
            newValue.removeAll { entry: OmhCredentialsWrapper ->
                entry.openMode == serviceType
            }
        }
    }

    /**
     * Remove server path at given index.
     */
    fun removeServer(i: Int) {
        synchronized(servers) {
            if (servers.size > i) servers.removeAt(i)
        }
    }

    /**
     * Add bookmark if not exist.
     */
    fun addBook(i: Array<String>) {
        if (containsBooks(i) != -1) {
            return
        }
        synchronized(books) {
            books.add(i)
        }
    }

    /**
     * @param i The bookmark name and path.
     * @param refreshdrawer boolean flag to indicate if drawer refresh is desired.
     * @return True if operation successful, false if failure.
     */
    fun addBook(
        i: Array<String>,
        refreshdrawer: Boolean,
    ): Boolean {
        if (containsBooks(i) != -1) {
            // book exists
            return false
        } else {
            synchronized(books) {
                books.add(i)
            }

            if (dataChangeListener != null) {
                dataChangeListener!!.onBookAdded(i, refreshdrawer)
            }

            return true
        }
    }

    /**
     * Add account if not exist.
     */
    fun addAccount(account: OmhCredentialsWrapper) {
        _accounts.let {
            if (!it.contains(account)) {
                it.add(account)
            }
        }
    }

    /**
     * Add server path.
     */
    fun addServer(i: Array<String>) {
        synchronized(servers) {
            servers.add(i)
        }
    }

    /**
     * Add hidden file path.
     */
    fun addHiddenFile(i: String) {
        synchronized(hiddenfiles) {
            hiddenfiles.put(i, VoidValue.SINGLETON)
        }
        if (dataChangeListener != null) {
            dataChangeListener!!.onHiddenFileAdded(i)
        }
    }

    /**
     * Remove hidden file path.
     */
    fun removeHiddenFile(i: String) {
        synchronized(hiddenfiles) {
            hiddenfiles.remove(i)
        }
        if (dataChangeListener != null) {
            dataChangeListener!!.onHiddenFileRemoved(i)
        }
    }

    /**
     * Set history collection.
     */
    fun setHistory(s: LinkedList<String>?) {
        history.clear()
        history.addAll(s!!)
    }

    /**
     * Get history collection.
     */
    fun getHistory(): LinkedList<String> {
        return history
    }

    /**
     * Add path to history.
     */
    fun addHistoryFile(i: String) {
        history.push(i)
        if (dataChangeListener != null) {
            dataChangeListener!!.onHistoryAdded(i)
        }
    }

    /**
     * Sort bookmarks alphabetically.
     */
    fun sortBook() {
        Collections.sort(books, BookSorter())
    }

    /**
     * Set servers collection.
     */
    @Synchronized
    fun setServers(servers: ArrayList<Array<String>>?) {
        if (servers != null) this.servers = servers
    }

    /**
     * Set bookmarks collection.
     */
    @Synchronized
    fun setBooks(books: ArrayList<Array<String>>?) {
        if (books != null) this.books = books
    }

    /**
     * Get servers collection.
     */
    @Synchronized
    fun getServers(): ArrayList<Array<String>> {
        return servers
    }

    /**
     * Get bookmarks collection.
     */
    @Synchronized
    fun getBooks(): ArrayList<Array<String>> {
        return books
    }

    /**
     * Get account for given service type.
     */
    @Synchronized
    fun getAccount(serviceType: OpenMode): OmhCredentialsWrapper? {
        val retval = accounts.find { it.openMode == serviceType }
        if (retval == null) {
            LOG.error("Unable to determine service type of cloudEntry {}", serviceType)
        }
        return retval
    }

    /**
     * Check if file is hidden.
     */
    fun isFileHidden(path: String): Boolean {
        try {
            return hiddenFiles?.getValueForExactKey(path) != null
        } catch (e: IllegalStateException) {
            LOG.warn("failed to get hidden file", e)
            return false
        }
    }

    @set:Synchronized
    var hiddenFiles: ConcurrentRadixTree<VoidValue?>?
        get() = hiddenfiles
        set(hiddenfiles) {
            if (hiddenfiles != null) this.hiddenfiles = hiddenfiles
        }

    @set:Synchronized
    var gridfiles: ArrayList<String?>?
        get() = gridfiles
        set(gridfiles) {
            if (gridfiles != null) {
                for (gridfile in gridfiles) {
                    setPathAsGridOrList(gridfile, GRID)
                }
            }
        }

    /**
     * Set list of paths that should be displayed in list mode.
     */
    @Synchronized
    fun setListfiles(listfiles: ArrayList<String?>?) {
        if (listfiles != null) {
            for (gridfile in listfiles) {
                setPathAsGridOrList(gridfile, LIST)
            }
        }
    }

    /**
     * Set path to be displayed in grid or list mode.
     */
    fun setPathAsGridOrList(
        path: String?,
        value: Int,
    ) {
        filesGridOrList.put(path, value)
    }

    /**
     * Get if path should be displayed in grid or list mode.
     *
     * @param path the path to find
     * @param defaultValue the default value to return if no value found
     * @return the value for the longest prefix matching path or defaultValue if none found
     */
    fun getListOrGridForPath(
        path: String?,
        defaultValue: Int,
    ): Int {
        val value = filesGridOrList.getValueForLongestKeyPrefixing(path)
        return value ?: defaultValue
    }

    /**
     * Clear history collection. Also notifies listener in background thread.
     */
    fun clearHistory() {
        history.clear()
        AppConfig.getInstance().runInBackground { dataChangeListener?.onHistoryCleared() }
    }

    @set:Synchronized
    var storages: ArrayList<String> = ArrayList()

    /**
     * Put drawer path with its menu id.
     */
    fun putDrawerPath(
        item: MenuItem,
        path: String?,
    ): Boolean {
        if (!TextUtils.isEmpty(path)) {
            try {
                tree.put(path, item.itemId)
                return true
            } catch (e: IllegalStateException) {
                LOG.warn("failed to put drawer path", e)
                return false
            }
        }
        return false
    }

    /**
     * @param path the path to find
     * @return the id of the longest containing MenuMetadata.path in getDrawerMetadata() or null
     */
    fun findLongestContainingDrawerItem(path: CharSequence?): Int? {
        return tree.getValueForLongestKeyPrefixing(path)
    }

    /**
     * Callbacks to do original changes in database (and ui if required) The callbacks are called in a
     * main thread
     */
    interface DataChangeListener {
        /** Called when a hidden file is added */
        fun onHiddenFileAdded(path: String)

        /** Called when a hidden file is removed */
        fun onHiddenFileRemoved(path: String)

        /** Called when a history entry is added */
        fun onHistoryAdded(path: String)

        /**
         * Called when a bookmark is added
         *
         * @param path The bookmark name and path.
         * @param refreshDrawer boolean flag to indicate if drawer refresh is desired.
         */
        fun onBookAdded(
            path: Array<String>,
            refreshDrawer: Boolean,
        )

        /** Called when history is cleared */
        fun onHistoryCleared()
    }
}
