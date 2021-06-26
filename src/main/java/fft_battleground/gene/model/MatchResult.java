package fft_battleground.gene.model;

public record MatchResult(short winnerTeamCode, int predictedWinnerScore, int predictedLoserScore) {

	public int getWinneringScore() {
		int score = this.getDifference();
		return score;
	}
	
	public int getLosingScore() {
		int score = (-1) * this.getDifference();
		return score;
	}
	
	protected int getDifference() {
		int difference = Math.abs(predictedWinnerScore - predictedLoserScore);
		return difference;
	}
}
