package de.bernhardunger.drools.model;

import java.util.Date;
/**
 * Representation of a result from the rule engine.
 * @author Bernhard
 *
 */
public class RuleResult {
	
	private String result;
	private Date timestamp;
	
	public RuleResult() {}
	
	public RuleResult(String result, Date timestamp) {
		super();
		this.result = result;
		this.timestamp = timestamp;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "RuleResult [result=" + result + ", timestamp=" + timestamp + "]";
	}
	
}
