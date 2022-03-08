package fft_battleground.gene;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import com.google.common.math.Quantiles;

import fft_battleground.gene.model.BotGenome;
import fft_battleground.gene.model.EvaluatorResult;
import fft_battleground.gene.model.Match;
import fft_battleground.gene.model.MatchResult;
import fft_battleground.model.BattleGroundTeam;
import fft_battleground.service.tournament.model.Tournament;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class GeneEvaluator {
	
	private MatchManager matchManager;
	
	private AtomicInteger highestResult = new AtomicInteger(0);
	private Lock winnerFileLock = new ReentrantLock();
	private Timer timer = new Timer();
	
	public GeneEvaluator() {}
	
	public GeneEvaluator(List<Tournament> tournaments, BotGenome genome) {
		
		this.matchManager = new MatchManager(tournaments, genome);
	}
	
	public EvaluatorResult scoreBot(Genotype<IntegerGene> genotype, int[] genomeIntegers, BotGenome genome) {
		final AtomicLong score = new AtomicLong(0);
		final AtomicInteger correctMatches = new AtomicInteger(0);
		//MatchResult[] botWinners = this.generateBotWinnerList(genotype, genomeIntegers, genome);
		
		IntStream.range(0, this.matchManager.size())
		.forEach(i -> {
			Match currentMatch = this.matchManager.getMatchList().get(i);
			MatchResult matchResult = this.projectAWinnerForMatch(genotype, genomeIntegers, genome, currentMatch, i);
			short currentGrandWinner = this.matchManager.getGrandWinnerCodeArray()[i];
			short currentBotWinner = matchResult.winnerTeamCode();
			if(currentBotWinner == currentGrandWinner) {
				long scoreAlteration = matchResult.getWinneringScore();
				score.addAndGet(scoreAlteration);
				correctMatches.getAndIncrement();
			} else {
				long scoreAlteration = (long) (GeneService.NEGATIVE_SCORE_MULTIPLIER * matchResult.getLosingScore());
				score.addAndGet(scoreAlteration);
			}
		});

		if(correctMatches.get() > this.highestResult.get()) {
			this.highestResult.set(correctMatches.get());
			this.timer.schedule(new WinnerFileUpdateTimerTask(score.get(), correctMatches.get(), highestResult, genotype, genome, matchManager, winnerFileLock, this), 0);
		}
		
		EvaluatorResult result = new EvaluatorResult(score.get(), correctMatches.get());
		return result;
	}
	
	public Map<Integer, Double> calculateScoreDifferencePercentiles(Genotype<IntegerGene> genotype, int[] genomeIntegers, BotGenome genome) {
		List<Integer> scoreDifferences = new LinkedList<>();
		MatchResult[] results = this.generateBotWinnerList(genotype, genomeIntegers, genome);
		for(int i = 0; i < results.length; i++) {
			int difference = results[i].getDifference();
			scoreDifferences.add(difference);
		}
		
		Map<Integer, Double> percentiles = Quantiles.percentiles()
				.indexes(IntStream.range(1, 100).toArray())
				.compute(scoreDifferences);
		
		return percentiles;
	}
	
	/*
	 * public void logWinner(BotGenome genome) { int score = this.scoreBot(genome);
	 * int percentage = (int) (((double) score )/( (double)
	 * this.matchManager.size()) * 100);
	 * log.info("Score for the winning bot {} out of {} ({}%)", score,
	 * this.matchManager.size(), percentage); }
	 */
	
	protected MatchResult[] generateBotWinnerList(Genotype<IntegerGene> genotype, int[] genomeIntegers, BotGenome genome) {
		MatchResult[] matchResults = new MatchResult[this.matchManager.size()];
		for(int i = 0; i < this.matchManager.size(); i++)
		{
			if(this.matchManager.getProblematicIndexes().contains(i)) {
				short winnerCode = BattleGroundTeam.NONE.getTeamCode();
				matchResults[i] = new MatchResult(winnerCode, 0, 0);
			} else {
				Match match = this.matchManager.getMatchList().get(i);
				Integer leftScore = this.generateTeamScore(genotype, genomeIntegers, match.getLeftTeamAbilityCodes(), match.getLeftTeamFaiths(), match.getLeftTeamBraves(), 
						match.getMapNumber(), genome, BattleGroundTeam.LEFT);
				Integer rightScore = this.generateTeamScore(genotype, genomeIntegers, match.getRightTeamAbilityCodes(), match.getRightTeamFaiths(), match.getRightTeamBraves(), 
						match.getMapNumber(), genome, BattleGroundTeam.RIGHT);
				if(leftScore >= rightScore) {
					short winnerCodes = match.getLeftTeam().getTeamCode();
					matchResults[i] = new MatchResult(winnerCodes, leftScore, rightScore);
				} else {
					short winnerCodes = match.getRightTeam().getTeamCode();
					matchResults[i] = new MatchResult(winnerCodes, rightScore, leftScore);
				}
			}
		}
		
		return matchResults;
	}
	
	protected MatchResult projectAWinnerForMatch(Genotype<IntegerGene> genotype, int[] genomeIntegers, BotGenome genome, Match match, int index) {
		Integer leftScore = this.generateTeamScore(genotype, genomeIntegers, match.getLeftTeamAbilityCodes(), match.getLeftTeamFaiths(), match.getLeftTeamBraves(), 
				match.getMapNumber(), genome, BattleGroundTeam.LEFT);
		Integer rightScore = this.generateTeamScore(genotype, genomeIntegers, match.getRightTeamAbilityCodes(), match.getRightTeamFaiths(), match.getRightTeamBraves(), 
				match.getMapNumber(), genome, BattleGroundTeam.RIGHT);
		MatchResult result = null;
		if(this.matchManager.getProblematicIndexes().contains(index)) {
			short winnerCode = BattleGroundTeam.NONE.getTeamCode();
			result = new MatchResult(winnerCode, 0, 0);
		} else if(leftScore >= rightScore) {
			short winnerCode = match.getLeftTeam().getTeamCode();
			result = new MatchResult(winnerCode, leftScore, rightScore);
		} else {
			short winnerCode = match.getRightTeam().getTeamCode();
			result = new MatchResult(winnerCode, rightScore, leftScore);
		}
		
		return result;
		
	}
	
	protected Integer generateTeamScore(Genotype<IntegerGene> genotype, int[] genomeIntegers, int[] teamAbilityCodes, short[] faiths, short[] braves, Integer mapNumber, BotGenome genome, BattleGroundTeam side) {
		int score = 0;
		//Map<Integer, Integer> genomeMap = genome.getAbilityGeneMap(genomeIntegers);
		for(int code : teamAbilityCodes) {
			Integer scoreValue = genomeIntegers[code];
			if(scoreValue != null) {
				score += scoreValue;
			}
		}
		
		if(GeneService.BRAVE_FAITH_ENABLED) {
			Integer braveMultiplier = genomeIntegers[genome.getBraveCode()];
			Integer faithMultiplier = genomeIntegers[genome.getFaithCode()];
			for(short faith: faiths) {
				score += faith * faithMultiplier;
			}
			for(short brave: braves) {
				score += brave * braveMultiplier;
			}
		}
		
		if(side == BattleGroundTeam.LEFT) {
			if(GeneService.SIDE_ENABLED) {
				Integer leftTeamBonus = genomeIntegers[genome.getLeftTeamCode()];
				score += leftTeamBonus;
			}
			if(GeneService.mapsEnabled) {
				Integer mapBonus = genomeIntegers[genome.getLeftTeamMapCodes().get(mapNumber)];
				score += mapBonus;
			}
		} else if(side == BattleGroundTeam.RIGHT) {
			if(GeneService.SIDE_ENABLED) {
				Integer rightTeamBonus = genomeIntegers[genome.getRightTeamCode()];
				score += rightTeamBonus;
			}
			if(GeneService.mapsEnabled) {
				Integer mapBonus = genomeIntegers[genome.getRightTeamMapCodes().get(mapNumber)];
				score += mapBonus;
			}
		}
		
		return score;
	}
	
}