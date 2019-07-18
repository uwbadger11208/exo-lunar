import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelDataParser {

	public final int[] INDICES;

	// Excel column headers (index for local code and
	// string for matching to spreadsheet
	//
	// STRING HERE MUST MATCH COLUMN HEADER IN SPREADSHEET
	// EXCEPT!!! THE SPREADSHEET CAN HAVE SPACES
	// (i.e. "Line Depth" in spreadsheet == "LineDepth" here)
	//
	// if Dona wants to do this, a smarter way of organizing
	// this which would allow for an easy looping through of columns
	// would be to define each of these as a struct or watever and
	// then explictly instantiating an array of them

	/** Object */
	public static final int OBJECT = 0;
	public static final String OBJECT_S = "Object";

	/** File stem */
	public static final int FILE = 1;
	public static final String FILE_S = "File";

	/** Time exposure was taken */
	public static final int TIME = 2;
	public static final String TIME_S = "Time";
	
	/** Length of exposure */
	public static final int EXP_TIME = 3;
	public static final String EXP_TIME_S = "Expo";

	/** Right ascension */
	public static final int RA = 4;
	public static final String RA_S = "RA";

	/** Declination */
	public static final int DEC = 5;
	public static final String DEC_S = "DEC";

	/** Azimuth */
	public static final int AZI = 6;
	public static final String AZI_S = "Azimuth";

	/** Elevation */
	public static final int ELEV = 7;
	public static final String ELEV_S = "Elevation";

	/** Lunar standard time */
	public static final int LST = 8;
	public static final String LST_S = "LST";

	/** Air mass */
	public static final int A_MASS = 9;
	public static final String A_MASS_S = "AM";

	/** Apparent magnitude */
	public static final int AP_MAG = 10;
	public static final String AP_MAG_S = "Mv";

	/** Surface brightness */
	public static final int SURF_BRT = 11;
	public static final String SURF_BRT_S = "S_Brt";

	/** Fraction of illumination */
	public static final int FRAC_ILL = 12;
	public static final String FRAC_ILL_S = "Illum";

	/** Angular diameter */
	public static final int ANG_WID = 13;
	public static final String ANG_WID_S = "Ang_Dia";

	/** Observer longitude */
	public static final int TAR_LON = 14;
	public static final String TAR_LON_S = "Obs_Long";

	/** Observer latitude */
	public static final int TAR_LAT = 15;
	public static final String TAR_LAT_S = "Obs_Lat";

	/** Solar longitude */
	public static final int SOL_LON = 16;
	public static final String SOL_LON_S = "S_Long";

	/** Solar latitude */
	public static final int SOL_LAT = 17;
	public static final String SOL_LAT_S = "S_Lat";

	/** Sun-moon distance */
	public static final int R = 18;
	public static final String R_S = "r";

	/** Sun-moon velocity */
	public static final int R_DOT = 19;
	public static final String R_DOT_S = "rdot";

	/** Moon-earth distance */
	public static final int DELTA = 20;
	public static final String DELTA_S = "delta";

	/** Moon-earth velocity  */
	public static final int DELTA_DOT = 21;
	public static final String DELTA_DOT_S = "deldot";

	/** S-O-T angle */
	public static final int SOT = 22;
	public static final String SOT_S = "S-O-T";

	/** L or T */
	public static final int L_OR_T = 23;
	public static final String L_OR_T_S = "L/T";

	/** Phase angle */
	public static final int STO = 24;
	public static final String STO_S = "Phase";

	/** Crater */
	public static final int OFF_CRAT = 25;
	public static final String OFF_CRAT_S = "Crater";

	/** East-west offset */
	public static final int OFF_EW_DIST = 26;
	public static final String OFF_EW_DIST_S = "Offset_EW";

	/** North-south offset */
	public static final int OFF_NS_DIST = 27;
	public static final String OFF_NS_DIST_S = "Offset_NS";

	/** Origin of offset */
	public static final int OFF_ORIG = 28;
	public static final String OFF_ORIG_S = "Origin";

	/** Depth of Fraunhofer line */
	public static final int LINE_DEPTH = 29;
	public static final String LINE_DEPTH_S = "Line_Depth";

	/** Lunar longtiude */
	public static final int FOV_LON = 30;
	public static final String FOV_LON_S = "Moon_Long";

	/** Lunar latitude */
	public static final int FOV_LAT = 31;
	public static final String FOV_LAT_S = "Moon_Lat";

	/** Lunar altitude */
	public static final int FOV_ALT = 32;
	public static final String FOV_ALT_S = "Alt";

	/** Field of view width */
	public static final int FOV = 33;
	public static final String FOV_S = "FOV";

	/** Filter */
	public static final int FILTER = 34;
	public static final String FILTER_S = "Filter";
	
	/** Wavelength */
	public static final int WAVELENGTH = 35;
	public static final String WAVELENGTH_S = "Wavelength";

	/** number of columns */
	public static final int N_PARAMS = 36;
	/** dummy blank cell type */
	public static final int BLANK_CELL = -1;
	
	private List<CellStyle> styles;
	public ExcelDataParser(Sheet log) throws ExcelDataParserException {

		// check for null
		if (log == null)
			throw new IllegalArgumentException();
		
		log.getWorkbook().setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
		
		Row headers = null; // headers row

		// find headers row by checking for the Object header
		int[] cols2check = {0,1}; // check first two rows

		for (Row r : log) {

			for (int j=0;j<cols2check.length;j++)
				if (getInfoIndex(r.getCell(j)) == OBJECT) {
					headers = r;
					break;
			}
		}

		// check that it was found
		if (headers == null)
			throw new ExcelDataParserException("No headers found");

		// instantiate indices
		INDICES = new int[N_PARAMS];
		for (int i = 0; i < N_PARAMS; i++)
			INDICES[i] = -1;

		// go through headers, finding column indices
		int numOfCols = Math.max(headers.getLastCellNum(), 
				log.getRow(headers.getRowNum() + 1).getLastCellNum());
		for (int i = 0; i < numOfCols; i++) {
			Cell c = headers.getCell(i);
			int info = getInfoIndex(c);
			if (info != BLANK_CELL) {
				if (INDICES[info] != -1) {
					throw new ExcelDataParserException("Duplicate header");
				}
				INDICES[info] = c.getColumnIndex();
			}
		}

		// check that all elements of INDICES were assigned
		int numUnassigned = 0;
		
		for (int i = 0; i < N_PARAMS; i++) {
			if (INDICES[i] == -1)
				numUnassigned++;
		}
		
		if (numUnassigned != 0) {
			String message = "Not all headers found. Missing indices are: \n";
			
			if (INDICES[OBJECT] == -1)
				message += (OBJECT_S+"\n");
			if (INDICES[FILE] == -1)
				message += (FILE_S+"\n");
			if (INDICES[TIME] == -1)
				message += (TIME_S+"\n");
			if (INDICES[EXP_TIME] == -1)
				message += (EXP_TIME_S+"\n");
			if (INDICES[RA] == -1)
				message += (RA_S+"\n");
			if (INDICES[DEC] == -1)
				message += (DEC_S+"\n");
			if (INDICES[AZI] == -1)
				message += (AZI_S+"\n");
			if (INDICES[ELEV] == -1)
				message += (ELEV_S+"\n");
			if (INDICES[LST] == -1)
				message += (LST_S+"\n");
			if (INDICES[A_MASS] == -1)
				message += (A_MASS_S+"\n");
			if (INDICES[AP_MAG] == -1)
				message += (AP_MAG_S+"\n");
			if (INDICES[SURF_BRT] == -1)
				message += (SURF_BRT_S+"\n");
			if (INDICES[FRAC_ILL] == -1)
				message += (FRAC_ILL_S+"\n");
			if (INDICES[ANG_WID] == -1)
				message += (ANG_WID_S+"\n");
			if (INDICES[TAR_LON] == -1)
				message += (TAR_LON_S+"\n");
			if (INDICES[TAR_LAT] == -1)
				message += (TAR_LAT_S+"\n");
			if (INDICES[SOL_LON] == -1)
				message += (SOL_LON_S+"\n");
			if (INDICES[SOL_LAT] == -1)
				message += (SOL_LAT_S+"\n");
			if (INDICES[R] == -1)
				message += (R_S+"\n");
			if (INDICES[R_DOT] == -1)
				message += (R_DOT_S+"\n");
			if (INDICES[DELTA] == -1)
				message += (DELTA_S+"\n");
			if (INDICES[DELTA_DOT] == -1)
				message += (DELTA_DOT_S+"\n");
			if (INDICES[SOT] == -1)
				message += (SOT_S+"\n");
			if (INDICES[L_OR_T] == -1)
				message += (L_OR_T_S+"\n");
			if (INDICES[STO] == -1)
				message += (STO_S+"\n");
			if (INDICES[OFF_CRAT] == -1)
				message += (OFF_CRAT_S+"\n");
			if (INDICES[OFF_EW_DIST] == -1)
				message += (OFF_EW_DIST_S+"\n");
			if (INDICES[OFF_NS_DIST] == -1)
				message += (OFF_NS_DIST_S+"\n");
			if (INDICES[OFF_ORIG] == -1)
				message += (OFF_ORIG_S+"\n");
			if (INDICES[LINE_DEPTH] == -1)
				message += (LINE_DEPTH_S+"\n");
			if (INDICES[FOV_LON] == -1)
				message += (FOV_LON_S+"\n");
			if (INDICES[FOV_LAT] == -1)
				message += (FOV_LAT_S+"\n");
			if (INDICES[FOV_ALT] == -1)
				message += (FOV_ALT_S+"\n");
			if (INDICES[FOV] == -1)
				message += (FOV_S+"\n");
			if (INDICES[FILTER] == -1)
				message += (FILTER_S+"\n");
			if (INDICES[WAVELENGTH] == -1)
				message += (WAVELENGTH_S+"\n");

			throw new ExcelDataParserException(message);
		}
		
		// create style and format
		styles = new ArrayList<CellStyle>();
		for (int decs = 0; decs < 10; decs++) {
			CellStyle style = log.getWorkbook().createCellStyle();
			DataFormat format = log.getWorkbook().createDataFormat();
			
			String formatStr = "#,###,###,##0";
			if (decs > 0)
				formatStr += ".";
			for (int i = 0; i < decs; i++) {
				formatStr += "0";
			}
			style.setDataFormat(format.getFormat(formatStr));
			style.setAlignment(HorizontalAlignment.CENTER);
			styles.add(decs,style);
		}
	}

	/**
	 * Private helper method reads in a cell and returns the index code for
	 * either one of the ephemeris info categories or a blank cell
	 * @param c the cell to be examined
	 * @return the index code of whether the cell contains (and if so which
	 * category it is of) the ephemeris info category header 
	 */
	private static int getInfoIndex(Cell c) {

		// check for null
		if (c == null)
			return BLANK_CELL;

		// get cell's row
		Row r = c.getRow();

		// get neighborhood cells
		Cell below = null; // right below
		Cell before = null; // right before

		// get row below. if cell is in last row, return blank
		if (r.getRowNum() != r.getSheet().getLastRowNum()) {

			// get cell before. check for null row. if it's null, return blank
			Row oneLower = r.getSheet().getRow(r.getRowNum() + 1);
			if (oneLower != null) {
				below = oneLower.getCell(c.getColumnIndex());
			} else {
				return BLANK_CELL;
			}
		} else {
			return BLANK_CELL;
		}

		// get cell before
		if (c.getColumnIndex() != 0) {
			before = r.getCell(c.getColumnIndex() - 1);
		} else {
			before = null;
		}

		// String to test
		String header = null;

		// do different things depending on type of cell
		switch (c.getCellType()) {

		case BLANK:

			// if blank, get text from cell below
			if (below.getCellType() == CellType.STRING) {
				header = below.getStringCellValue().replaceAll("\\s","");
			} else {
				return BLANK_CELL;
			}

		case STRING:

			// if string, check if already have header. if not...
			if (header == null) {

				// get cell value
				header = c.getStringCellValue().replaceAll("\\s","");

				/* probably don't need to get value on bottom unless top
				 * value is missing, but I'll keep code in here just in case
				 *
				 * // check if more on bottom
				 * if (below != null) {
				 *   	if (below.getCellType() == CellType.STRING) {
				 *	    	header += " " + below.getStringCellValue();
				 *	    }
				 * }
				 */
			} 

			// now go through possible matches and return appropriate char

			// object name
			if (header.equals(OBJECT_S)) {
				return OBJECT;

				// file/image name
			} else if (header.equals(FILE_S)) {
				return FILE;

				// time taken
			} else if (header.equals(TIME_S)) {
				return TIME;
				
			} else if (header.equals(FOV_S)) {
				return FOV;

				// length of exposure
			} else if (header.equals(EXP_TIME_S)) {
				return EXP_TIME;

				// right acension
			} else if (header.equals(RA_S)) {
				return RA;

				// declination
			} else if (header.equals(DEC_S)) {
				return DEC;

				// azimuthal angle
			} else if (header.equals(AZI_S)) {
				return AZI;

				// elevation angle
			} else if (header.equals(ELEV_S)) {
				return ELEV;

				// local sidereal time
			} else if (header.equals(LST_S)) {
				return LST;

				// airmass
			} else if (header.equals(A_MASS_S)) {
				return A_MASS;

				// apparent magnitude
			} else if (header.equals(AP_MAG_S)) {
				return AP_MAG;

				// surface brightness
			} else if (header.equals(SURF_BRT_S)) {
				return SURF_BRT;

				// illuminated fraction
			} else if (header.equals(FRAC_ILL_S)) {
				return FRAC_ILL;

				// angular width/diameter
			} else if (header.equals(ANG_WID_S)) {
				return ANG_WID;

				// observer longitude
			} else if (header.equals(TAR_LON_S)) {
				return TAR_LON;

				// either latitude
			} else if (header.equals(TAR_LAT_S)) {
				return TAR_LAT;
				/*
				 
				   // NO LONGER NEED TO DO THIS BECAUSE
				   // EACH IS ITS OWN COL WITH HEADER

				// check for null
				if (before != null) {

					// check for string type
					if (before.getCellType() == CellType.STRING) {
						String beforeStr = before.getStringCellValue().replaceAll("\\s","");

						// get which latitude it is
						if (beforeStr.equals("Observer:Sub-")) {
							return TAR_LAT;
						} else if (beforeStr.equals("Solar:Sub-")) {
							return SOL_LAT;
						} else if (beforeStr.equals("FOVLocation")) {
							return FOV_LAT;
						} else {
							return BLANK_CELL;
						}
					}
				} else {
					return BLANK_CELL;
				}
				*/

				// solar longitude
			} else if (header.equals(SOL_LON_S)) {
				return SOL_LON;

				// solar latitude
			} else if (header.equals(SOL_LAT_S)) {
				return SOL_LAT;

				// solar range
			} else if (header.equals(R_S)) {
				return R;

				// solar range rate
			} else if (header.equals(R_DOT_S)) {
				return R_DOT;

				// observer range
			} else if (header.equals(DELTA_S)) {
				return DELTA;

				// observer range rate
			} else if (header.equals(DELTA_DOT_S)) {
				return DELTA_DOT;

				// sun-observer-target
			} else if (header.equals(SOT_S)) {
				return SOT;

				// leading or trailing
			} else if (header.equals(L_OR_T_S)) {
				return L_OR_T;

				// sun-target-observer
			} else if (header.equals(STO_S)) {
				return STO;
				
			} else if (header.equals(OFF_CRAT_S)) {
				return OFF_CRAT;
				
			} else if (header.equals(OFF_EW_DIST_S)) {
				return OFF_EW_DIST;
				
			} else if (header.equals(OFF_NS_DIST_S)) {
				return OFF_NS_DIST;
				
			} else if (header.equals(OFF_ORIG_S)) {
				return OFF_ORIG;
				
			} else if (header.equals(LINE_DEPTH_S)) {
				return LINE_DEPTH;
				
			} else if (header.equals(FOV_LON_S)) {
				return FOV_LON;

			} else if (header.equals(FOV_LAT_S)) {
				return FOV_LAT;
				
			} else if (header.equals(FOV_ALT_S)) {
				return FOV_ALT;
			
			} else if (header.equals(FILTER_S)) {
				return FILTER;
				
			} else if (header.equals(WAVELENGTH_S)) {
				return WAVELENGTH;

				// everything else
			} else {
				return BLANK_CELL;
			}

		case NUMERIC: 

		case FORMULA: 

		default: return BLANK_CELL;

		}
	}
	
	public CellStyle getStyle(int decPlaces) {
		return styles.get(decPlaces);
	}

	public static void main(String[] args) {
		try {
			System.out.println("starting input stream...");
			InputStream in = new FileInputStream("lib/Lunar_Log_Summary_2013"
					+ "_final_062514.xlsx");
			System.out.println("done");
			System.out.println("starting workbook...");
			Workbook wb = new XSSFWorkbook(in);
			System.out.println("done");
			Sheet dayOne = wb.getSheet("13May20");
			if (dayOne != null) {
				System.out.println("starting excel data parser...");
				ExcelDataParser parser = new ExcelDataParser(dayOne);
				System.out.println("done");
				
				System.out.println("printing collected indices");
				for (Integer i : parser.INDICES)
					System.out.println(i);
			} else {
				System.out.println("Sheet is null");
			}
			wb.close();
		} catch (IOException e) {
			System.out.println("IOException");
		} catch (ExcelDataParserException e) {
			System.out.println("EDPException");
			System.out.println(e.getMessage());
		}
	}
}
