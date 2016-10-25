package dcraft.web.core;

public enum HtmlMode {
	Static,
	Dynamic,	// Pui, fallback on Ssi
	Strict, 	// Pui only
	Ssi		// Server Side Includes
}
