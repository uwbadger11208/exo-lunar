import java.io.File;
import java.io.IOException;
import java.util.Date;


public class LibrationEphemeris {
	
	private AxisEphemeris z;
	private AxisEphemeris y;
	private AxisEphemeris origin;
	
	public LibrationEphemeris(Date[] dates) throws IOException {
		y = new AxisEphemeris(dates,AxisEphemeris.Y);
		z = new AxisEphemeris(dates,AxisEphemeris.Z);
		origin = new AxisEphemeris(dates,AxisEphemeris.O);
	}
	
	public Matrix coordTrans() throws EphemerisDataParseException, 
		EphemerisDataMissingException {
		Vector center = new Vector(origin.getDeclination(),
				origin.getRightAcension()).scale(origin.getTargetRange());
		Vector yFromEarth = new Vector(y.getDeclination(),
				y.getRightAcension()).scale(y.getTargetRange());
		Vector zFromEarth = new Vector(z.getDeclination(),
				z.getRightAcension()).scale(z.getTargetRange());
		
		Vector yHat = yFromEarth.plus(center.negative());
		yHat = yHat.scale(1 / yHat.norm());
		
		Vector zHat = zFromEarth.plus(center.negative());
		zHat = zHat.scale(1 / zHat.norm());
		
		Vector xHat = yHat.cross(zHat);
		
		Vector[] cols = new Vector[3];
		cols[0] = xHat;
		cols[1] = yHat;
		cols[2] = zHat;
		boolean isRows = false;
		
		return new Matrix(cols,isRows);
	}
}
