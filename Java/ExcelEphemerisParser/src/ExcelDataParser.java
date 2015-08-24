import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelDataParser {

	public final int[] INDICES;

	public static final int OBJECT = 0;
	public static final int FILE = 1;
	public static final int TIME = 2;
	public static final int EXP_TIME = 3;
	public static final int RA = 4;
	public static final int DEC = 5;
	public static final int AZI = 6;
	public static final int ELEV = 7;
	public static final int LST = 8;
	public static final int A_MASS = 9;
	public static final int AP_MAG = 10;
	public static final int SURF_BRT = 11;
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
	public static final int OFF_CRAT = 25;
	public static final int OFF_EW_DIST = 26;
	public static final int OFF_NS_DIST = 27;
	public static final int OFF_ORIG = 28;
	public static final int LINE_DEPTH = 29;
	public static final int FOV_LON = 30;
	public static final int FOV_LAT = 31;
	public static final int FOV_ALT = 32;
	public static final int FOV = 33;
	public static final int N_PARAMS = 34;

	public static final int BLANK = -1;
	
	private List<CellStyle> styles;
	public ExcelDataParser(Sheet log) throws ExcelDataParserException {

		// check for null
		if (log == null)
			throw new IllegalArgumentException();
		
		log.getWorkbook().setMissingCellPolicy(Row.CREATE_NULL_AS_BLANK);
		
		Row headers = null; // headers row

		// find headers row
		for (Row r : log) {
			if (getInfoIndex(r.getCell(0)) == OBJECT) {
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
			if (info != BLANK) {
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
				message += "Object\n";
			if (INDICES[FILE] == -1)
				message += "File\n";
			if (INDICES[TIME] == -1)
				message += "Time\n";
			if (INDICES[EXP_TIME] == -1)
				message += "Expo\n";
			if (INDICES[RA] == -1)
				message += "RA\n";
			if (INDICES[DEC] == -1)
				message += "DEC\n";
			if (INDICES[AZI] == -1)
				message += "Azimuth\n";
			if (INDICES[ELEV] == -1)
				message += "Elevation\n";
			if (INDICES[LST] == -1)
				message += "LST\n";
			if (INDICES[A_MASS] == -1)
				message += "AM\n";
			if (INDICES[AP_MAG] == -1)
				message += "Mv\n";
			if (INDICES[SURF_BRT] == -1)
				message += "Surface Brightness\n";
			if (INDICES[FRAC_ILL] == -1)
				message += "Illuminated Fraction\n";
			if (INDICES[ANG_WID] == -1)
				message += "Diameter\n";
			if (INDICES[TAR_LON] == -1)
				message += "Observer: Sub-Longitude\n";
			if (INDICES[TAR_LAT] == -1)
				message += "Observer: Sub-Latitude\n";
			if (INDICES[SOL_LON] == -1)
				message += "Solar: Sub-Longitude\n";
			if (INDICES[SOL_LAT] == -1)
				message += "Solar: Sub-Latitude\n";
			if (INDICES[R] == -1)
				message += "r\n";
			if (INDICES[R_DOT] == -1)
				message += "rdot\n";
			if (INDICES[DELTA] == -1)
				message += "delta\n";
			if (INDICES[DELTA_DOT] == -1)
				message += "deldot\n";
			if (INDICES[SOT] == -1)
				message += "S-O-T\n";
			if (INDICES[L_OR_T] == -1)
				message += "L/T\n";
			if (INDICES[STO] == -1)
				message += "S-T-O\n";
			if (INDICES[OFF_CRAT] == -1)
				message += "Offset Crater\n";
			if (INDICES[OFF_EW_DIST] == -1)
				message += "Offset E/W Dist\n";
			if (INDICES[OFF_NS_DIST] == -1)
				message += "Offset N/S Dist\n";
			if (INDICES[OFF_ORIG] == -1)
				message += "Offset Origin\n";
			if (INDICES[LINE_DEPTH] == -1)
				message += "Line Depth\n";
			if (INDICES[FOV_LON] == -1)
				message += "FOV Longitude\n";
			if (INDICES[FOV_LAT] == -1)
				message += "FOV Latitude\n";
			if (INDICES[FOV_ALT] == -1)
				message += "FOV Altitude\n";
			if (INDICES[FOV] == -1)
				message += "FOV\n";

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
			style.setAlignment(CellStyle.ALIGN_CENTER);
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
			return BLANK;

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
				return BLANK;
			}
		} else {
			return BLANK;
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

		case Cell.CELL_TYPE_BLANK:

			// if blank, get text from cell below
			if (below.getCellType() == Cell.CELL_TYPE_STRING) {
				header = below.getStringCellValue().replaceAll("\\s","");
			} else {
				return BLANK;
			}

		case Cell.CELL_TYPE_STRING:

			// if string, check if already have header. if not...
			if (header == null) {

				// get cell value
				header = c.getStringCellValue().replaceAll("\\s","");

				/* probably don't need to get value on bottom unless top
				 * value is missing, but I'll keep code in here just in case
				 *
				 * // check if more on bottom
				 * if (below != null) {
				 *   	if (below.getCellType() == Cell.CELL_TYPE_STRING) {
				 *	    	header += " " + below.getStringCellValue();
				 *	    }
				 * }
				 */
			} 

			// now go through possible matches and return appropriate char

			// object name
			if (header.equals("Object")) {
				return OBJECT;

				// file/image name
			} else if (header.equals("File")) {
				return FILE;

				// time taken
			} else if (header.equals("Time")) {
				return TIME;
				
			} else if (header.equals("FOV")) {
				return FOV;

				// length of exposure
			} else if (header.equals("Expo")) {
				return EXP_TIME;

				// right acension
			} else if (header.equals("RA")) {
				return RA;

				// declination
			} else if (header.equals("DEC")) {
				return DEC;

				// azimuthal angle
			} else if (header.equals("Azimuth")) {
				return AZI;

				// elevation angle
			} else if (header.equals("Elevation")) {
				return ELEV;

				// local sidereal time
			} else if (header.equals("LST")) {
				return LST;

				// airmass
			} else if (header.equals("AM")) {
				return A_MASS;

				// apparent magnitude
			} else if (header.equals("Mv")) {
				return AP_MAG;

				// surface brightness
			} else if (header.equals("Surface")) {
				return SURF_BRT;

				// illuminated fraction
			} else if (header.equals("Illuminated")) {
				return FRAC_ILL;

				// angular width/diameter
			} else if (header.equals("Diameter")) {
				return ANG_WID;

				// observer longitude
			} else if (header.equals("Observer:Sub-")) {
				return TAR_LON;

				// either latitude
			} else if (header.equals("Latitude")) {

				// check for null
				if (before != null) {

					// check for string type
					if (before.getCellType() == Cell.CELL_TYPE_STRING) {
						String beforeStr = before.getStringCellValue().replaceAll("\\s","");

						// get which latitude it is
						if (beforeStr.equals("Observer:Sub-")) {
							return TAR_LAT;
						} else if (beforeStr.equals("Solar:Sub-")) {
							return SOL_LAT;
						} else if (beforeStr.equals("FOVLocation")) {
							return FOV_LAT;
						} else {
							return BLANK;
						}
					}
				} else {
					return BLANK;
				}

				// solar longitude
			} else if (header.equals("Solar:Sub-")) {
				return SOL_LON;

				// solar range
			} else if (header.equals("r")) {
				return R;

				// solar range rate
			} else if (header.equals("rdot")) {
				return R_DOT;

				// observer range
			} else if (header.equals("delta")) {
				return DELTA;

				// observer range rate
			} else if (header.equals("deldot")) {
				return DELTA_DOT;

				// sun-observer-target
			} else if (header.equals("S-O-T")) {
				return SOT;

				// leading or trailing
			} else if (header.equals("L/T")) {
				return L_OR_T;

				// sun-target-observer
			} else if (header.equals("S-T-O")) {
				return STO;
				
			} else if (header.equals("Offset")) {
				return OFF_CRAT;
				
			} else if (header.equals("E/WDist")) {
				return OFF_EW_DIST;
				
			} else if (header.equals("N/SDist")) {
				return OFF_NS_DIST;
				
			} else if (header.equals("Origin")) {
				return OFF_ORIG;
				
			} else if (header.equals("Line")) {
				return LINE_DEPTH;
				
			} else if (header.equals("FOVLocation")) {
				return FOV_LON;
				
			} else if (header.equals("Altitude(km)")) {
				return FOV_ALT;

				// everything else
			} else {
				return BLANK;
			}

		case Cell.CELL_TYPE_NUMERIC: 

		case Cell.CELL_TYPE_FORMULA: 

		default: return BLANK;

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
