import java.io.IOException;
import java.util.Date;
import java.util.HashMap;


public class LibrationEphemeris {
	
	public static final double LUNAR_RADIUS = 1737.53; // in kilometers
	public static final double LENS_ANG_DIAM = 412.5; // in arcsec
	public static final double APER_DIAM = 180; // arcsec
	private AxisEphemeris x;
	private AxisEphemeris z;
	private HashMap<String,Double[]> craterCoords;
	
	public LibrationEphemeris(Date[] dates) throws IOException {
		z = new AxisEphemeris(dates,AxisEphemeris.Z);
		x = new AxisEphemeris(dates,AxisEphemeris.X);
		
		craterCoords = getCraterMap();
	}
	
	public Matrix coordTrans(double ra, double dec, double delta) 
			throws EphemerisDataParseException, 
		EphemerisDataMissingException {
		Vector center = new Vector(dec*Math.PI/180,ra*Math.PI/180).scale(delta);
		Vector zFromEarth = new Vector(z.getDeclination()*Math.PI/180,
				z.getRightAcension()*Math.PI/180).scale(z.getTargetRange());
		Vector xFromEarth = new Vector(x.getDeclination()*Math.PI/180,
				x.getRightAcension()*Math.PI/180).scale(x.getTargetRange());
		
		Vector zHat = zFromEarth.plus(center.negative());
		zHat = zHat.scale(1 / zHat.norm());
		
		Vector xHat = xFromEarth.plus(center.negative());
		xHat = xHat.scale(1 / xHat.norm());
		
		Vector yHat = zHat.cross(xHat);
		
		Vector[] cols = new Vector[3];
		cols[0] = xHat;
		cols[1] = yHat;
		cols[2] = zHat;
		boolean isRows = false;
		
		return new Matrix(cols,isRows);
	}
	
	public boolean advance() {
		boolean zVar = z.advance();
		boolean xVar = x.advance();
		return xVar && zVar;
	}
	
	public HashMap<String,Double[]> getCraterCoords() {
		return craterCoords;
	}
	
	private static HashMap<String,Double[]> getCraterMap() {
		HashMap<String,Double[]> craters = new HashMap<String,Double[]>();
		//          crater name                 long (e+), lat (n+) 
		craters.put("Langrenus", new Double[]   	{ 60.9, -8.9});
		craters.put("Cleomedes", new Double[]   	{ 55.5, 27.7});
		craters.put("Petavius", new Double[]    	{ 60.4,-25.3});
		craters.put("Grimaldi", new Double[]    	{-68.8, -5.2}); // 291.2
		craters.put("Aristarchus", new Double[] 	{-47.4, 23.7}); // 312.6
		craters.put("Tycho", new Double[]       	{-11.4,-43.3}); // 348.6
		craters.put("Plato", new Double[]       	{ -9.3, 51.6}); // 350.7
		craters.put("Apollonius", new Double[]  	{ 61.1,  4.5});
		craters.put("Endymion", new Double[]    	{ 56.5, 53.6});
		craters.put("Messala", new Double[]     	{ 59.9, 39.2});
		craters.put("Atlas", new Double[]       	{ 44.4, 46.7});
		craters.put("Janssen", new Double[]     	{ 40.8,-45.0});
		craters.put("Ptolemaeus", new Double[]  	{ -1.8, -9.2}); // 358.2
		craters.put("Kepler", new Double[]      	{-38.0,  8.1}); // 322.0
		craters.put("Copernicus", new Double[]  	{-20.1,  9.6}); // 339.9
		craters.put("Gassendi", new Double[]    	{-40.0,-17.6}); // 320.0
		craters.put("Mare Iridum", new Double[] 	{-31.5, 44.1}); // 328.5
		craters.put("Theophilus", new Double[]  	{ 26.4,-11.4});
		craters.put("Godin", new Double[]       	{ 10.2,  1.8});
		craters.put("Vieta", new Double[]       	{-56.3,-29.2}); // 303.7
		craters.put("Furnerius", new Double[]   	{ 60.4,-36.3});
		craters.put("Schickard", new Double[]   	{-54.6,-44.4});
		craters.put("Proclus", new Double[]     	{ 46.8, 16.1});
		craters.put("Stevinus", new Double[]		{ 54.2,-32.5});
		craters.put("Aristoteles", new Double[] 	{ 17.4, 50.2});
		craters.put("Mons Gruithuisen", new Double[]{-40.5, 36.6}); 
		craters.put("Scheiner", new Double[]		{-27.8,-60.5});
		craters.put("Moon center", new Double[] 	{  0.0,  0.0});
		
		return craters;
	}
}
