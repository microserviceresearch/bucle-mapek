package es.upv.pros.microservices.mapek.events;

public abstract class EventCallback {
	
	public abstract void execute(String event);

}
