package es.upv.pros.microservices.mapek.phases;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.upv.pros.microservices.mapek.bpmn.LocalChange;

@Component
public class Planning {

	@Autowired
	private Execution execution;
	
	public void run(String featureVector, LocalChange localChange) throws IOException, InterruptedException {
		
		//Save the Feature Vector in a csv file
		
		RandomAccessFile raf = new RandomAccessFile("featureVector.csv","rw");
		
		String line = raf.readLine();		
		FileWriter writer = new FileWriter("FeatureVector.csv");		
		StringBuilder sb = new StringBuilder();
		
		sb.append(line); // Attributes	
		sb.append("\n");		
		sb.append(featureVector); //Feature Vector Codified		
		sb.append("\n");
		
		writer.write(sb.toString());	
		writer.close();
		
		//Execute the R script and the prediction will be in a txt file
		
		Runtime.getRuntime().exec("Rscript.exe predictor.R");
		
		Thread.sleep(1000);
		
		//Read the rule predicted
		
		Integer ruleNumber=1;
		
		try {
			File myObj = new File("microservicePrediction.txt");
		    Scanner myReader = new Scanner(myObj);
		    while (myReader.hasNextLine()) {
		    	String data = myReader.nextLine();
		    	System.out.println(data);
		    	if(data.contains("\"R1\"")) {
		    		ruleNumber = 1;
		    	}
		    	else if(data.equals("\"R2\"")) {
		    		ruleNumber = 2;
		    	}
		    	else if(data.equals("\"R3\"")) {
		    		ruleNumber = 3;
		    	}
		    	else if(data.equals("\"R4\"")) {
		    		ruleNumber = 4;
		    	}
		    	else if(data.equals("\"R5\"")) {
		    		ruleNumber = 5;
		    	}
		    	else if(data.equals("\"R6\"")) {
		    		ruleNumber = 6;
		    	}
		    	else if(data.equals("\"R7\"")) {
		    		ruleNumber = 7;
		    	}
		    	else if(data.equals("\"R8\"")) {
		    		ruleNumber = 8;
		    	}
		    	else if(data.equals("\"R9\"")) {
		    		ruleNumber = 9;
		    	}
		    	else if(data.equals("\"R10\"")) {
		    		ruleNumber = 10;
		    	}
		    	else if(data.equals("\"R11\"")) {
		    		ruleNumber = 11;
		    	}
		    	else if(data.equals("\"R12\"")) {
		    		ruleNumber = 12;
		    	}
		    	else if(data.equals("\"R-1\"")){
		    		ruleNumber = 3;
		    	}
		      }
		    myReader.close();
		} catch (FileNotFoundException e) {
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		}
		System.out.println("Rule predicted: "+ruleNumber);
		execution.run(ruleNumber, localChange);
	}

}
