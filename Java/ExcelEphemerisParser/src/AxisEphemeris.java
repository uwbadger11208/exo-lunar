import java.io.File;
import java.io.IOException;
import java.util.Date;


public class AxisEphemeris extends Ephemeris {

	public static final String O = "http://ssd.jpl.nasa.gov/"
			+ "horizons_batch.cgi?batch=1&"
			+ "COMMAND=%27301%27&"
			+ "CENTER=%27695@399%27&"
			+ "OBJ_DATA=%27NO%27&"
			+ "MAKE_EPHEM=%27YES%27&"
			+ "TABLE_TYPE=%27OBS%27&"
			+ "RANGE_UNITS=%27KM%27&"
			+ "ANG_FORMAT=%27DEG%27&"
			+ "QUANTITIES=%271,20%27&"
			+ "SKIP_DAYLT=%27YES%27&"
			+ "CSV_FORMAT=%27NO%27&"
			+ "STEP_SIZE=%271%20m%27";
	
	public static final String Z = "http://ssd.jpl.nasa.gov/"
			+ "horizons_batch.cgi?batch=1&"
			+ "COMMAND=%27g:0,0,0@301%27&"
			+ "CENTER=%27695@399%27&"
			+ "OBJ_DATA=%27NO%27&"
			+ "MAKE_EPHEM=%27YES%27&"
			+ "TABLE_TYPE=%27OBS%27&"
			+ "RANGE_UNITS=%27KM%27&"
			+ "ANG_FORMAT=%27DEG%27&"
			+ "QUANTITIES=%271,20%27&"
			+ "SKIP_DAYLT=%27YES%27&"
			+ "CSV_FORMAT=%27NO%27&"
			+ "STEP_SIZE=%271%20m%27";
	
	public static final String Y = "http://ssd.jpl.nasa.gov/"
			+ "horizons_batch.cgi?batch=1&"
			+ "COMMAND=%27g:0,90,0@301%27&"
			+ "CENTER=%27695@399%27&"
			+ "OBJ_DATA=%27NO%27&"
			+ "MAKE_EPHEM=%27YES%27&"
			+ "TABLE_TYPE=%27OBS%27&"
			+ "RANGE_UNITS=%27KM%27&"
			+ "ANG_FORMAT=%27DEG%27&"
			+ "QUANTITIES=%271,20%27&"
			+ "SKIP_DAYLT=%27YES%27&"
			+ "CSV_FORMAT=%27NO%27&"
			+ "STEP_SIZE=%271%20m%27";
	
	// class constants - indices of ephemeris rows
	public static final int RA = 4;
	public static final int DEC = 5;
	public static final int DELTA = 6;
	public static final int DELTA_DOT = 7;

	public AxisEphemeris(Date[] dates, String urlBase) throws IOException {
		super(dates,urlBase);
	}
	
	public AxisEphemeris(File file) throws IOException {
		super(file);
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
	
}
