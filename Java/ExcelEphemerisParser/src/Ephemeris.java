import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Date;
import java.util.TimeZone;

/**
 * This class represents a JPL Ephemeris file.
 * @author Nick Derr
 */
public class Ephemeris {

	public static final String JPL_URL = "http://ssd.jpl.nasa.gov/"
			+ "horizons_batch.cgi?batch=1&"
			+ "COMMAND=%27301%27&"
			+ "CENTER=%27695@399%27&"
			+ "OBJ_DATA=%27NO%27&"
			+ "MAKE_EPHEM=%27YES%27&"
			+ "TABLE_TYPE=%27OBS%27&"
			+ "RANGE_UNITS=%27KM%27&"
			+ "ANG_FORMAT=%27DEG%27&"
			+ "QUANTITIES=%271,4,7,8,9,10,13,14,15,19,20,23,24%27&"
			+ "SKIP_DAYLT=%27YES%27&"
			+ "CSV_FORMAT=%27NO%27&"
			+ "STEP_SIZE=%271%20m%27";

	private Scanner ephem; // to get lines from ephemeris
	private String[] current; // the current line of the ephemeris, split by spaces
	private boolean closed; // whether ephem has been closed

	// class constants - indices of ephemeris rows
	public static final int DATE = 0;
	public static final int TIME = 1;
	public static final int SOLAR_LUNAR = 2;
	public static final int RA = 3;
	public static final int DEC = 4;
	public static final int AZI = 5;
	public static final int ELEV = 6;
	public static final int LST = 7;
	public static final int A_MASS = 8;
	public static final int AP_MAG = 9;    // 3/20/15: incremented surf_brt and up to
	public static final int SURF_BRT = 11; // reflect inclusion of mag_ex in ephemeris data
	public static final int FRAC_ILL = 12;
	public static final int ANG_WID = 13;
	public static final int TAR_LON = 14;
	public static final int TAR_LAT = 15;
	public static final int SOL_LON = 16;
	public static final int SOL_LAT = 17;
	public static final int R = 18;
	public static final int R_DOT = 19;
	public static final int DELTA = 20;
	public static final int DELTA_DOT = 21;
	public static final int SOT = 22;
	public static final int L_OR_T = 23;
	public static final int STO = 24;

	// unit conversions
	public static final int MIL_PER_MIN = 60000;
	public static final int MIN_PER_35_DAYS = 50400;

	// class constants - date/time formats and time zone
	public static final String DATE_FORMAT = "yyyy-MMM-dd";
	public static final String TIME_FORMAT = "HH:mm";
	public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	/**
	 * Instantiates a new Ephemeris object from the given text file
	 * @param file the text file from which to instantiate the Ephemeris
	 * @throws IOException if the given file is unreadable
	 */
	public Ephemeris(File file) throws IOException {

		// check for null values
		if (file == null)
			throw new IllegalArgumentException("Null values not accepted"
					+ " as a parameter for Ephemeris constructor");

		// instantiate Scanner
		ephem = new Scanner(file);

		// skip header info
		String line = "";
		while (!line.equals("$$SOE")) 
			line = ephem.nextLine();

		// check that data exists after header
		if (!ephem.hasNext())
			throw new IllegalArgumentException(
					"Provided file contains no lines of ephemeris data");

		// read in current line's data, check that it's not end of ephemeris
		current = ephem.nextLine().trim().split("\\s+");
		if (current[0].equals("$$EOE"))
			throw new IllegalArgumentException(
					"Provided file contains no lines of ephemeris data");
	}

	public Ephemeris(Date[] dates) throws IOException {
		// check for null values
		if (dates == null)
			throw new IllegalArgumentException("Null values not accepted"
					+ " as a parameter for Ephemeris constructor");

		// get ephemeris ranges
		List<Date[]> ephDates = new LinkedList<Date[]>();
		
		Date startDate = dates[0];
		Date endDate = null;
		SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);
		System.out.println();
		boolean first = true;
		for (int i = 0; i < dates.length; i++) {
			if (first) {
				startDate = dates[i];
				first = false;
			} else if (dates[i].getTime() - dates[i-1].getTime() > 3*24*60*MIL_PER_MIN) {
				endDate = dates[i-1];
				Date[] period = new Date[2];
				period[0] = startDate;
				period[1] = endDate;
				ephDates.add(period);
				startDate = dates[i];
				for (int j = 0; j < period.length; j++)
					System.out.print(dateFormatter.format(period[j]) + " ");
				System.out.println();
			}
			if (i == dates.length - 1) {
				endDate = dates[i];
				Date[] period = new Date[2];
				period[0] = startDate;
				period[1] = endDate;
				ephDates.add(period);
				for (int j = 0; j < period.length; j++)
					System.out.print(dateFormatter.format(period[j]) + " ");
				System.out.println();
			}
		}
		System.out.println("divided into " + (ephDates.size()) + " periods...");

		// construct string builder from which to make scanner
		String line = "";
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (Date[] period : ephDates) {
			System.out.print("\nacquiring Period " + (i+1) + " ephemeris...");
			i++;
			URL url = new URL(Ephemeris.ephRequest(period));
			Scanner in = new Scanner(url.openStream());
			System.out.println("done");
			// skip header info
			System.out.println("***************************************************");
			while (!line.equals("$$SOE")) {
				line = in.nextLine();
				if (line.contains("Start time") || line.contains("Stop  time"))
					System.out.println(line);
			}
			System.out.print("reading in data...");

			do {
				line = in.nextLine();
				if (!line.equals("$$EOE") && !line.equals("") && 
						!line.contains("Daylight Cut-off")) {
					sb.append(line + '\n');
				} 
			} while (!line.equals("$$EOE"));

			in.close();
			System.out.println("done");
			System.out.println("***************************************************");
		}

		// add ending signal
		sb.append("$$EOE");

		// instantiate Scanner
		ephem = new Scanner(sb.toString());

		// read in current line's data, check that it's not end of ephemeris
		current = ephem.nextLine().trim().split("\\s+");
		if (current[0].equals("$$EOE"))
			throw new IllegalArgumentException(
					"Provided file contains no lines of ephemeris data");

	}

	public Ephemeris(Date start, Date end) throws IOException {

		// check for null values
		if (start == null || end == null)
			throw new IllegalArgumentException("Null values not accepted"
					+ " as a parameter for Ephemeris constructor");

		// check if date range is too long
		Date mid = end;
		List<Date> ephDates = new LinkedList<Date>();

		if (((mid.getTime() - start.getTime()) / MIL_PER_MIN) > MIN_PER_35_DAYS) {
			while (((mid.getTime() - start.getTime()) / MIL_PER_MIN) > MIN_PER_35_DAYS) {
				ephDates.add(0,mid);
				mid = new Date(mid.getTime() - ((long) MIL_PER_MIN)*((long) MIN_PER_35_DAYS));
			}
			ephDates.add(0,start);

			System.out.println("divided into " + (ephDates.size()-1) + " time periods...");
		} else {
			ephDates.add(0,end);
			ephDates.add(0,start);

			System.out.println("collected into a single period...");
		}

		// construct string builder from which to make scanner
		String line = "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i <= ephDates.size() - 2; i++) {
			System.out.print("\nacquiring Period " + (i+1) + " ephemeris...");
			URL url = new URL(Ephemeris.ephRequest(ephDates.get(i),ephDates.get(i+1)));
			Scanner in = new Scanner(url.openStream());
			System.out.println("done");
			// skip header info
			System.out.println("***************************************************");
			while (!line.equals("$$SOE")) {
				line = in.nextLine();
				if (line.contains("Start time") || line.contains("Stop  time"))
					System.out.println(line);
			}
			System.out.print("reading in data...");

			do {
				line = in.nextLine();
				if (!line.equals("$$EOE") && !line.equals("") && 
						!line.contains("Daylight Cut-off")) {
					sb.append(line + '\n');
				} 
			} while (!line.equals("$$EOE"));

			in.close();
			System.out.println("done");
			System.out.println("***************************************************");
		}

		// add ending signal
		sb.append("$$EOE");

		// instantiate Scanner
		ephem = new Scanner(sb.toString());

		// read in current line's data, check that it's not end of ephemeris
		current = ephem.nextLine().trim().split("\\s+");
		if (current[0].equals("$$EOE"))
			throw new IllegalArgumentException(
					"Provided file contains no lines of ephemeris data");
	}

	/**
	 * If the ephemeris contains another line, advance the 'current' line to
	 * that one and returns true. Otherwise, returns false and closes the
	 * ephemeris scanner.
	 * @return
	 */
	public boolean advance() {

		// if the scanner's closed, it can't advance
		boolean finished = closed;

		// it's not, get the next line and check
		if (!closed) {
			current = ephem.nextLine().trim().split("\\s+");
			finished = current[0].equals("$$EOE");
		}

		// if process is finished, try to close scanner
		if (finished) close();

		// return whether it advanced
		return !finished;
	}

	/**
	 * Returns whether the ephemeris has been closed
	 * @return whether the ephemeris has been closed
	 */
	public boolean isClosed() {
		return closed;
	}

	public Date getDateTime() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT + 
				TIME_FORMAT);
		try {
			return formatter.parse(current[DATE] + " " + current[TIME]);
		} catch (ParseException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}
	
	public Date getDate() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
		try {
			return formatter.parse(current[DATE]);
		} catch (ParseException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getRightAcension() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[RA]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getDeclination() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[DEC]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getAzimuth() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[AZI]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getElevation() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[ELEV]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getLocalSiderealTime() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[LST]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getAirmass() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[A_MASS]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getApparentMagnitude() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[AP_MAG]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getSurfaceBrightness() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[SURF_BRT]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getFractionIlluminated() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[FRAC_ILL]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getAngularWidth() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[ANG_WID]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getTargetLongitude() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[TAR_LON]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getTargetLatitude() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[TAR_LAT]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getSolarLongitude() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[SOL_LON]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getSolarLatitude() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[SOL_LAT]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getSolarRange() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[R]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getSolarRangeRate() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[R_DOT]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getTargetRange() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[DELTA]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getTargetRangeRate() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[DELTA_DOT]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getSunObserverTarget() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[SOT]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getSunLeadingTrailing() throws
	EphemerisDataMissingException {
		try {
			return current[L_OR_T];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public double getSunTargetObserver() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		try {
			return Double.parseDouble(current[STO]);
		} catch (NumberFormatException e) {
			throw new EphemerisDataParseException();
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getDateStr() throws 
	EphemerisDataMissingException {
		try {
			return current[DATE];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getTimeStr() throws 
	EphemerisDataMissingException {
		try {
			return current[TIME];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getRightAcensionStr() throws 
	EphemerisDataMissingException {
		try {
			return current[RA];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getDeclinationStr() throws 
	EphemerisDataMissingException {
		try {
			return current[DEC];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getAzimuthStr() throws 
	EphemerisDataMissingException {
		try {
			return current[AZI];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getElevationStr() throws 
	EphemerisDataMissingException {
		try {
			return current[ELEV];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getLocalSiderealTimeStr() throws 
	EphemerisDataMissingException {
		try {
			return current[LST];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getAirmassStr() throws 
	EphemerisDataMissingException {
		try {
			return current[A_MASS];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getApparentMagnitudeStr() throws 
	EphemerisDataMissingException {
		try {
			return current[AP_MAG];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getSurfaceBrightnessStr() throws 
	EphemerisDataMissingException {
		try {
			return current[SURF_BRT];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getFractionIlluminatedStr() throws 
	EphemerisDataMissingException {
		try {
			return current[FRAC_ILL];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getAngularWidthStr() throws 
	EphemerisDataMissingException {
		try {
			return current[ANG_WID];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getTargetLongitudeStr() throws 
	EphemerisDataMissingException {
		try {
			return current[TAR_LON];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getTargetLatitudeStr() throws 
	EphemerisDataMissingException {
		try {
			return current[TAR_LAT];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getSolarLongitudeStr() throws 
	EphemerisDataMissingException {
		try {
			return current[SOL_LON];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getSolarLatitudeStr() throws 
	EphemerisDataMissingException {
		try {
			return current[SOL_LAT];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getSolarRangeStr() throws 
	EphemerisDataMissingException {
		try {
			return current[R];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getSolarRangeRateStr() throws 
	EphemerisDataMissingException {
		try {
			return current[R_DOT];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getTargetRangeStr() throws 
	EphemerisDataMissingException {
		try {
			return current[DELTA];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getTargetRangeRateStr() throws 
	EphemerisDataMissingException {
		try {
			return current[DELTA_DOT];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getSunObserverTargetStr() throws 
	EphemerisDataMissingException {
		try {
			return current[SOT];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	public String getSunTargetObserverStr() throws 
	EphemerisDataMissingException {
		try {
			return current[STO];
		} catch (NullPointerException e) {
			throw new EphemerisDataMissingException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EphemerisDataMissingException();
		}
	}

	/**
	 * Closes the ephemeris scanner if it hasn't already been closed
	 */
	public void close() {
		// close scanner if not already done
		if (!closed) ephem.close();
		closed = true;
	}

	/**
	 * Driver method for testing purposes
	 */
	public static void main(String[] args) {

		// try making it
		Ephemeris eph = null;
		try {
			eph = new Ephemeris(new File("lib/horizons_results.txt"));
		} catch (IOException exc) {
			System.out.println("ERROR: " + exc.getMessage());
			System.exit(-1);
		}

		// check number of lines
		int i = 0;
		do {
			i++;
		} while (eph.advance());
		System.out.println(i + " should be 120");
		System.out.println(eph.advance() + " should be false");

		// check closing works
		try {
			eph = new Ephemeris(new File("lib/horizons_results.txt"));
		} catch (IOException exc) {
			System.out.println("ERROR: " + exc.getMessage());
			System.exit(-1);
		}
		eph.close();
		System.out.println(eph.advance() + " should be false");

		// try some values
		try {
			eph = new Ephemeris(new File("lib/horizons_results.txt"));
		} catch (IOException exc) {
			System.out.println("ERROR: " + exc.getMessage());
			System.exit(-1);
		}

		for (i = 0; i < 4; i++)
			eph.advance();
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.FULL);
		df.setTimeZone(UTC);
		try {
			System.out.println();
			System.out.println("*** using values ***");
			System.out.println("Date/Time: " + df.format(eph.getDateTime()));
			System.out.println("RA: " + eph.getRightAcension());
			System.out.println("Dec: " + eph.getDeclination());
			System.out.println("Azim: " + eph.getAzimuth());
			System.out.println("Elev: " + eph.getElevation());
			System.out.println("LST: " + eph.getLocalSiderealTime());
			System.out.println("Airmass: " + eph.getAirmass());
			System.out.println("Ap Mag: " + eph.getApparentMagnitude());
			System.out.println("Surf Brightness: " + eph.getSurfaceBrightness());
			System.out.println("Frac Illum: " + eph.getFractionIlluminated());
			System.out.println("Ang Diam: " + eph.getAngularWidth());
			System.out.println("Obs Lon: " + eph.getTargetLongitude());
			System.out.println("Obs Lat: " + eph.getTargetLatitude());
			System.out.println("Sol Lon: " + eph.getSolarLongitude());
			System.out.println("Sol Lat: " + eph.getSolarLatitude());
			System.out.println("r: " + eph.getSolarRange());
			System.out.println("r dot: " + eph.getSolarRangeRate());
			System.out.println("delta: " + eph.getTargetRange());
			System.out.println("delta dot: " + eph.getTargetRangeRate());
			System.out.println("S-O-T: " + eph.getSunObserverTarget());
			System.out.println("L or T: " + eph.getSunLeadingTrailing());
			System.out.println("S-T-O: " + eph.getSunTargetObserver());
			System.out.println();
			System.out.println("*** using strings ***");
			System.out.println("Date: " + eph.getDateStr());
			System.out.println("Time: " + eph.getTimeStr());
			System.out.println("RA: " + eph.getRightAcensionStr());
			System.out.println("Dec: " + eph.getDeclinationStr());
			System.out.println("Azim: " + eph.getAzimuthStr());
			System.out.println("Elev: " + eph.getElevationStr());
			System.out.println("LST: " + eph.getLocalSiderealTimeStr());
			System.out.println("Airmass: " + eph.getAirmassStr());
			System.out.println("Ap Mag: " + eph.getApparentMagnitudeStr());
			System.out.println("Surf Brightness: " + eph.getSurfaceBrightnessStr());
			System.out.println("Frac Illum: " + eph.getFractionIlluminatedStr());
			System.out.println("Ang Diam: " + eph.getAngularWidthStr());
			System.out.println("Obs Lon: " + eph.getTargetLongitudeStr());
			System.out.println("Obs Lat: " + eph.getTargetLatitudeStr());
			System.out.println("Sol Lon: " + eph.getSolarLongitudeStr());
			System.out.println("Sol Lat: " + eph.getSolarLatitudeStr());
			System.out.println("r: " + eph.getSolarRangeStr());
			System.out.println("r dot: " + eph.getSolarRangeRateStr());
			System.out.println("delta: " + eph.getTargetRangeStr());
			System.out.println("delta dot: " + eph.getTargetRangeRateStr());
			System.out.println("S-O-T: " + eph.getSunObserverTargetStr());
			System.out.println("L or T: " + eph.getSunLeadingTrailing());
			System.out.println("S-T-O: " + eph.getSunTargetObserverStr());
		} catch (EphemerisDataException e) {
			System.out.println("shouldn't trigger");
		}
	}

	public static String ephRequest(Date start, Date end) {
		DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		return JPL_URL + "&START_TIME=%27" + df.format(start) + "%27&STOP_TIME=%27" +
		df.format(end) + "%27";
	}
	
	public static String ephRequest(Date[] period) {
		DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		return JPL_URL + "&START_TIME=%27" + df.format(period[0]) + "%27&STOP_TIME=%27" +
		df.format(period[1]) + "-23:59%27";
	}

}
