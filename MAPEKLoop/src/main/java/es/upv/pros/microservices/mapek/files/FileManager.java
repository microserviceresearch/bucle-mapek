package es.upv.pros.microservices.mapek.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

@Component
public class FileManager {

	public void saveBPMNFile(String path, String xml) throws FileNotFoundException, UnsupportedEncodingException{
         File fichero=new File(path);
		 PrintWriter writer = new PrintWriter(fichero, "UTF-8");
		 writer.print(xml);
		 writer.close();
	 }
	 
	public String getBPMNFromFile(String path) throws IOException{
		 String xml=new String(Files.readAllBytes(Paths.get(path))); 
		 return xml;
	 }
}
