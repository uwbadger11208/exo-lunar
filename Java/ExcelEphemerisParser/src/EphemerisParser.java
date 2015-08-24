import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/**
 * The main method of a program which reads in a .txt file from a JPL ephemeris
 * and inserts certain values into an already existing .xlsx file, likewise
 * passed by the user
 * 
 * @author Nick Derr
 */
public class EphemerisParser {

	// format of sheet date names
	public static final String SHEET_NAME_FORMAT = "yyMMMdd";
	public static final String SHEET_TIME_FORMAT = "HH:mm";
	public static final String FILES_TO_TRANSFER = "moon";
	public static final int FNAME_LENGTH = FILES_TO_TRANSFER.length();

	public static final String XLS = ".xls";
	public static final int XLS_LEN = XLS.length();
	public static final String XLSX = ".xlsx";
	public static final int XLSX_LEN = XLSX.length();

	public static final int SEC_PER_MIN = 60;
	public static final int MIN_PER_HOUR = 60;

	/**
	 * The main method. Reads in a .txt file, extracts information from the
	 * ephemeris therein, and inserts the information into an excel file
	 * likewise passed by the user. If a .txt file with a list of times
	 * is passed to the user, the program will loop through those times 
	 * extracting the information and exit. If no such file is provided, the
	 * program will prompt the user for times to use for the extraction
	 * continuously until the user indicates the program should exit
	 * 
	 * @param args the user arguments
	 */
	public static void main(String[] args) {

		// the class representing the provided files
		Ephemeris ephem = null;
		Workbook log = null;
		Sheet night = null;
		ExcelDataParser parse = null;
		DateFormat dateFormatter = new SimpleDateFormat(SHEET_NAME_FORMAT);
		DateFormat timeFormatter = new SimpleDateFormat(SHEET_TIME_FORMAT);

		boolean ephProvided;

		// the filenames
		String excelFile = null;
		String excelFileBackup = null;
		String ephemFile = null;

		// print usage if wrong num args
		if (args.length == 0) {
			System.out.println("Usage: java -jar ephparse.jar [-e ephemeris.txt] log_file.xlsx");
			System.exit(-1);
		}

		if (args[0].equals("-e")) {
			ephProvided = true;
			if (args.length != 3) {
				System.out.println("Usage: java -jar ephparse.jar [-e ephemeris.txt] log_file.xlsx");
				System.exit(-1);
			}
		} else {
			ephProvided = false;
			if (args.length != 1) {
				System.out.println("Usage: java -jar ephparse.jar [-e ephemeris.txt] log_file.xlsx");
				System.exit(-1);
			}
		}

		// get filenames
		if (ephProvided) {
			ephemFile = args[1];
			excelFile = args[2];
		} else {
			excelFile = args[0];
		}

		// try instantiating classes
		//Date start = null; 
		try {

			// get ephemeris if here
			if (ephProvided) {
				System.out.print("retrieving ephemeris data...");
				// check for text file
				if (args[1].indexOf(".txt") != args[1].length() - 4)
					throw new IllegalArgumentException(
							"Provided file is not a .txt file");

				ephem = new Ephemeris(new File(args[1]));
				System.out.println("done");
			}

			// get excel log
			System.out.print("retrieving excel spreadsheet...");
			InputStream in;

			// if XLSX, read in and immediately write to HSSF, then read that
			if (excelFile.indexOf(XLS) == excelFile.length() - XLS.length()) {
				in = new FileInputStream(excelFile);
				log = new HSSFWorkbook(in);
				in.close();
			} else if (excelFile.indexOf(XLSX) == excelFile.length() - XLSX.length()) {
				/*
				System.out.print("saving xlsx file to temporary xls file...");
				try {
					xlsx2xls(excelFile,styles);
				} catch (Exception exc) {
					System.out.println("error");
					System.out.println(exc.getMessage());
					System.out.println("Could not save xlsx file to temporary"
							+ " xls file. Check format");
					System.exit(-1);
				}
				in = new FileInputStream(xlsx2xlsName(excelFile));
				log = new HSSFWorkbook(in);
				*/
				in = new FileInputStream(excelFile);
				log = new XSSFWorkbook(in);
				in.close();
			} else {
				throw new IOException();
			}
			System.out.println("done");

			

			// get ephemeris if NOT here
			if (!ephProvided) {

				System.out.print("retrieving dates for ephemeris...");
				// get dates of sheet
				Date[] dates = null;
				try {
					//*
					dates = new Date[log.getNumberOfSheets()];
					for (int i = 0; i < dates.length; i++)
						dates[i] = dateFormatter.parse(log.getSheetName(i));
					 //*/
					/*
					start = dateFormatter.parse(log.getSheetName(0));
					end = dateFormatter.parse(log.getSheetName(log.getNumberOfSheets()-1));
					end = new Date(end.getTime() + 24*60*Ephemeris.MIL_PER_MIN);
					*/
				} catch (ParseException e) {
					throw new WebEphemerisException("unable to parse start and end"
							+ " dates from spreadsheet");
				}

				System.out.println("done");

				// get URL of eph request
				System.out.println("retrieving ephemeris data...");
				ephem = new Ephemeris(dates);
				System.out.println("... ephemeris retrieval complete");
			}

		} catch (FileNotFoundException e) {

			// try file not found
			System.out.println("error");
			if (ephem == null) {
				System.out.println("While opening ephemeris file \""
						+ ephemFile + "\": file not found");
			} else {
				System.out.println("While opening Excel file \""
						+ excelFile + "\": file not found");
			}
			System.exit(-1);
		} catch (IOException e) {

			// don't know what other IOExceptions might do
			System.out.println("error");
			if (ephem == null) {
				if (ephProvided) {
					System.out.println("Could not open ephemeris file \""
							+ ephemFile + "\"");
				} else {
					System.out.println("Could not access url \""
							+ Ephemeris.JPL_URL + "\"");
					System.out.println("Exception: " + e.getMessage());
					System.out.println("Stack trace:");
					e.printStackTrace();
				}
			} else {

				// check to see if wrong file extension
				if (excelFile.indexOf(".xlsx") == excelFile.length() - 5 ||
						excelFile.indexOf(".xls") == excelFile.length() - 4) {
					System.out.println("Could not open Excel file \""
							+ excelFile + "\". Check file format");
				} else {
					System.out.println("While opening Excel file \""
							+ excelFile + "\": Provided file is not a .xls"
							+ " or .xlsx file");
				}
			}
			System.exit(-1);
		} catch (IllegalArgumentException e) {

			// check illegal argument from ephemeris
			System.out.println("error");
			System.out.println("While opening ephemeris file \""
					+ ephemFile + "\": " + e.getMessage());
			System.exit(-1);
		}  catch (Exception e) {

			System.out.println("error");
			System.out.println("Unspecified problem loading files. Check format"
					+ " of excel file. error trace: ");
			System.out.println(e.getMessage());
			System.out.println(e.getClass().getName());
			System.exit(-1);
		}

		// create backup file in case writing process corrupts orig file
		System.out.print("creating backup file...");
		try {
			excelFileBackup = excelFile.substring(0,excelFile.indexOf('.')) +
					"_backup" + excelFile.substring(excelFile.indexOf('.'));
			OutputStream out = null;
			out = new FileOutputStream(excelFileBackup);
			log.write(out);
			out.flush();
			out.close();
			System.out.println("done");
		} catch (Exception exc) {
			System.out.println("error");
			System.out.println("Could not write backup file. Check format");
			System.exit(-1);
		} 

		// for each sheet in workbook
		for (int i = 0; i < log.getNumberOfSheets(); i++) {

			// get night
			night = log.getSheetAt(i);

			// advance ephemeris to start of night
			try {
				System.out.print("advancing to " + night.getSheetName() + 
						"...");

				Date sheetDate = dateFormatter.parse(night.getSheetName());
				while (sheetDate.before(ephem.getDate()) && i < log.getNumberOfSheets()) {
					i++;
					night = log.getSheetAt(i);
					sheetDate = dateFormatter.parse(night.getSheetName());
					System.out.println("skipped");
					System.out.println("Ephemeris date is after current sheet"
							+ " date.");
					System.out.print("advancing to " + night.getSheetName() + 
							"...");
				}

				// go through ephemeris until at start of current night
				boolean found = false;
				while (!found && !ephem.isClosed()) {
					Date ephemDate = ephem.getDate();

					// if date matches, we're there!
					if (dateFormatter.format(ephemDate).equals(night.getSheetName()))
						found = true;

					// otherwise, go to next minute
					if (!found) ephem.advance();
				}

				// make sure night was found in ephemeris
				if (ephem.isClosed()) {
					System.out.println("not found");
					System.out.println("Ephemeris ends before log does. Saving"
							+ " progress thus far.");
					break;
				} else {
					parse = new ExcelDataParser(night);
				}
				System.out.println("done");

			} catch (EphemerisDataMissingException e) {

				// check if ephemeris data is not there
				System.out.println("error");
				System.out.println("Ephemeris data absent. Check"
						+ " formatting of ephemeris file");
				System.out.print("original file never "
						+ "altered. marking backup file "
						+ "for deletion...");
				new File(excelFileBackup).deleteOnExit();
				System.out.println("done");
				System.exit(-1);
			} catch (EphemerisDataParseException e) {

				// check if ephemeris data cannot be parsed
				System.out.println("error");
				System.out.println("Could not parse ephemeris data"
						+ " to numerical value. Check formatting of ephemeris"
						+ " file or try importing data as strings");
				System.out.print("original file never "
						+ "altered. marking backup file "
						+ "for deletion...");
				new File(excelFileBackup).deleteOnExit();
				System.out.println("done");
				System.exit(-1);
			} catch (ExcelDataParserException e) {

				// check if program could parse out column indices from workbook
				System.out.println("error");
				System.out.println("Could not parse out column indices from "
						+ "workbook: " + e.getMessage());
				System.out.print("original file never "
						+ "altered. marking backup file "
						+ "for deletion...");
				new File(excelFileBackup).deleteOnExit();
				System.out.println("done");
				System.exit(-1);
			} catch (ParseException e) {

				System.out.println("error");
				System.out.println("Could not parse date from sheet name"
						+ night.getSheetName() + ". Use yyMMMdd format");
				System.exit(-1);
			}

			// go row by row
			// make sure times are in order
			int timeInt = 0;
			int imTimeInt = 0;
			String lastTimeStr = null;
			String timeStr = null;
			for (Row image : night) {

				// get cell with name
				Cell filecell = image.getCell(parse.INDICES[ExcelDataParser.FILE]);
				if (filecell != null) {

					// if not null, get contents
					if (filecell.getCellType() == Cell.CELL_TYPE_STRING) {
						String name = filecell.getStringCellValue();

						// check if it's the filetype being transfered
						if (name.length() > FNAME_LENGTH) {
							if (name.substring(0,FNAME_LENGTH).equals(
									FILES_TO_TRANSFER)) {

								// if it is, try the transfer
								try {
									System.out.print(name + "...");

									//*
									// find time to transfer
									Cell timeCell = image.getCell(parse.INDICES[ExcelDataParser.TIME]);

									// check not null
									if (timeCell == null) {
										throw new BadTransferException("Time missing");
									}

									// try to get time
									if (timeCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
										timeStr = timeFormatter.format(timeCell.getDateCellValue());
									} else if (timeCell.getCellType() == Cell.CELL_TYPE_STRING) {
										timeStr = timeCell.getStringCellValue();
									} else {
										throw new BadTransferException("Time must be entered in excel using time or"
												+ " string format. Correct and try again");
									}
									
									imTimeInt = getTimeInt(timeStr);

									if (imTimeInt > timeInt) {
										transferData(ephem,image,parse);
										
										timeInt = imTimeInt;
										lastTimeStr = timeStr;
										System.out.println("done");
									} else {
										System.out.println("error");
										System.out.println(lastTimeStr
												+ " is after " + timeStr + 
												". Re-order so times are in ascending order");
										System.out.print("marking backup file for deletion...");
										new File(excelFileBackup).deleteOnExit();
										System.out.println("done");
										System.exit(-1);
									}
								} catch (EphemerisDataMissingException e) {

									// look for data missing in ephemeris. backup
									// can be deleted - never tried to write file
									System.out.println("error");
									System.out.println("Missing data in"
											+ " ephemeris. Check ephemeris format");
									System.out.print("marking backup file for deletion...");
									new File(excelFileBackup).deleteOnExit();
									System.out.println("done");
									System.exit(-1);
								} catch (BadTransferException e) {

									// look for error in transfer. backup can be
									// deleted - never tried to write file
									System.out.println("error");
									System.out.println(e.getMessage());
									System.out.print("original file never "
											+ "altered. marking backup file "
											+ "for deletion...");
									new File(excelFileBackup).deleteOnExit();
									System.out.println("done");
									System.exit(-1);
								}
							}
						}
					}
				}
			}
			
			for (int ii = 0; ii < parse.INDICES.length; ii++) {
				night.autoSizeColumn(parse.INDICES[ii]);
			}
		}


		try {
			System.out.print("writing edited excel file...");
			
			OutputStream out = new FileOutputStream(excelFile);
			log.write(out);
			out.flush();
			out.close();
			System.out.print("file successfully written. marking backup file for deletion...");
			try {
				new File(excelFileBackup).deleteOnExit();
				System.out.println("done");
			} catch (Exception e) {
				System.out.println("error");
				System.out.println("Could not delete backup");
			}
		} catch (Exception e) {
			System.out.println("error");
			System.out.println(e.getClass().getName());
			e.printStackTrace();
			System.out.println("Could not write to file. Check"
					+ " that file is not corrupted."
					+ " Backup located at \"" + excelFileBackup + 
					"\"");
			System.out.print("Copying backup to original location...");
			try {
				InputStream in = null;
				in = new FileInputStream(excelFileBackup);
				if (excelFileBackup.indexOf(XLS) == excelFile.length() - XLS.length()) {
					log = new HSSFWorkbook(in);
				} else {
					log = new XSSFWorkbook(in);
				}
				try {
					OutputStream out = null;
					out = new FileOutputStream(excelFile);
					log.write(out);
					out.flush();
					out.close();
					System.out.println("done");
				} catch (IOException exc) {
					System.out.println("error");
					System.out.println("Could not write backup to original "
							+ "filename. Check format");
				}
			} catch (IOException ex) {
				System.out.println("error");
				System.out.println("Could not read in backup. Check format.");
			}
		}
	}
	
	private static void transferData(Ephemeris eph, Row im, ExcelDataParser p) 
			throws EphemerisDataMissingException, BadTransferException {


		// find time to transfer
		System.out.print("finding time in ephemeris...");
		DateFormat time = new SimpleDateFormat(Ephemeris.TIME_FORMAT);
		Cell timeCell = im.getCell(p.INDICES[ExcelDataParser.TIME]);
		String timeStarted = null;

		// check not null
		if (timeCell == null) {
			throw new BadTransferException("Time missing");
		}

		// try to get time
		if (timeCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
			timeStarted = time.format(timeCell.getDateCellValue());
		} else if (timeCell.getCellType() == Cell.CELL_TYPE_STRING) {
			timeStarted = timeCell.getStringCellValue();
		} else {
			throw new BadTransferException("Time must be entered in excel using time or"
					+ " string format. Correct and try again");
		}

		// find exposure time
		int expTime = 0;
		Cell expCell = im.getCell(p.INDICES[ExcelDataParser.EXP_TIME]);

		// check not null
		if (expCell == null) {
			throw new BadTransferException("Exposure time missing");
		}

		// try to get time
		if (expCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
			expTime = (int) expCell.getNumericCellValue();
		} else if (expCell.getCellType() == Cell.CELL_TYPE_STRING) {
			try {
				expTime = Integer.parseInt(expCell.getStringCellValue());
			} catch (NumberFormatException e) {
				throw new BadTransferException("invalid format for exposure time");
			}
		} else {
			throw new BadTransferException("invalid format for exposure time");
		}

		String timeToTransfer = getEphTime(timeStarted,expTime);

		boolean foundTime = false;
		while (!foundTime && !eph.isClosed()) {
			// check if this is right row
			if (eph.getTimeStr().equals(timeToTransfer))
				foundTime = true;

			// if not, go to next
			if (!foundTime)
				eph.advance();
		}

		// make sure we've found the time
		if (!foundTime) {
			throw new BadTransferException("Ephemeris does not contain desired time");
		}

		// transfer the data
		System.out.print("data transfer beginning...");

		// RA
		if (!transferRA(p,eph,im.getCell(p.INDICES[ExcelDataParser.RA]))) {
			System.out.print("RA parse failed, attempting string...");
		}

		// Dec
		if (!transferDEC(p,eph,im.getCell(p.INDICES[ExcelDataParser.DEC]))) {
			System.out.print("dec parse failed, attempting string...");
		}

		// Azimuthal
		if (!transferAzimuth(p,eph,im.getCell(p.INDICES[ExcelDataParser.AZI]))) {
			System.out.print("azi parse failed, attempting string...");
		}

		// Elevation
		if (!transferElevation(p,eph,im.getCell(p.INDICES[ExcelDataParser.ELEV]))) {
			System.out.print("elev parse failed, attempting string...");
		}

		// Sidereal
		if (!transferLST(p,eph,im.getCell(p.INDICES[ExcelDataParser.LST]))) {
			System.out.print("LST parse failed, attempting string...");
		}

		// airmass
		if (!transferAirmass(p,eph,im.getCell(p.INDICES[ExcelDataParser.A_MASS]))) {
			System.out.print("airmass parse failed, attempting string...");
		}

		// apparent mag
		if (!transferAppMag(p,eph,im.getCell(p.INDICES[ExcelDataParser.AP_MAG]))) {
			System.out.print("app mag parse failed, attempting string...");
		}

		// surface brightness
		if (!transferSurfBright(p,eph,im.getCell(p.INDICES[ExcelDataParser.SURF_BRT]))) {
			System.out.print("surf brt parse failed, attempting string...");
		}

		// illuminated fraction
		if (!transferFracIll(p,eph,im.getCell(p.INDICES[ExcelDataParser.FRAC_ILL]))) {
			System.out.print("illum frac parse failed, attempting string...");
		}

		// diameter/angular width
		if (!transferDiameter(p,eph,im.getCell(p.INDICES[ExcelDataParser.ANG_WID]))) {
			System.out.print("diameter parse failed, attempting string...");
		}

		// observer longitude
		if (!transferObsLon(p,eph,im.getCell(p.INDICES[ExcelDataParser.TAR_LON]))) {
			System.out.print("obs lon parse failed, attempting string...");
		}

		// observer latitude
		if (!transferObsLat(p,eph,im.getCell(p.INDICES[ExcelDataParser.TAR_LAT]))) {
			System.out.print("obs lat parse failed, attempting string...");
		}

		// solar longitude
		if (!transferSunLon(p,eph,im.getCell(p.INDICES[ExcelDataParser.SOL_LON]))) {
			System.out.print("sol lon parse failed, attempting string...");
		}

		// solar latitude
		if (!transferSunLat(p,eph,im.getCell(p.INDICES[ExcelDataParser.SOL_LAT]))) {
			System.out.print("sol lat parse failed, attempting string...");
		}

		// solar range
		if (!transferR(p,eph,im.getCell(p.INDICES[ExcelDataParser.R]))) {
			System.out.print("r parse failed, attempting string...");
		}

		// solar range rate
		if (!transferRDot(p,eph,im.getCell(p.INDICES[ExcelDataParser.R_DOT]))) {
			System.out.print("r dot parse failed, attempting string...");
		}

		// observer range
		if (!transferDelta(p,eph,im.getCell(p.INDICES[ExcelDataParser.DELTA]))) {
			System.out.print("delta parse failed, attempting string...");
		}

		// observer range rate
		if (!transferDeltaDot(p,eph,im.getCell(p.INDICES[ExcelDataParser.DELTA_DOT]))) {
			System.out.print("delta dot parse failed, attempting string...");
		}

		// S-O-T
		if (!transferSOT(p,eph,im.getCell(p.INDICES[ExcelDataParser.SOT]))) {
			System.out.print("S-O-T parse failed, attempting string...");
		}

		transferLOrT(p,eph,im.getCell(p.INDICES[ExcelDataParser.L_OR_T]));

		// S-T-O
		if (!transferSTO(p,eph,im.getCell(p.INDICES[ExcelDataParser.STO]))) {
			System.out.print("S-T-O parse failed, attempting string...");
		}
		
		if (!transferLunarCoords(p,eph,im)) {
			System.out.print("Lunar Coords failed...");
		}
	}

	/**
	 * Formats cell to display the given number of decimal places, centered
	 * @param c the cell to format
	 * @param decPlaces the number of decimal places to display
	 */
	private static void formatCell(ExcelDataParser p, Cell c, int decPlaces) {
		c.setCellStyle(p.getStyle(decPlaces));
	}

	/**
	 * Returns the time in the ephemeris corresponding to the midpoint of an 
	 * exposure, given its beginning time and it exposure length
	 * @param time time the exposure began
	 * @param expTime length of the exposure
	 * @return the time to transfer for this image in the ephemeris
	 */
	private static String getEphTime(String time, int expTime) 
			throws BadTransferException {

		// pull out ints from string time
		int min = 0; 
		int hour = 0;
		try {
			min = Integer.parseInt(time.substring(time.indexOf(':')+1));
			hour = Integer.parseInt(time.substring(0,time.indexOf(':')));
		} catch (NumberFormatException e) {
			throw new BadTransferException("invalid format for starting time");
		} catch (StringIndexOutOfBoundsException e) {
			throw new BadTransferException("invalid format for starting time");
		}

		// add have of exp time
		int toAdd = (int) ((((double) expTime / SEC_PER_MIN) / 2) + .5);
		min += toAdd;
		if (min >= MIN_PER_HOUR) {
			hour++;
			min = 0;
		}

		// output as string
		String hStr;
		String mStr;
		if (hour >= 10) {
			hStr = "" + hour;
		} else {
			hStr = "0" + hour;
		}
		if (min >= 10) {
			mStr = "" + min;
		} else {
			mStr = "0" + min;
		}
		return hStr + ":" + mStr;
	}

	// transfer helper methods
	// each of the below transfer the stated info from the ephemeris to the
	// given cell. each (except for /T,/L) attempts to transfer the number if it
	// does so successfully, it returns true. if the parse fails, it attempts to
	// transfer the string and return false. they all throw data missing exceptions

	private static boolean transferRA(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws 
			EphemerisDataMissingException {
		try {
			target.setCellValue(eph.getRightAcension());
			formatCell(p,target,5);
			return true;
		} catch (EphemerisDataParseException e) {
			target.setCellValue(eph.getRightAcensionStr());
			return false;
		}
	}

	private static boolean transferDEC(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {
			target.setCellValue(eph.getDeclination());
			formatCell(p,target,5);
			return true;
		} catch (EphemerisDataParseException e) {
			target.setCellValue(eph.getDeclinationStr());
			return false;
		}
	}

	private static boolean transferAzimuth(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {
			target.setCellValue(eph.getAzimuth());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {	
			target.setCellValue(eph.getAzimuthStr());
			return false;
		}
	}

	private static boolean transferElevation(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {	
			target.setCellValue(eph.getElevation());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {
			target.setCellValue(eph.getElevationStr());
			return false;
		}
	}

	private static boolean transferLST(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {	
			target.setCellValue(eph.getLocalSiderealTime());
			formatCell(p,target,4);
			return true;
		} catch (EphemerisDataParseException e) {	
			target.setCellValue(eph.getLocalSiderealTimeStr());
			return false;
		}
	}

	private static boolean transferAirmass(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {
			target.setCellValue(eph.getAirmass());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {
			target.setCellValue(eph.getAirmassStr());
			return false;
		}
	}

	private static boolean transferAppMag(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {	
			target.setCellValue(eph.getApparentMagnitude());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {	
			target.setCellValue(eph.getApparentMagnitudeStr());
			return false;
		}
	}

	private static boolean transferSurfBright(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {	
			target.setCellValue(eph.getSurfaceBrightness());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {	
			target.setCellValue(eph.getSurfaceBrightnessStr());
			return false;
		}
	}

	private static boolean transferFracIll(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {	
			target.setCellValue(eph.getFractionIlluminated());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {	
			target.setCellValue(eph.getFractionIlluminatedStr());
			return false;
		}
	}

	private static boolean transferDiameter(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {		
			target.setCellValue(eph.getAngularWidth());
			formatCell(p,target,1);
			return true;
		} catch (EphemerisDataParseException e) {		
			target.setCellValue(eph.getAngularWidthStr());
			return false;
		}
	}

	private static boolean transferObsLon(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {		
			target.setCellValue(eph.getTargetLongitude());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {	
			target.setCellValue(eph.getTargetLongitudeStr());
			return false;
		}
	}

	private static boolean transferObsLat(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {		
			target.setCellValue(eph.getTargetLatitude());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {		
			target.setCellValue(eph.getTargetLatitudeStr());
			return false;
		}
	}

	private static boolean transferSunLon(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {		
			target.setCellValue(eph.getSolarLongitude());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {		
			target.setCellValue(eph.getSolarLongitudeStr());
			return false;
		}
	}

	private static boolean transferSunLat(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {
			target.setCellValue(eph.getSolarLatitude());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {
			target.setCellValue(eph.getSolarLatitudeStr());
			return false;
		}
	}

	private static boolean transferR(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {
			target.setCellValue(eph.getSolarRange());
			formatCell(p,target,0);
			return true;
		} catch (EphemerisDataParseException e) {
			target.setCellValue(eph.getSolarRangeStr());
			return false;
		}
	}

	private static boolean transferRDot(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {
			target.setCellValue(eph.getSolarRangeRate());
			formatCell(p,target,3);
			return true;
		} catch (EphemerisDataParseException e) {
			target.setCellValue(eph.getSolarRangeRateStr());
			return false;
		}
	}

	private static boolean transferDelta(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {
			target.setCellValue(eph.getTargetRange());
			formatCell(p,target,0);
			return true;
		} catch (EphemerisDataParseException e) {
			target.setCellValue(eph.getTargetRangeStr());
			return false;
		}
	}

	private static boolean transferDeltaDot(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {
			target.setCellValue(eph.getTargetRangeRate());
			formatCell(p,target,3);
			return true;
		} catch (EphemerisDataParseException e) {
			target.setCellValue(eph.getTargetRangeRateStr());
			return false;
		}
	}

	private static boolean transferSOT(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {
			target.setCellValue(eph.getSunObserverTarget());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {
			target.setCellValue(eph.getSunObserverTargetStr());
			return false;
		}
	}

	private static void transferLOrT(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {

		target.setCellValue(eph.getSunLeadingTrailing());
		formatCell(p,target, 0);
	}

	private static boolean transferSTO(ExcelDataParser p, Ephemeris eph, 
			Cell target) throws EphemerisDataMissingException {
		try {
			target.setCellValue(eph.getSunTargetObserver());
			formatCell(p,target,2);
			return true;
		} catch (EphemerisDataParseException e) {
			target.setCellValue(eph.getSunTargetObserverStr());
			return false;
		}
	}
	
	private static boolean transferLunarCoords(ExcelDataParser p, 
			Ephemeris eph, Row im) throws 
			EphemerisDataMissingException {
		try {
			String craterName;
			double ewDist;
			double nsDist;
			String origin;
			double fov;
			
			Cell cName = im.getCell(p.INDICES[ExcelDataParser.OFF_CRAT]);
			Cell ew = im.getCell(p.INDICES[ExcelDataParser.OFF_EW_DIST]);
			Cell ns = im.getCell(p.INDICES[ExcelDataParser.OFF_NS_DIST]);
			Cell ori = im.getCell(p.INDICES[ExcelDataParser.OFF_ORIG]);
			Cell f = im.getCell(p.INDICES[ExcelDataParser.FOV]);
			
			Cell targetLon = im.getCell(p.INDICES[ExcelDataParser.FOV_LON]);
			Cell targetLat = im.getCell(p.INDICES[ExcelDataParser.FOV_LAT]);
			Cell targetAlt = im.getCell(p.INDICES[ExcelDataParser.FOV_ALT]);
			
			if (cName.getCellType() == Cell.CELL_TYPE_STRING) {
				craterName = cName.getStringCellValue();
			} else {
				throw new ExcelDataParserException("bad crater value");
			}
			
			if (craterName.equals("Moon Center")) return true;
			
			if (ori.getCellType() == Cell.CELL_TYPE_STRING) {
				origin = ori.getStringCellValue();
			} else {
				throw new ExcelDataParserException("bad origin value");
			}
			
			if (origin.equals("term")) return true;
			
			if (ew.getCellType() == Cell.CELL_TYPE_NUMERIC) {
				ewDist = ew.getNumericCellValue();
			} else {
				throw new ExcelDataParserException("bad e/w distance value");
			}
			
			if (ns.getCellType() == Cell.CELL_TYPE_NUMERIC) {
				nsDist = ns.getNumericCellValue();
			} else {
				throw new ExcelDataParserException("bad n/s distance value");
			}
			
			if (f.getCellType() == Cell.CELL_TYPE_NUMERIC) {
				fov = f.getNumericCellValue();
			} else {
				throw new ExcelDataParserException("bad fov value");
			}
			
			Double[] coords = eph.getLunarCoords(craterName, 
					ewDist, nsDist, origin, fov);
			
			targetLon.setCellValue(coords[0]);
			formatCell(p,targetLon,1);
			
			targetLat.setCellValue(coords[1]);
			formatCell(p,targetLat,1);
			
			targetAlt.setCellValue(coords[2]);
			formatCell(p,targetAlt,1);
			
			return true;
		} catch (EphemerisDataParseException e) {
			System.out.print(e.getMessage() + "...");
			throw new EphemerisDataMissingException();
		} catch (ExcelDataParserException e) {
			System.out.print(e.getMessage() + "...");
			throw new EphemerisDataMissingException();
		}
	}
	
	private static int getTimeInt(String timeStr) {
		int colonInd = timeStr.indexOf(':');
		return Integer.parseInt(timeStr.substring(0,colonInd) + 
				timeStr.substring(colonInd+1));
	}
	
	public static Date addDay(Date d) {
		return new Date(d.getTime() + 24*60*Ephemeris.MIL_PER_MIN);
	}
	
	/*
	private static void copyCellStyle(CellStyle s1, CellStyle s2, Workbook w) {
		s1.setAlignment(s2.getAlignment());
		s1.setBorderBottom(s2.getBorderBottom());
		s1.setBorderLeft(s2.getBorderLeft());
		s1.setBorderRight(s2.getBorderRight());
		s1.setBorderTop(s2.getBorderTop());
		s1.setBottomBorderColor(s2.getBottomBorderColor());
		s1.setDataFormat(s2.getDataFormat());
		s1.setFillBackgroundColor(s2.getFillBackgroundColor());
		s1.setFillForegroundColor(s2.getFillForegroundColor());
		s1.setFillPattern(s2.getFillPattern());
		//s1.setFont(w.getFontAt(s2.getFontIndex()));
		s1.setHidden(s2.getHidden());
		s1.setLocked(s2.getLocked());
		s1.setIndention(s2.getIndention());
		s1.setLeftBorderColor(s2.getLeftBorderColor());
		s1.setRightBorderColor(s2.getRightBorderColor());
		s1.setRotation(s2.getRotation());
		s1.setShrinkToFit(s2.getShrinkToFit());
		s1.setTopBorderColor(s2.getTopBorderColor());
		s1.setVerticalAlignment(s2.getVerticalAlignment());
		s1.setWrapText(s2.getWrapText());
	}
	*/
}
