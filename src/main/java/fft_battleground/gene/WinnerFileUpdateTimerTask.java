package fft_battleground.gene;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.function.FailableConsumer;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import fft_battleground.gene.model.BotGenome;
import fft_battleground.gene.model.ResultData;
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
	
	private int score;
	private AtomicInteger highestResultRef;
	private BotGenome genomeRef;
	private Genotype<IntegerGene> genotypeRef;
	private MatchManager matchManagerRef;
	private Lock winnerFileLockRef;
	private FailableConsumer<BotGenome, IOException> writeWinnerFileFunction;
	
	public WinnerFileUpdateTimerTask(int score, AtomicInteger highestResult, Genotype<IntegerGene> genotype, BotGenome genome, MatchManager matchManager, 
			Lock winnerFileLock) {
		this.score = score;
		this.highestResultRef = highestResult;
		this.genomeRef = genome;
		this.genotypeRef = genotype;
		this.matchManagerRef = matchManager;
		this.winnerFileLockRef = winnerFileLock;
	}
	
	@Override
	public void run() {
		highestResultRef.set(score);
		Function<Chromosome<IntegerGene>, Integer> chromosomeToIntegerFunction = chromosome -> chromosome.gene().allele();
		List<Integer> winningBotGenomeIntegerList = this.genotypeRef.stream().map(chromosomeToIntegerFunction).collect(Collectors.toList());
		Optional<Integer> maxGene = winningBotGenomeIntegerList.parallelStream().max(Integer::compare);
		Optional<Integer> minGene = winningBotGenomeIntegerList.parallelStream().min(Integer::compare);
		int maxGeneValue = maxGene.isPresent() ? maxGene.get() : 0;
		int minGeneValue = minGene.isPresent() ? minGene.get() : 0;
		long countAtMaxLevel = winningBotGenomeIntegerList.parallelStream().filter(gene -> gene.equals(maxGeneValue)).count();
		long countAtMinLevel = minGene.isPresent() ? winningBotGenomeIntegerList.parallelStream().filter(gene -> gene.equals(minGeneValue)).count() : 0; 
		
		int percentage = (int) (((double) score )/( (double) matchManagerRef.size()) * 100);
		
		log.info("Score for this bot is {} out of {} ({}%).  Max level {} with {} genes of this level.  Min level {} with {} genes of this level.  {} total genes", 
				score, matchManagerRef.size(), percentage, maxGeneValue, countAtMaxLevel, minGeneValue, countAtMinLevel, 
				winningBotGenomeIntegerList.size());
		
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
		String filename = "winner.txt";
		File file = new File(filename);
		
		ResultData data = new ResultData(geneIntegers, attributeNames, percentage, GeneService.abilityEnabled, GeneService.itemEnabled, GeneService.userSkillsEnabled,
				GeneService.classEnabled, GeneService.mapsEnabled, GeneService.BRAVE_FAITH_ENABLED, GeneService.SIDE_ENABLED, 
				GeneService.NUMBER_OF_TOURNAMENTS_TO_ANALYZE, GeneService.THRESHOLD_PERCENTAGE, GeneService.POPULATION, GeneService.MAX_RANGE_OF_GENE,
				GeneService.MIN_RANGE_OF_GENE, GeneService.MAX_RANGE_OF_BRAVE_FAITH, GeneService.MIN_RANGE_OF_BRAVE_FAITH);
		data.sortGeneticAttributesByAbsoluteValue();
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(file, data);
	}
}