package de.bernhardunger.drools.util;

import java.util.List;

import de.bernhardunger.drools.model.EventComposite;

/**
 * Example of evnt filter inteface.
 * TODO: Filter needs to be completed, i.e. filtering with list of filter expression and regular expressions.
 * 
 * @author Bernhard
 *
 */
public interface EventFilter {
	/**
	 * Filter events by eventDetail string.
	 * If a eventDetail matches the filterExpression the eventComposite will not be added to the result list.
	 * The result list contains only elements that not match the filterExpression.
	 * 
	 * @param List of eventComposites
	 * @return filtered eventDetail list. 
	 * An empty list if all input elements has been filtered out.
	 * An empty list if input list is empty or null.
	 * @throws IllegalArgumentException if filterExpression is null
	 */
	public  List<EventComposite> filterByEventDetails(List<EventComposite> eventComposites, String filterExpression) throws IllegalArgumentException;
	
	/**
	 * Filter singele event by eventDetail string.
	 * If a eventDetail matches the filterExpression the filter return null, otherwise the input EventComposite element.
	 *
	 * @param eventComposite
	 * @param filterExpression
	 * @return
	 * @throws IllegalArgumentException if filterExpression or  eventComposite is null
	 */
	public EventComposite filterByEventDetail(EventComposite eventComposite, String filterExpression) throws IllegalArgumentException;
}
