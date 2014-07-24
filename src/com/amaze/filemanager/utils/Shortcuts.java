package com.amaze.filemanager.utils;

import android.app.Activity;
import android.os.Environment;

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

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class Shortcuts {
    public Shortcuts() {
    }

    public void makeS() throws ParserConfigurationException, TransformerException {
        String sd = Environment.getExternalStorageDirectory() + "/";
        String[] a = new String[]{sd + Environment.DIRECTORY_DCIM, sd + Environment.DIRECTORY_DOWNLOADS, sd + Environment.DIRECTORY_MOVIES, sd + Environment.DIRECTORY_MUSIC, sd + Environment.DIRECTORY_PICTURES};
        File g = new File("/data/data/com.amaze.filemanager/shortcut.xml");
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
        File g = new File("/data/data/com.amaze.filemanager/shortcut.xml");
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
        StreamResult result = new StreamResult(new File("/data/data/com.amaze.filemanager/shortcut.xml"));
        transformer.transform(source, result);

    }

    public ArrayList<File> readS() throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
        ArrayList<File> f = new ArrayList<File>();
        File g = new File("/data/data/com.amaze.filemanager/shortcut.xml");
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

    public void removeS(File f1, Activity s) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        //	ArrayList<File> f=new ArrayList<File>();
        File g = new File("/data/data/com.amaze.filemanager/shortcut.xml");
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
        StreamResult result = new StreamResult(new File("/data/data/com.amaze.filemanager/shortcut.xml"));
        transformer.transform(source, result);
        Crouton.makeText(s, "Successful", Style.INFO).show();
    }
}
