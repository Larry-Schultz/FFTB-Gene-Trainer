package fft_battleground.gene.model;

import java.util.Comparator;

public record EvaluatorResult(long score, int winners) implements Comparable<EvaluatorResult> {
	
	@Override
	public int compareTo(EvaluatorResult o) {
		return Integer.compare(this.winners, o.winners);
	}

}
