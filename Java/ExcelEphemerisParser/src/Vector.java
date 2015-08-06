import java.text.DecimalFormat;

/**
 * Represents a three dimensional Euclidean vector using an array of size 3
 * @author nderr
 */
public class Vector {
	
	// constants
	public static final int DIM = 3;
	public static final int X = 0;
	public static final int Y = 1;
	public static final int Z = 2;

	// the vector itself
	private double[] elem;
	
	/**
	 * Instantiates a vector with zenith and azimuth on a unit sphere
	 * @param el elevation angle (0 through pi)
	 * @param azi azimuthal angle (0 though 2 pi)
	 */
	public Vector(double el, double azi) {
		elem = new double[DIM];
		elem[X] = Math.cos(azi) * Math.cos(el);
		elem[Y] = Math.sin(azi) * Math.cos(el);
		elem[Z] = Math.sin(el);
	}
	
	public Vector() {
		elem = new double[DIM];
		elem[X] = 0;
		elem[Y] = 0;
		elem[Z] = 0;
	}
	
	/**
	 * Instantiates a vector with the provided cartesian coordinates
	 * @param x cartesian x
	 * @param y cartesian y
	 * @param z cartesian z
	 */
	public Vector(double x, double y, double z) {
		elem = new double[DIM];
		elem[X] = x;
		elem[Y] = y;
		elem[Z] = z;
	}
	
	public double get(int i) {
		return elem[i];
	}
	
	/**
	 * The vector's x-coordinate
	 */
	public double getX() {
		return elem[X];
	}
	
	/**
	 * The vector's y-coordinate
	 */
	public double getY() {
		return elem[Y];
	}
	
	/**
	 * The vector's z-coordinate
	 */
	public double getZ() {
		return elem[Z];
	}

	/**
	 * Sets the vector's x-coordinate
	 */
	public void setX(double val) {
		elem[X] = val;
	}
	
	/**
	 * Sets the vector's y-coordinate
	 */
	public void setY(double val) {
		elem[Y] = val;
	}
	
	public void set(int i, double val) {
		elem[i] = val;
	}
	
	/**
	 * Sets the vector's z-coordinate
	 */
	public void setZ(double val) {
		elem[Z] = val;
	}
	
	public Vector scale(double scalar) {
		return new Vector(getX()*scalar,getY()*scalar,getZ()*scalar);
	}
	
	/**
	 * Calculates the dot product of this vector with the provided vector
	 * @param that vector to be dotted with
	 * @return the dot product of the two vectors
	 */
	public double dot(Vector that) {
		return this.getX() * that.getX() + this.getY() * that.getY() 
				+ this.getZ() * that.getZ();
	}
	
	public Vector cross(Vector that) {
		return new Vector(this.getY()*that.getZ() - this.getZ()*that.getY(),
						  this.getZ()*that.getX() - this.getX()*that.getZ(),
						  this.getX()*that.getY() - this.getY()*that.getX());
	}
	
	public double norm() {
		return Math.sqrt(Math.pow(getX(), 2) + Math.pow(getY(), 2) + Math.pow(getZ(), 2));
	}
	
	public Vector plus(Vector that) {
		return new Vector(this.getX() + that.getX(),
						  this.getY() + that.getY(),
						  this.getZ() + that.getZ());
	}
	
	public double angBetween(Vector that) {
		double dotProd = this.dot(that) / (this.norm() * that.norm());
		return Math.acos(dotProd);
	}
	
	public Vector negative() {
		return new Vector(-getX(),-getY(),-getZ());
	}
	
	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("#0.00");
		return "[ " + df.format(getX()) + " , " + df.format(getY()) + " , " + 
				df.format(getZ()) + " ] ";
	}
	
}