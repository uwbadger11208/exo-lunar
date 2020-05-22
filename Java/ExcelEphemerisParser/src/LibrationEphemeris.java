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
		craters.put("langrenus", new Double[]   	{ 60.9, -8.9});
		craters.put("cleomedes", new Double[]   	{ 55.5, 27.7});
		craters.put("petavius", new Double[]    	{ 60.4,-25.3});
		craters.put("grimaldi", new Double[]    	{-68.8, -5.2}); // 291.2
		craters.put("aristarchus", new Double[] 	{-47.4, 23.7}); // 312.6
		craters.put("tycho", new Double[]       	{-11.4,-43.3}); // 348.6
		craters.put("plato", new Double[]       	{ -9.3, 51.6}); // 350.7
		craters.put("apollonius", new Double[]  	{ 61.1,  4.5});
		craters.put("endymion", new Double[]    	{ 56.5, 53.6});
		craters.put("messala", new Double[]     	{ 59.9, 39.2});
		craters.put("atlas", new Double[]       	{ 44.4, 46.7});
		craters.put("janssen", new Double[]     	{ 40.8,-45.0});
		craters.put("ptolemaeus", new Double[]  	{ -1.8, -9.2}); // 358.2
		craters.put("kepler", new Double[]      	{-38.0,  8.1}); // 322.0
		craters.put("copernicus", new Double[]  	{-20.1,  9.6}); // 339.9
		craters.put("gassendi", new Double[]    	{-40.0,-17.6}); // 320.0
		craters.put("mare iridum", new Double[] 	{-31.5, 44.1}); // 328.5
		craters.put("theophilus", new Double[]  	{ 26.4,-11.4});
		craters.put("godin", new Double[]       	{ 10.2,  1.8});
		craters.put("vieta", new Double[]       	{-56.3,-29.2}); // 303.7
		craters.put("furnerius", new Double[]   	{ 60.4,-36.3});
		craters.put("schickard", new Double[]   	{-54.6,-44.4});
		craters.put("proclus", new Double[]     	{ 46.8, 16.1});
		craters.put("stevinus", new Double[]		{ 54.2,-32.5});
		craters.put("aristoteles", new Double[] 	{ 17.4, 50.2});
		craters.put("mons gruithuisen", new Double[]{-40.5, 36.6}); 
		craters.put("scheiner", new Double[]		{-27.8,-60.5});
		craters.put("moon center", new Double[] 	{  0.0,  0.0});
		craters.put("mons rumker", new Double[]     {-58.1, 40.8});
		craters.put("w equator", new Double[]       { 55.2,  4.6});
		
		return craters;
	}
}
