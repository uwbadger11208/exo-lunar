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
import static java.lang.Math.cos;
import static java.lang.Math.pow;

/**
 * This class represents a JPL Ephemeris file.
 * @author Nick Derr
 */
public class Ephemeris {

	public static final String JPL_URL = "https://ssd.jpl.nasa.gov/"
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

	private SolarSpectra spectra;

	protected Scanner ephem; // to get lines from ephemeris
	protected String[] current; // the current line of the ephemeris, split by spaces
	protected boolean closed; // whether ephem has been closed
	LibrationEphemeris libra; // keep track of libration

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
		System.out.println("selenocentric origin: divided into " + (ephDates.size()) + " periods...");

		// construct string builder from which to make scanner
		String line = "";
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (Date[] period : ephDates) {
			System.out.print("\nacquiring Period " + (i+1) + " selenocentric origin ephemeris...");
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

		// get libration data
		libra = new LibrationEphemeris(dates);

		spectra = new SolarSpectra();
	}

	public Ephemeris(Date[] dates, String urlBase) throws IOException {
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

		String axisMess;
		if (urlBase.equals(AxisEphemeris.X)) {
			axisMess = "selenocentric x-axis";
		} else if (urlBase.equals(AxisEphemeris.Z)) {
			axisMess = "selenocentric z-axis";
		} else {
			axisMess = "UNKNOWN VALUE";
		}
		System.out.println(axisMess + ": divided into " + (ephDates.size()) + " periods...");

		// construct string builder from which to make scanner
		String line = "";
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (Date[] period : ephDates) {
			System.out.print("\nacquiring Period " + (i+1) + " " + axisMess + " ephemeris...");
			i++;
			URL url = new URL(Ephemeris.ephRequest(urlBase,period));
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

	public Double[] getGeocentricCrater(String craterName) throws 
	EphemerisDataMissingException, EphemerisDataParseException,
	BadTransferException {
		Double[] craterCoords = libra.getCraterCoords().get(craterName);
			if (craterCoords == null)
				throw new BadTransferException("bad crater name");
		Vector moonCenter = new Vector(getDeclination()*Math.PI/180,getRightAcension()*Math.PI/180).scale(getTargetRange());
		Vector selenoCrater = new Vector(craterCoords[1]*Math.PI/180,craterCoords[0]*Math.PI/180).scale(
				LibrationEphemeris.LUNAR_RADIUS);
		Vector geoCrater = coordTrans().times(
				selenoCrater).plus(moonCenter);
		double r1 = geoCrater.getAzi()*180/Math.PI;
		double d1 = geoCrater.getElev()*180/Math.PI;
		return new Double[] {r1,d1};
	}

	public Double[] getLunarCoords(String craterName, double ew_dist, double ns_dist, String origin, double fov) 
			throws BadTransferException, EphemerisDataMissingException, EphemerisDataParseException {
		Double[] coords = getFovRaDec(craterName,ew_dist,ns_dist,origin,fov);
		return getLunarCoords(coords[0],coords[1]);
	}

	public Double[] getFovRaDec(String craterName, double ew_dist, double ns_dist, String origin, double fov) 
			throws BadTransferException, EphemerisDataMissingException, EphemerisDataParseException {

		String[] orig_info = origin.split(" ");
		String orig_dist = orig_info[0];
		String orig_dir = null;
		if (orig_info.length == 2)
			orig_dir = orig_info[1];

		if (orig_info.length > 2)
			throw new BadTransferException("bad origin value");

		Double[] start;
		switch (orig_dist) {
		case "crater":
			start = getGeocentricCrater(craterName); 
			if (start == null)
				throw new BadTransferException("bad crater value");
			break;
		case "limb":
			if (orig_dir == null)
				throw new BadTransferException("bad origin value");
			start = getLimbRaDec(craterName,orig_dir,fov); break;
		case "aper":
			if (orig_dir == null)
				throw new BadTransferException("bad origin value");
			start = getLimbRaDec(craterName,orig_dir,LibrationEphemeris.APER_DIAM); break;
		case "edge":
			if (orig_dir == null)
				throw new BadTransferException("bad origin value");
			start = getLimbRaDec(craterName,orig_dir,LibrationEphemeris.LENS_ANG_DIAM); break;
		default:
			throw new BadTransferException("bad origin value");
		}

		start[0] += ew_dist / 240; // E/W in secs of time
		start[1] += ns_dist / 60; // N/S in arcmin

		return start;
	}

	public Double[] getLimbRaDec(String craterName, String direction, double fov) 
			throws EphemerisDataParseException, EphemerisDataMissingException, 
			BadTransferException {

		double d1,r1,d0,r0;
		r0 = getRightAcension();
		d0 = getDeclination();
		Vector center = new Vector(d0 * Math.PI / 180,r0 * Math.PI / 180);

		Double[] craterCoords = getGeocentricCrater(craterName);

		if (craterCoords == null)
			throw new BadTransferException("bad crater value");

		r1 = craterCoords[0];
		d1 = craterCoords[1];

		String[] dirs = direction.split(",");
		if (dirs.length != 1)
			throw new EphemerisDataParseException("invalid limb direction");

		double dotProd = cos((((fov + getAngularWidth())/2) / 3600) * Math.PI / 180);
		double minTol = pow(10,-10);
		double start,end,resid;
		resid = new Vector(d1 * Math.PI / 180,r1 * Math.PI / 180).dot(center) - dotProd;
		switch (dirs[0]) {
		case "n":
			start = d1;
			end = d1;
			while (new Vector(end * Math.PI / 180, r1 * Math.PI / 180).dot(center) - dotProd > 0)
				end += 0.125;

			resid = new Vector(((start + end) / 2) * Math.PI / 180, r1 * Math.PI / 180).dot(center) - dotProd;
			while (Math.abs(resid) > minTol) {
				if (resid > 0) {
					start = (start + end) / 2;
				} else {
					end = (start + end) / 2;
				}
				resid = new Vector(((start + end) / 2) * Math.PI / 180,r1 * Math.PI / 180).dot(center) - dotProd;
			}
			d1 = (start + end) / 2;

			break;
		case "s":
			start = d1;
			end = d1;
			while (new Vector(end * Math.PI / 180, r1 * Math.PI / 180).dot(center) - dotProd > 0)
				end -= 0.125;

			resid = new Vector(((start + end) / 2) * Math.PI / 180, r1 * Math.PI / 180).dot(center) - dotProd;
			while (Math.abs(resid) > minTol) {
				if (resid > 0) {
					start = (start + end) / 2;
				} else {
					end = (start + end) / 2;
				}
				resid = new Vector(((start + end) / 2) * Math.PI / 180,r1 * Math.PI / 180).dot(center) - dotProd;
			}
			d1 = (start + end) / 2;

			break;
		case "e":
			start = r1;
			end = r1;
			while (new Vector(d1 * Math.PI / 180, end * Math.PI / 180).dot(center) - dotProd > 0)
				end += 0.125;

			resid = new Vector(d1 * Math.PI / 180,((start + end) / 2) * Math.PI / 180).dot(center) - dotProd;
			while (Math.abs(resid) > minTol) {
				if (resid > 0) {
					start = (start + end) / 2;
				} else {
					end = (start + end) / 2;
				}
				resid = new Vector(d1 * Math.PI / 180,((start + end) / 2) * Math.PI / 180).dot(center) - dotProd;
			}
			r1 = (start + end) / 2;
			break;

		case "w":
			start = r1;
			end = r1;
			while (new Vector(d1 * Math.PI / 180, end * Math.PI / 180).dot(center) - dotProd > 0)
				end -= 0.125;

			resid = new Vector(d1 * Math.PI / 180,((start + end) / 2) * Math.PI / 180).dot(center) - dotProd;
			while (Math.abs(resid) > minTol) {
				if (resid > 0) {
					start = (start + end) / 2;
				} else {
					end = (start + end) / 2;
				}
				resid = new Vector(d1 * Math.PI / 180,((start + end) / 2) * Math.PI / 180).dot(center) - dotProd;
			}
			r1 = (start + end) / 2;

			break;
		default: throw new EphemerisDataParseException("invalid limb direction");
		}

		return new Double[] {r1,d1};
	}

	public Double[] getLunarCoords(double ra, double dec) throws EphemerisDataMissingException, EphemerisDataParseException {
		double hypo = getTargetRange();
		double d0,r0;
		r0 = getRightAcension();
		d0 = getDeclination();
		Vector moonCenter = new Vector(d0 * Math.PI / 180,r0 * Math.PI / 180);
		Vector sightLine = new Vector(dec * Math.PI / 180,ra * Math.PI / 180);
		double cosine = moonCenter.dot(sightLine);
		sightLine = sightLine.scale(hypo*cosine);

		Vector radialVec = sightLine.plus(moonCenter.scale(hypo).negative());

		Vector lunarVec = coordTrans().transpose().times(radialVec);

		double alt = lunarVec.norm() - LibrationEphemeris.LUNAR_RADIUS;
		double lat = lunarVec.getElev() * 180 / Math.PI;
		double lon = lunarVec.getAzi() * 180 / Math.PI;

		return new Double[] {lon,lat,alt};
	}

	/**
	 * If the ephemeris contains another line, advance the 'current' line to
	 * that one and returns true. Otherwise, returns false and closes the
	 * ephemeris scanner.
	 * @return
	 */
	public boolean advance() {

		libra.advance();

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

	public Matrix coordTrans() throws EphemerisDataParseException,
	EphemerisDataMissingException {
		return libra.coordTrans(getRightAcension(), getDeclination(), getTargetRange());
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

	public double transferLineDepth(String filter, double wavelength) throws BadTransferException,
	EphemerisDataMissingException, EphemerisDataParseException {
		switch (filter) {
		case "Na D2 5890/4 A": 
			if (wavelength == 5889.9509)
				return spectra.naLineDepth(this.getSolarRangeRate());

			throw new BadTransferException("mismatched filter/wavelength."
					+ "\nNa Filter should correspond to 5889.9509 A wavelength");

		case "K D1 7699/5 A": 
			if (wavelength == 7698.9647)
				return spectra.kLineDepth(this.getSolarRangeRate());

			throw new BadTransferException("mismatched filter/wavelength."
					+ "\nK Filter should correspond to 7698.9647 A wavelength");

		default: throw new BadTransferException("bad filter value."
				+ "\nFilter should be Na D2 5890/4 A or K D1 7699/5 A");
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
		return ephRequest(JPL_URL,period);
	}

	public static String ephRequest(String urlBase, Date[] period) {
		DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		return urlBase + "&START_TIME=%27" + df.format(period[0]) + "%27&STOP_TIME=%27" +
		df.format(period[1]) + "-23:59%27";
	}

}
