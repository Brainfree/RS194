package pre194;

public final class StringUtil {

	public static final char[] BASE37_LOOKUP = {'_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
	public static final String ASCII_TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\u00a3$%^&*()-_=+[{]};:'@#~,<.>/?\\| ";

	public static long toBase37(String s) {
		s = s.trim();
		long l = 0L;

		for (int i = 0; i < s.length() && i < 12; i++) {
			char c = s.charAt(i);
			l *= 37L;

			if (c >= 'A' && c <= 'Z') {
				l += (c + 1) - 'A';
			} else if (c >= 'a' && c <= 'z') {
				l += (c + 1) - 'a';
			} else if (c >= '0' && c <= '9') {
				l += (c + 27) - '0';
			}
		}
		return l;
	}

	public static String fromBase37(long l) {
		// >= 37 to the 12th power
		if (l < 0L || l >= 6582952005840035281L) {
			return "invalid_name";
		}

		int len = 0;
		char[] chars = new char[12];
		while (l != 0L) {
			long l1 = l;
			l /= 37L;
			chars[11 - len++] = BASE37_LOOKUP[(int) (l1 - l * 37L)];
		}
		return new String(chars, 12 - len, len);
	}

	public static int getHash(String s) {
		int hash = 0;
		s = s.toUpperCase();
		for (int i = 0; i < s.length(); i++) {
			hash = hash * 61 + s.charAt(i) - 32;
		}
		return hash;
	}

	/**
	 * Only allows the string to contain 'a' to 'z', '0' to '9', and '_'.
	 *
	 * @param string the input string.
	 * @return the safe string.
	 */
	public static String getSafe(String string) {
		string = string.toLowerCase().trim();
		StringBuilder sb = new StringBuilder();

		for (int n = 0; n < string.length(); n++) {
			if (n >= 12) {
				break;
			}

			char c = string.charAt(n);

			if (isLowercaseAlpha(c) || isNumeral(c)) {
				sb.append(c);
			} else {
				sb.append('_');
			}
		}

		string = sb.toString();

		while (string.charAt(0) == '_') {
			string = string.substring(1);
		}

		while (string.charAt(string.length() - 1) == '_') {
			string = string.substring(0, string.length() - 1);
		}

		return string;
	}

	public static String getFormatted(String s) {
		if (s.length() > 0) {
			char[] chars = s.toCharArray();

			for (int n = 0; n < chars.length; n++) {
				if (chars[n] == '_') {
					chars[n] = ' ';

					// next letter will be upper case
					int m = n + 1;
					if (m < chars.length && isLowercaseAlpha(chars[m])) {
						chars[m] = (char) ((chars[m] + 'A') - 'a');
					}
				}
			}

			// First letter always upper case
			if (isLowercaseAlpha(chars[0])) {
				chars[0] = (char) ((chars[0] + 'A') - 'a');
			}
			return new String(chars);
		}
		return s;
	}

	public static String getPunctuated(String s) {
		char[] chars = s.toLowerCase().toCharArray();

		boolean capitalize = true;
		for (int n = 0; n < chars.length; n++) {
			char c = chars[n];

			if (capitalize && isLowercaseAlpha(c)) {
				chars[n] -= ' ';
				capitalize = false;
			}

			if (c == '.' || c == '!') {
				capitalize = true;
			}
		}
		return new String(chars);
	}

	public static String toAsterisks(String s) {
		char[] c = new char[s.length()];
		for (int n = 0; n < c.length; n++) {
			c[n] = '*';
		}
		return new String(c);
	}

	public static boolean isSymbol(char c) {
		return !isAlpha(c) && !isNumeral(c);
	}

	public static boolean isNotLowercaseAlpha(char c) {
		if (c < 'a' || c > 'z') {
			return true;
		}
		return c == 'v' || c == 'x' || c == 'j' || c == 'q' || c == 'z';
	}

	public static boolean isAlpha(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
	}

	public static boolean isNumeral(char c) {
		return c >= '0' && c <= '9';
	}

	public static boolean isLowercaseAlpha(char c) {
		return c >= 'a' && c <= 'z';
	}

	public static boolean isUppercaseAlpha(char c) {
		return c >= 'A' && c <= 'Z';
	}

	public static boolean isASCII(char c) {
		return c >= ' ' && c <= '~';
	}

}
