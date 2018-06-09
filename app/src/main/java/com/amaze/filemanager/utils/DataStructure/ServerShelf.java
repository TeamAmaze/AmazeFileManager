package com.amaze.filemanager.utils.DataStructure;

import com.amaze.filemanager.utils.DataStructure.DataIterator.Iterator;
import com.amaze.filemanager.utils.DataStructure.DataIterator.ServersIterator;

import java.util.ArrayList;

public class ServerShelf implements Aggregate {

    private ArrayList<String[]> servers;

    public ServerShelf() {
        this.servers = new ArrayList<>() ;
    }

    public void addServer(String[] aServer) {
        servers.add(aServer) ;
    }

    public void removeServer(int index) {
        servers.remove(index) ;
    }

    public String[] getServerAt(int index) {
        return servers.get(index) ;
    }

    public int getLength() {
        return servers.size() ;
    }

    public ArrayList<String[]> getServers() {
        return servers;
    }

    public void setServers(ArrayList<String[]> servers) {
        this.servers = servers;
    }

    public void clearServers() {
        servers.clear();
    }

    @Override
    public Iterator createIterator() {
        return new ServersIterator(this);
    }
}
