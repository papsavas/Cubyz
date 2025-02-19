package io.cubyz;

import io.cubyz.translate.Language;
import io.cubyz.translate.TextKey;

/**
 * Stores all things that can be changed on both sides.
 */

public class Settings {
	
	private static Language currentLanguage = null;
	
	public static int entityDistance = 5;
	
	public static void setLanguage(Language lang) {
		currentLanguage = lang;
		TextKey.updateLanguage();
	}
	public static Language getLanguage() {
		return currentLanguage;
	}
	
}
