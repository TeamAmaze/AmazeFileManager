package com.amaze.filemanager.utils;

import android.app.*;
import android.os.*;
import de.keyboardsurfer.android.widget.crouton.*;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class Shortcuts
{public Shortcuts(){}
public void makeS() throws ParserConfigurationException, TransformerException{
String sd=Environment.getExternalStorageDirectory()+"/";
String[] a=new String[]{sd+Environment.DIRECTORY_DCIM,sd+Environment.DIRECTORY_DOWNLOADS,sd+Environment.DIRECTORY_MOVIES,sd+Environment.DIRECTORY_MUSIC,sd+Environment.DIRECTORY_PICTURES};
	File g=new File("/data/data/com.amaze.filemanager/shortcut.xml");
	if(!g.exists()){
	DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance(); 
	DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

// root elements
	Document doc = docBuilder.newDocument();
	Element rootElement = doc.createElement("shortcut");
	doc.appendChild(rootElement);
	for(int i=0;i<a.length;i++){
	Element staff = doc.createElement("path");
	staff.appendChild(doc.createTextNode(a[i]));
	rootElement.appendChild(staff);}
	TransformerFactory transformerFactory = TransformerFactory.newInstance();
	Transformer transformer = transformerFactory.newTransformer(); 
	DOMSource source = new DOMSource(doc); 
	StreamResult result = new StreamResult(g);
	transformer.transform(source, result);
}}
public void addS(File f) throws Exception{
File g=new File("/data/data/com.amaze.filemanager/shortcut.xml");
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
public ArrayList<File> readS() throws FileNotFoundException, IOException, SAXException, ParserConfigurationException{
    ArrayList<File> f=new ArrayList<File>();
	File g=new File("/data/data/com.amaze.filemanager/shortcut.xml");
	DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance(); 
	DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

// root elements
	Document doc = docBuilder.parse(g);
	NodeList nList = doc.getElementsByTagName("path");
	for (int temp = 0; temp < nList.getLength(); temp++) {
		Node nNode = nList.item(temp);
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
		
			f.add(new File(nNode.getTextContent()));
}}return f;
	}
	public boolean isShortcut(File f) throws IOException, ParserConfigurationException, SAXException{
		boolean b=false;
		ArrayList<File> x=readS();
		for(int i=0;i<x.size();i++){
			if(x.get(i)==f){
				b=true;
			}else{b=false;}
		}
		return b;
	}
	public void removeS(File f1,Activity s) throws IOException, SAXException, ParserConfigurationException, TransformerException{
	//	ArrayList<File> f=new ArrayList<File>();
		File g=new File("/data/data/com.amaze.filemanager/shortcut.xml");
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance(); 
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

// root elements
		Document doc = docBuilder.parse(g);
		NodeList List = doc.getElementsByTagName("shortcut");
		Node n=List.item(0);
	
		NodeList nList = n.getChildNodes();
		
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
		
if(nNode.getTextContent().equals(f1.getPath())){
				//Element e=(Element)nNode;
				n.removeChild(nNode);
		
			}}
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer(); 
		DOMSource source = new DOMSource(doc); 
		StreamResult result = new StreamResult(new File("/data/data/com.amaze.filemanager/shortcut.xml"));
		transformer.transform(source, result);
		Crouton.makeText(s,"Successful",Style.INFO).show();
	}
}
