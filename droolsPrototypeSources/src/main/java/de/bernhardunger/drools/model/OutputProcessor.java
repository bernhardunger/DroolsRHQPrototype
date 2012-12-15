package de.bernhardunger.drools.model;

import java.util.List;
/**
 * Example for an output processor for handling of rule action results.
 * @author Bernhard Unger
 *
 */
public class OutputProcessor  {
	

	public void processOutput(List<RuleResult> resultList) {
		
		for (RuleResult ruleResult : resultList) {
			System.out.println(ruleResult);
		}
		
	}


}
