package datasets;

import java.util.Arrays;

/**
 * As the name implies, this is a an all-purpose utility containing
 * methods that do various kinds of conversion.
 *
 */
public final class SonicScrewdriver {

	public static String exceptionStacktraceToString(Exception e) {
	    return Arrays.toString(e.getStackTrace());
	}
}
