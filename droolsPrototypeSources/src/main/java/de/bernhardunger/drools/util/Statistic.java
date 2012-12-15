package de.bernhardunger.drools.util;
/**
 * Statistic entries for test runs.
 * 
 * @author Bernhard Unger
 */
public class Statistic {
	private int step;
	private long noOfEvents;
	private long execTime;
	private double averageCpuLoad;
	private double eventsPerSecond;
	

	
	public Statistic(int step, long noOfEvents, long execTime, double averageCpuLoad) {
		super();
		this.step = step;
		this.noOfEvents = noOfEvents;
		this.execTime = execTime;
		this.averageCpuLoad = averageCpuLoad;
		this.eventsPerSecond =  ((double) noOfEvents  / (double) execTime) * 1000d;
	}
	public long getNoOfEvents() {
		return noOfEvents;
	}
	public void setNoOfEvents(int noOfEvents) {
		this.noOfEvents = noOfEvents;
	}
	public long getExecTime() {
		return execTime;
	}
	public void setExecTime(long execTime) {
		this.execTime = execTime;
	}
	public double getAverageCpuLoad() {
		return averageCpuLoad;
	}
	public void setAverageCpuLoad(double averageCpuLoad) {
		this.averageCpuLoad = averageCpuLoad;
	}
	public int getStep() {
		return step;
	}
	public void setStep(int step) {
		this.step = step;
	}
	public double getEventsPerSecond() {
		return eventsPerSecond;
	}
	
}
