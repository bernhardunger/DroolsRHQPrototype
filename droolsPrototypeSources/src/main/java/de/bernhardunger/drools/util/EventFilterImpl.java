package de.bernhardunger.drools.util;

import java.util.ArrayList;
import java.util.List;

import de.bernhardunger.drools.model.EventComposite;
/**
 * Example of a EventFilter implementation.
 * @author Bernhard Unger
 *
 */
public class EventFilterImpl implements EventFilter {

	
	/**
	 * @see EventFilter
	 */
	@Override
	public List<EventComposite> filterByEventDetails(List<EventComposite> eventComposites, String filterExpression) throws IllegalArgumentException {
		if ( null == filterExpression ) {
			throw new IllegalArgumentException("filterExpression must not be null");
		}
		if ( null == eventComposites ) {
			return new ArrayList<EventComposite>();
		}
		
		List<EventComposite> filteredEvents = new ArrayList<EventComposite>();
		for (EventComposite event : eventComposites ) {
			if ( ! event.getEventDetail().equalsIgnoreCase(filterExpression) ) {
				filteredEvents.add(event);
			}
		}
		return filteredEvents;
	}
	
	/**
	 * @see EventFilter
	 */
	@Override
	public EventComposite filterByEventDetail(EventComposite eventComposite, String filterExpression)
			throws IllegalArgumentException {
		
		if ( null == filterExpression ) {
			throw new IllegalArgumentException("filterExpression must not be null");
		}
		if ( null == eventComposite ) {
			throw new IllegalArgumentException("eventComposite must not be null");
		}
		if ( eventComposite.getEventDetail().equalsIgnoreCase(filterExpression) ) {
			return null;
		}
		return eventComposite;

	}

}
