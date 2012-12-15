/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package de.bernhardunger.drools.model;

import java.io.Serializable;
import java.util.Date;

import de.bernhardunger.drools.model.EventSeverity;

/**
 * A Data transfer object for the Event structure.
 * Not all fields will be filled in every given time.
 * @author Heiko W. Rupp
 * @author Bernhard Unger
 */
public class EventComposite implements Serializable {

    private static final long serialVersionUID = 1L;

	private String eventDetail;			//Das Event Detail als Text
    private int resourceId;				//Eindeutiger Ressourcen Identifier
    private String resourceName;		//Name der Ressource
    private String resourceAncestry;	//Herkunft der Ressource
    private int resourceTypeId;			//Eindeutiger Ressourcen-Typ Identifier
    private int eventId;				//Eindeutiger Event Identifier
    private String sourceLocation;		//Quelle des Events (z.B. Log Datei, SNMP Trap)
    private EventSeverity severity;		//Schweregrad des Events
    private long timestamp = -1;		//Zeitstempel des Events

    public EventComposite() {
        // needed by JSON-based UI pages
    }

    public EventComposite(String eventDetail, int resourceId, String resourceName, String resourceAncestry,
        int resourceTypeId, int eventId, EventSeverity severity, String sourceLocation, Long timestamp) {
        super();
        this.eventDetail = eventDetail;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceAncestry = resourceAncestry;
        this.resourceTypeId = resourceTypeId;
        this.eventId = eventId;
        this.severity = severity;
        this.sourceLocation = sourceLocation;
        if (timestamp != null) {
            this.timestamp = timestamp;
        }
    }

    public String getEventDetail() {
        return eventDetail;
    }

    public void setEventDetail(String eventDetail) {
        this.eventDetail = eventDetail;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public EventSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(EventSeverity severity) {
        this.severity = severity;
    }

    public Date getTimestamp() {
        return new Date(timestamp);
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp.getTime();
    }

    public String getResourceAncestry() {
        return resourceAncestry;
    }

    public void setResourceAncestry(String resourceAncestry) {
        this.resourceAncestry = resourceAncestry;
    }

    public int getResourceTypeId() {
        return resourceTypeId;
    }

    public void setResourceTypeId(int resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }

	@Override
	public String toString() {
		return "EventComposite [eventId=" + eventId + ", eventDetail=" + eventDetail + ", resourceId="
				+ resourceId + ", resourceName=" + resourceName
				+ ", resourceAncestry=" + resourceAncestry
				+ ", resourceTypeId=" + resourceTypeId + ", eventId=" + eventId
				+ ", sourceLocation=" + sourceLocation + ", severity="
				+ severity + ", timestamp=" + new Date(timestamp) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((eventDetail == null) ? 0 : eventDetail.hashCode());
		result = prime * result + eventId;
		result = prime
				* result
				+ ((resourceAncestry == null) ? 0 : resourceAncestry.hashCode());
		result = prime * result + resourceId;
		result = prime * result
				+ ((resourceName == null) ? 0 : resourceName.hashCode());
		result = prime * result + resourceTypeId;
		result = prime * result
				+ ((severity == null) ? 0 : severity.hashCode());
		result = prime * result
				+ ((sourceLocation == null) ? 0 : sourceLocation.hashCode());
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventComposite other = (EventComposite) obj;
		if (eventDetail == null) {
			if (other.eventDetail != null)
				return false;
		} else if (!eventDetail.equals(other.eventDetail))
			return false;
		if (eventId != other.eventId)
			return false;
		if (resourceAncestry == null) {
			if (other.resourceAncestry != null)
				return false;
		} else if (!resourceAncestry.equals(other.resourceAncestry))
			return false;
		if (resourceId != other.resourceId)
			return false;
		if (resourceName == null) {
			if (other.resourceName != null)
				return false;
		} else if (!resourceName.equals(other.resourceName))
			return false;
		if (resourceTypeId != other.resourceTypeId)
			return false;
		if (severity != other.severity)
			return false;
		if (sourceLocation == null) {
			if (other.sourceLocation != null)
				return false;
		} else if (!sourceLocation.equals(other.sourceLocation))
			return false;
		if (timestamp != other.timestamp)
			return false;
		return true;
	}

}
