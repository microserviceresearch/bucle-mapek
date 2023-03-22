package es.upv.pros.microservices.mapek.bpmn;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalChange {
	String microservice;
	String change;
	String modifiedElement;
	String modifiedEvent;
	String updatedEvent;
	
	public String getMicrosercice() {
		return microservice;
	}
	public void setMicroservice(String microservice) {
		this.microservice = microservice;
	}
	public String getChange() {
		return change;
	}
	public void setChange(String change) {
		this.change = change;
	}
	public String getModifiedElement() {
		return modifiedElement;
	}
	public void setModifiedElement(String modifiedElement) {
		this.modifiedElement = modifiedElement;
	}
	
	public String getModifiedEvent() {
		return modifiedEvent;
	}
	public void setModifiedEvent(String modifiedEvent) {
		this.modifiedEvent = modifiedEvent;
	}
	
	public String getUpdatedEvent() {
		return updatedEvent;
	}
	public void setUpdatedEvent(String updatedEvent) {
		this.updatedEvent = updatedEvent;
	}
	
	public static LocalChange parseJSON(String localChange) throws JSONException{
		JSONObject changeJSON=new JSONObject(localChange);
		LocalChange change=new LocalChange();
		change.setChange(changeJSON.getString("change"));
		change.setMicroservice(changeJSON.getString("microserice"));
		change.setModifiedElement(changeJSON.getString("modifiedElement"));
		change.setModifiedEvent(changeJSON.getString("modifiedEvent"));
		change.setUpdatedEvent(changeJSON.getString("updatedEvent"));
		return change;
		
	}
}
