package dcraft.lang;

import org.joda.time.DateTimeZone;

public interface IChronologyResource {
	IChronologyResource getParentChronologyResource();
	String getDefaultChronology();
	DateTimeZone getDefaultChronologyDefinition();
}
