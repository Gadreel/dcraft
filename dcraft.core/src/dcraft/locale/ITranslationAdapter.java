package dcraft.locale;

public interface ITranslationAdapter {
	int rateLocale(String locale);
	LocaleDefinition getLocaleDefinition(String localename);
}
