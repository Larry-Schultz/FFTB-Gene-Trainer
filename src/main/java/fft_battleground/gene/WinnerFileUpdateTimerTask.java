package fft_battleground.gene;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import fft_battleground.gene.model.BotGenome;
import fft_battleground.gene.model.ResultData;
import fft_battleground.service.model.HighScore;
import io.jenetics.Chromosome;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@NoArgsConstructor
public class WinnerFileUpdateTimerTask extends TimerTask {
	
	private long score;
	private int correctMatches;
	private AtomicInteger highestResultRef;
	private BotGenome genomeRef;
	private Genotype<IntegerGene> genotypeRef;
	private MatchManager matchManagerRef;
	private Lock winnerFileLockRef;
	private GeneEvaluator geneEvaluator;
	
	public WinnerFileUpdateTimerTask(long score, int correctMatches, AtomicInteger highestResult, Genotype<IntegerGene> genotype, BotGenome genome, MatchManager matchManager, 
			Lock winnerFileLock, GeneEvaluator geneEvaluator) {
		this.score = score;
		this.correctMatches = correctMatches;
		this.highestResultRef = highestResult;
		this.genomeRef = genome;
		this.genotypeRef = genotype;
		this.matchManagerRef = matchManager;
		this.winnerFileLockRef = winnerFileLock;
		this.geneEvaluator = geneEvaluator;
	}
	
	@Override
	public void run() {
		highestResultRef.set(this.correctMatches);
		Function<Chromosome<IntegerGene>, Integer> chromosomeToIntegerFunction = chromosome -> chromosome.gene().allele();
		List<Integer> winningBotGenomeIntegerList = this.genotypeRef.stream().map(chromosomeToIntegerFunction).collect(Collectors.toList());
		Optional<Integer> maxGene = winningBotGenomeIntegerList.parallelStream().max(Integer::compare);
		Optional<Integer> minGene = winningBotGenomeIntegerList.parallelStream().min(Integer::compare);
		int maxGeneValue = maxGene.isPresent() ? maxGene.get() : 0;
		int minGeneValue = minGene.isPresent() ? minGene.get() : 0;
		long countAtMaxLevel = winningBotGenomeIntegerList.parallelStream().filter(gene -> gene.equals(maxGeneValue)).count();
		long countAtMinLevel = minGene.isPresent() ? winningBotGenomeIntegerList.parallelStream().filter(gene -> gene.equals(minGeneValue)).count() : 0; 
		
		int percentage = (int) (((double) correctMatches )/( (double) matchManagerRef.size()) * 100);
		int geneCount = winningBotGenomeIntegerList.size();
		
		log.info("Score for this bot is {}. {} out of {} matches were guessed correctly ({}%).  Max level {} with {} genes of this level.  Min level {} with {} genes of this level.  {} total genes", 
				this.score, this.correctMatches, this.matchManagerRef.size(), percentage, maxGeneValue, countAtMaxLevel, minGeneValue, countAtMinLevel, geneCount);
		
		this.updateHighScore(percentage, maxGeneValue, countAtMaxLevel, minGeneValue, countAtMinLevel, geneCount);
		
		List<String> attributeNames = this.genomeRef.getElements().stream().map(orderedElement -> orderedElement.getElement().getKey()).collect(Collectors.toList());
		if(percentage >= GeneService.WRITE_FILE_PERCENTAGE_THRESHOLD) {
			try {
				log.info("Writing new winner");
				winnerFileLockRef.lock();
				this.writeWinnerToFile(winningBotGenomeIntegerList, attributeNames, percentage);
			} catch(Exception e) {
				log.error("Error writing to winner file", e);
			} finally {
				winnerFileLockRef.unlock();
			}
		}
	}
	
	private void writeWinnerToFile(List<Integer> geneIntegers, List<String> attributeNames, final int percentage) throws JsonGenerationException, JsonMappingException, IOException {
		String filename = "winner-" + GeneService.NUMBER_OF_TOURNAMENTS_TO_ANALYZE.toString() + ".txt";
		File file = new File(filename);
		
		Map<Integer, Double> percentiles = this.calculatePercentiles(geneIntegers);
		
		String dateString = this.generateCurrentTimeFormattedDateString();
		
		ResultData data = new ResultData(geneIntegers, attributeNames, percentage, GeneService.abilityEnabled, GeneService.itemEnabled, GeneService.userSkillsEnabled,
				GeneService.classEnabled, GeneService.mapsEnabled, GeneService.BRAVE_FAITH_ENABLED, GeneService.SIDE_ENABLED, 
				GeneService.NUMBER_OF_TOURNAMENTS_TO_ANALYZE, GeneService.THRESHOLD_PERCENTAGE, GeneService.POPULATION, GeneService.MAX_RANGE_OF_GENE,
				GeneService.MIN_RANGE_OF_GENE, GeneService.MAX_RANGE_OF_BRAVE_FAITH, GeneService.MIN_RANGE_OF_BRAVE_FAITH, percentiles, dateString,
				this.score);
		data.sortGeneticAttributesByAbsoluteValue();
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(file, data);
	}
	
	private void updateHighScore(int percentage, int maxGeneValue, long countAtMaxLevel, int minGeneValue, long countAtMinLevel, int geneCount) {
		String dateString = this.generateCurrentTimeFormattedDateString();
		HighScore highScore = new HighScore(this.score, percentage, this.correctMatches, this.matchManagerRef.size(), 
				maxGeneValue, countAtMaxLevel, minGeneValue, countAtMinLevel, geneCount, dateString);
		this.matchManagerRef.setHighScore(highScore);
	}
	
	private Map<Integer, Double> calculatePercentiles(List<Integer> geneIntegers) {
		int[] genes = geneIntegers.stream().mapToInt(gene -> gene.intValue()).toArray();
		Map<Integer, Double> percentiles = this.geneEvaluator.calculateScoreDifferencePercentiles(this.genotypeRef, genes, this.genomeRef);
		return percentiles;
	}
	
	private String generateCurrentTimeFormattedDateString() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa");
		Date currentTime = new Date();
		String dateString = sdf.format(currentTime);
		
		return dateString;
	}
}