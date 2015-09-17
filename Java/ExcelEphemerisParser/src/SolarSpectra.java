
public class SolarSpectra {

	private double[][] na;
	private double[][] k;
	
	public static final double SOLAR_REDSHIFT = 0.636;
	
	public static final int VELOCITY = 0;
	public static final int AMPLITUDE = 1;
	
	public SolarSpectra() {
		na = new double[][]{
			{-4.69523302,0.1653},
			{-4.33894020,0.1445},
			{-3.97755748,0.1225},
			{-3.61617477,0.1062},
			{-3.25988195,0.0918},
			{-2.89849923,0.0799},
			{-2.53711652,0.0710},
			{-2.18082370,0.0643},
			{-1.81944098,0.0586},
			{-1.45805827,0.0544},
			{-1.10176545,0.0517},
			{-0.74038273,0.0502},
			{-0.37900002,0.0505},
			{-0.02270720,0.0506},
			{0.33867552,0.0500},
			{0.70005823,0.0504},
			{1.05635105,0.0517},
			{1.41773377,0.0547},
			{1.77911648,0.0595},
			{2.13540930,0.0640},
			{2.49679202,0.0697},
			{2.85817473,0.0786},
			{3.21955745,0.0887},
			{3.57585027,0.1053},
			{3.93723298,0.1214},
			{4.29861570,0.1410},
			{4.65490852,0.1641}
		};
		
		k = new double[][]{
			{-4.97679434,0.7927},
			{-4.61855258,0.7499},
			{-4.26031083,0.7090},
			{-3.89817515,0.6515},
			{-3.53993339,0.5963},
			{-3.17779771,0.5385},
			{-2.81955595,0.4794},
			{-2.46131420,0.4203},
			{-2.09917851,0.3639},
			{-1.74093676,0.3120},
			{-1.37880108,0.2694},
			{-1.02055932,0.2380},
			{-0.66231757,0.2143},
			{-0.30018188,0.1989},
			{0.05805987,0.1933},
			{0.41630162,0.1978},
			{0.77843731,0.2115},
			{1.13667906,0.2357},
			{1.49881475,0.2705},
			{1.85705650,0.3138},
			{2.21529826,0.3721},
			{2.57743394,0.4350},
			{2.93567569,0.4980},
			{3.29781138,0.5631},
			{3.65605313,0.6253},
			{4.01429489,0.6806},
			{4.37643057,0.7303},
			{4.73467233,0.7737}
		};
	}
	
	public double kLineDepth(double moonSun) throws
	BadTransferException {
		return lineDepth(k,moonSun);
	}
	
	public double naLineDepth(double moonSun) throws
	BadTransferException {
		return lineDepth(na,moonSun);
	}
	
	private double lineDepth(double[][] array, double moonSun) throws
	BadTransferException {
		int i = 0;
		while (!(array[i][VELOCITY] < (moonSun + SOLAR_REDSHIFT) && 
				array[i+1][VELOCITY] > (moonSun + SOLAR_REDSHIFT) && i <= array.length - 2)) {
			i++;
		}
		if (i == array.length - 1)
			throw new BadTransferException("Moon-Sun Velocity out of range (+/- 5 km/s)");
		
		double minVel = array[i][VELOCITY];
		double maxVel = array[i+1][VELOCITY];
		
		double minAmp = array[i][AMPLITUDE];
		double maxAmp = array[i+1][AMPLITUDE];
		
		double slope = (maxAmp - minAmp) / (maxVel - minVel);
		double diff = SOLAR_REDSHIFT + moonSun - minVel;
		
		return minAmp + slope*diff;
	}
	
}
