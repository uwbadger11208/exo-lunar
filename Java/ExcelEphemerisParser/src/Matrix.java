
public class Matrix {

	public static final int DIM = 3;

	Vector[] rows;
	Vector[] cols;
	double[][] elem;

	public Matrix() {
		elem = new double[DIM][DIM];
		rows = new Vector[DIM];
		cols = new Vector[DIM];

		for (int i = 0; i < DIM; i++) {
			rows[i] = new Vector();
			cols[i] = new Vector();
		}
	}

	public Matrix(Vector[] info, boolean isRow) {
		elem = new double[DIM][DIM];
		Vector[] newVecs = new Vector[DIM];
		for (int i = 0; i < newVecs.length; i++) {
			newVecs[i] = new Vector(info[0].get(i),info[1].get(i),info[2].get(i));
			for (int j = 0; j < DIM; j++) {
				if (isRow) {
					//System.out.println(i + " " + j);
					elem[i][j] = info[i].get(j);
				} else {
					elem[i][j] = info[j].get(i);
				}
			}
		}

		if (isRow) {
			rows = info;
			cols = newVecs;
		} else {
			cols = info;
			rows = newVecs;
		}
	}
	
	public void set(int i, int j, double val) {
		elem[i][j] = val;
		rows[i].set(j,val);
		cols[j].set(i,val);
	}
	
	public Vector times(Vector that) {
		
		Vector newVec = new Vector();
		for (int i = 0; i < DIM; i++) {
			newVec.set(i,this.rows[i].dot(that));
		}
		
		return newVec;
	}

	public Matrix transpose() {
		Matrix newMat = new Matrix();
		
		for (int r = 0; r < rows.length; r++) {
			for (int c = 0; c < cols.length; c++) {
				newMat.set(r,c,elem[c][r]);
			}
		}
	
		return newMat;
	}
	
	public Matrix times(Matrix that) {

		Matrix newMat = new Matrix();
		for (int r = 0; r < rows.length; r++) {
			for (int c = 0; c < cols.length; c++) {
				newMat.set(r,c,this.rows[r].dot(that.cols[c]));
			}
		}
		
		return newMat;
	}
	
	public String toString() {
		return rows[0].toString() + "\n" + rows[1].toString() + "\n" + rows[2].toString();
	}
	
	public static void main(String[] args) {
		double[] a1 = {1.0,2.0,3.0};
		double[] a2 = {4.0,5.0,6.0};
		double[] a3 = {7.0,8.0,9.0};
		Vector v1 = new Vector(1.5,2.4,3.8);
		Vector v2 = new Vector(4.7,5.2,6.0);
		Vector v3 = new Vector(7.3,8.5,9.2);
		Vector x = new Vector(1,0,0);
		Vector y = new Vector(0,1,0);
		Vector z = new Vector(0,0,1);
		Vector[] vecs = new Vector[3];
		vecs[0] = v1;
		vecs[1] = v2;
		vecs[2] = v3;
		
		Matrix m1 = new Matrix(vecs,true);
		Matrix m2 = new Matrix(vecs,false);
		
		System.out.println(new Matrix().toString());
		System.out.println();
		System.out.println(m1.toString());
		System.out.println();
		System.out.println(m1.transpose().toString());
		System.out.println();
		System.out.println(m2.toString());
		System.out.println();
		System.out.println(m2.transpose().toString());
		System.out.println();
		System.out.println(m1.times(v1).toString());
		System.out.println();
		System.out.println(m1.times(m2).toString());
		System.out.println();
		System.out.println(m1.times(x).toString());
		System.out.println();
		System.out.println(m1.times(y).toString());
		System.out.println();
		System.out.println(m1.times(z).toString());
	}

}
