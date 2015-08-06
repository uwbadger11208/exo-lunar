
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
		Vector[] newVecs = new Vector[DIM];
		for (int i = 0; i < newVecs.length; i++) {
			newVecs[i] = new Vector(info[0].get(i),info[1].get(i),info[2].get(i));
			for (int j = 0; j < DIM; j++) {
				if (isRow) {
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

}
