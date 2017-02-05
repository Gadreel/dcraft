package dcraft.locale;

public interface ITranslationAdapter {
	String getWorkingLocale();
	int rateLocale(String locale);
	LocaleDefinition getLocaleDefinition(String localename);
}
