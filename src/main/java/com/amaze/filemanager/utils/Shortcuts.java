/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

package com.amaze.filemanager.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class Shortcuts {
    Context context;
    String path;
    public Shortcuts(Context context) {
        this.context=context;
        path=context.getFilesDir()+"/shortcut.xml";
    }

    public void makeS() throws ParserConfigurationException, TransformerException {
        String sd = Environment.getExternalStorageDirectory() + "/";
        String[] a = new String[]{sd + Environment.DIRECTORY_DCIM, sd + Environment.DIRECTORY_DOWNLOADS, sd + Environment.DIRECTORY_MOVIES, sd + Environment.DIRECTORY_MUSIC, sd + Environment.DIRECTORY_PICTURES};
        File g = new File(path);
        if (!g.exists()) {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

// root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("shortcut");
            doc.appendChild(rootElement);
            for (int i = 0; i < a.length; i++) {
                Element staff = doc.createElement("path");
                staff.appendChild(doc.createTextNode(a[i]));
                rootElement.appendChild(staff);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(g);
            transformer.transform(source, result);
        }
    }

    public void addS(File f) throws Exception {
        File g = new File(path);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

// root elements
        Document doc = docBuilder.parse(g);
        NodeList nList = doc.getElementsByTagName("shortcut");
        Node nNode = nList.item(0);
        Element eElement = (Element) nNode;


// f elements
        Element staff = doc.createElement("path");
        staff.appendChild(doc.createTextNode(f.getPath()));
        eElement.appendChild(staff);


        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(path));
        transformer.transform(source, result);

    }

    public ArrayList<File> readS() throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
        ArrayList<File> f = new ArrayList<File>();
        File g = new File(path);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

// root elements
        Document doc = docBuilder.parse(g);
        NodeList nList = doc.getElementsByTagName("path");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                f.add(new File(nNode.getTextContent()));
            }
        }
        return f;
    }

    public boolean isShortcut(File f) throws IOException, ParserConfigurationException, SAXException {
        boolean b = false;
        ArrayList<File> x = readS();
        for (int i = 0; i < x.size(); i++) {
            if (x.get(i) == f) {
                b = true;
            } else {
                b = false;
            }
        }
        return b;
    }

    public void removeS(File f1, Context s) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        //	ArrayList<File> f=new ArrayList<File>();
        File g = new File(path);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

// root elements
        Document doc = docBuilder.parse(g);
        NodeList List = doc.getElementsByTagName("shortcut");
        Node n = List.item(0);

        NodeList nList = n.getChildNodes();

        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);

            if (nNode.getTextContent().equals(f1.getPath())) {
                //Element e=(Element)nNode;
                n.removeChild(nNode);

            }
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(path));
        transformer.transform(source, result);
    }
}
