package evaluation;

public class F1Score {

	public double precision = 0;
	public double recall = 0;
	public double f1 = 0;

	public double tp = 0;
	public double fp = 0;
	public double fn = 0;

	public void score() {
		precision = tp / (tp + fp);
		recall = tp / (tp + fn);
		f1 = 2 * (precision * recall) / (precision + recall);
	}

	@Override
	public String toString() {
		return "F1Score [F1=" + f1 + ", P=" + precision + ", R=" + recall + " (tp=" + tp + ", fp=" + fp + ", fn=" + fn
				+ ")]";
	}

}
