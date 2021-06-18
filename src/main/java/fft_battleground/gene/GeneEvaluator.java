package fft_battleground.gene;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import fft_battleground.gene.model.BotGenome;
import fft_battleground.gene.model.Match;
import fft_battleground.model.BattleGroundTeam;
import fft_battleground.tournament.model.Tournament;
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
	
	public int scoreBot(Genotype<IntegerGene> genotype, int[] genomeIntegers, BotGenome genome) {
		final AtomicInteger score = new AtomicInteger(0);
		short[] botWinners = this.generateBotWinnerList(genotype, genomeIntegers, genome);
		if(botWinners.length != this.matchManager.size()) {
			log.error("winner list size mismatch with botWinners at {}, grandWinnersList at {} and there are {} matches", 
					botWinners.length, this.matchManager.size(), this.matchManager.getMatchList().size());
		} else {
			IntStream.range(0,botWinners.length)
			.forEach(i -> {
				short currentGrandWinner = this.matchManager.getGrandWinnerCodeArray()[i];
				short currentBotWinner = botWinners[i];
				if(currentGrandWinner == currentBotWinner) {
					score.getAndIncrement();
				}
			});
		}

		if(score.get() > this.highestResult.get()) {
			this.timer.schedule(new WinnerFileUpdateTimerTask(score.get(), highestResult, genotype, genome, matchManager, winnerFileLock), 0);
		}
		return score.get();
	}
	
	/*
	 * public void logWinner(BotGenome genome) { int score = this.scoreBot(genome);
	 * int percentage = (int) (((double) score )/( (double)
	 * this.matchManager.size()) * 100);
	 * log.info("Score for the winning bot {} out of {} ({}%)", score,
	 * this.matchManager.size(), percentage); }
	 */
	
	protected short[] generateBotWinnerList(Genotype<IntegerGene> genotype, int[] genomeIntegers, BotGenome genome) {
		short[] winnerCodes = new short[this.matchManager.size()];
		for(int i = 0; i < this.matchManager.size(); i++)
		{
			if(this.matchManager.getProblematicIndexes().contains(i)) {
				winnerCodes[i] = BattleGroundTeam.NONE.getTeamCode();
			} else {
				Match match = this.matchManager.getMatchList().get(i);
				Integer leftScore = this.generateTeamScore(genotype, genomeIntegers, match.getLeftTeamAbilityCodes(), match.getLeftTeamFaiths(), match.getLeftTeamBraves(), 
						match.getMapNumber(), genome, BattleGroundTeam.LEFT);
				Integer rightScore = this.generateTeamScore(genotype, genomeIntegers, match.getRightTeamAbilityCodes(), match.getRightTeamFaiths(), match.getRightTeamBraves(), 
						match.getMapNumber(), genome, BattleGroundTeam.RIGHT);
				if(leftScore >= rightScore) {
					winnerCodes[i] = match.getLeftTeam().getTeamCode();
				} else {
					winnerCodes[i] = match.getRightTeam().getTeamCode();
				}
			}
		}
		
		return winnerCodes;
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