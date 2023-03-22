package es.upv.pros.microservices.mapek.bpmn;

import java.io.File;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.stereotype.Component;

@Component
public class BPMNManager {
	
	private BpmnModelInstance bigPicture;
	
	public BpmnModelInstance getBigPicture(){
		if(bigPicture==null) this.loadBigPicture();
		return bigPicture;
	}

	private void loadBigPicture(){
			//This method should obtain the big picture through the REST API of the Global Composition Manager 
			BpmnModelInstance model=null;
			File path=new File(System.getProperty("user.dir")+File.separator+"bpmn");
			for(String file:path.list()){
				if(file.contains(".bpmn")){
					model=Bpmn.readModelFromFile(new File(path.getAbsolutePath()+File.separator+file));
				}
			}
			this.bigPicture=model;
	}
}
