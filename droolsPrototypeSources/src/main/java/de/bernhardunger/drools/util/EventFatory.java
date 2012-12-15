package de.bernhardunger.drools.util;

import de.bernhardunger.drools.model.EventComposite;
import de.bernhardunger.drools.model.EventSeverity;
/**
 * Event Factory for creation of test events.
 * @author Bernhard Unger
 *
 */
public class EventFatory {
	
	public static final String EVENT_HTTP_CONNECTION = "http connection in use";
	public static final String EVENT_HTTP_CONNECTION_RESSOURCE = "webserver ressource";
	public static final int EVENT_HTTP_CONNECTION_RESSOURCE_ID = 1;
	public static final int EVENT_HTTP_CONNECTION_RESSOURCE_TYPE_ID = 1000;
	public static final String EVENT_DB_CONNECTION = "db connection in use";
	public static final String EVENT_DB_CONNECTION_RESSOURCE = "DB ressource";
	public static final int EVENT_DB_CONNECTION_RESSOURCE_ID = 2;
	public static final int EVENT_DB_CONNECTION_RESSOURCE_TYPE_ID = 2000;
	public static final String EVENT_DETAIL_TYPE_C = "type C event description";
	public static final String EVENT_DETAIL_TYPE_C_RESSOURCE = "type C ressource";
	public static final int EVENT_TYPE_C_RESSOURCE_ID = 3;
	public static final int EVENT_TYPE_C_RESSOURCE_TYPE_ID = 3;
	public static final String EVENT_DETAIL_TYPE_D = "type D event description";
	public static final String EVENT_DETAIL_TYPE_D_RESSOURCE = "type D ressource";
	public static final int EVENT_TYPE_D_RESSOURCE_ID = 4;
	public static final int EVENT_TYPE_D_RESSOURCE_TYPE_ID = 4;
	
	public static EventComposite makeEvent(String eventDetail, String resourceName, int resourceId, int resourceTypeId,
			int eventId, long timestamp) {
		
		EventSeverity severity = EventSeverity.INFO;
		if (eventDetail.equals(EVENT_HTTP_CONNECTION)) {
			severity = EventSeverity.INFO;
		} else if (eventDetail.equals(EVENT_DB_CONNECTION)) {
			severity = EventSeverity.INFO;
		} else if (eventDetail.equals(EVENT_DETAIL_TYPE_C)) {
			severity = EventSeverity.ERROR;
		} else if (eventDetail.equals(EVENT_DETAIL_TYPE_D)) {
			severity = EventSeverity.FATAL;
		} else {
			severity = EventSeverity.INFO;
		}

		return new EventComposite(eventDetail, resourceId, resourceName, "resourceAncestry", resourceTypeId, eventId,
				severity, "sourceLocation", timestamp);
	}
	
	public static EventComposite makeSimpleEvent(String eventDetail, int resourceId, int eventId, long timestamp) {
		
		EventSeverity severity = EventSeverity.INFO;
		
		return new EventComposite(eventDetail, resourceId, "resourceName" + resourceId, "resourceAncestry" + resourceId, resourceId, eventId,
				severity, "sourceLocation", timestamp);
	}
}
