package fft_battleground.gene;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fft_battleground.exception.TournamentApiException;
import fft_battleground.gene.model.BotGenome;
import fft_battleground.tournament.Tips;
import fft_battleground.tournament.TournamentService;
import fft_battleground.tournament.model.Tournament;
import fft_battleground.tournament.model.TournamentInfo;
import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.EliteSelector;
import io.jenetics.GaussianMutator;
import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.MeanAlterer;
import io.jenetics.MultiPointCrossover;
import io.jenetics.Mutator;
import io.jenetics.NumericChromosome;
import io.jenetics.Recombinator;
import io.jenetics.RouletteWheelSelector;
import io.jenetics.SinglePointCrossover;
import io.jenetics.StochasticUniversalSelector;
import io.jenetics.TournamentSelector;
import io.jenetics.TruncationSelector;
import io.jenetics.UniformCrossover;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.Limits;
import io.jenetics.util.Factory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GeneService {

	public static final int NUMBER_OF_TOURNAMENTS_TO_ANALYZE = 1000;
	
	public static final int MAX_RANGE_OF_GENE = 100;
	public static final int MIN_RANGE_OF_GENE = (-1) * MAX_RANGE_OF_GENE;
	
	public static final int MAX_RANGE_OF_BRAVE_FAITH = 100;
	public static final int MIN_RANGE_OF_BRAVE_FAITH = 1;
	
	public static final int THREAD_COUNT = 5;
	public static final int POPULATION = 10 * 1000;
	public static final double THRESHOLD_PERCENTAGE = 0.65;
	public static final int WRITE_FILE_PERCENTAGE_THRESHOLD = 60;
	
	public static final boolean DO_KICKSTART = false;
	public static final String kickstartFilename = "winner2000.txt";
	
	public static final boolean abilityEnabled = true;
	public static final boolean itemEnabled = true;
	public static final boolean userSkillsEnabled = true;
	public static final boolean classEnabled = true;
	public static final boolean mapsEnabled = true;
	public static final boolean BRAVE_FAITH_ENABLED = true;
	public static final boolean SIDE_ENABLED = true;
	
	private BotGenome coreGenome;
	private Tips tips;

	@PostConstruct
	public void run() throws TournamentApiException {
		TournamentService tournamentService = new TournamentService();
		try {
			this.tips = tournamentService.getCurrentTips();
		} catch (TournamentApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		this.coreGenome = DO_KICKSTART ? BotGenome.readGenomeFromFile(kickstartFilename) : new BotGenome(tips);
		GeneEvaluator evaluator = this.createGeneEvaluator(tournamentService, coreGenome);
		tournamentService = null;
		
		LocalDateTime start = LocalDateTime.now();
		this.runGeneticTraining(tips, evaluator);
		LocalDateTime end = LocalDateTime.now();
		
		Long minutes = ChronoUnit.MINUTES.between(start, end);
		log.info("The training took {} minutes to run", minutes.toString());
	}
	
	public BotGenome runGeneticTraining(final Tips tips, final GeneEvaluator evaluator) {
		List<IntegerChromosome> chromosomes = this.coreGenome.toGeneList().parallelStream().map(gene -> IntegerChromosome.of(gene)).collect(Collectors.toList());
		Factory<Genotype<IntegerGene>> gtf = Genotype.of(chromosomes);
     
        
        
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        // 3.) Create the execution environment.
        Engine<IntegerGene, Integer> engine = Engine
            .builder(gt -> this.eval(gt, evaluator), gtf)
            .survivorsSelector(new EliteSelector<>())
            .offspringSelector(new StochasticUniversalSelector<>())
            //.alterers(new UniformCrossover<>(0.2), new Mutator<>(0.15))
            .populationSize(POPULATION)
            .executor(executorService)
            .build();
 
        // 4.) Start the execution (evolution) and
        //     collect the result.
        int threshold = (int) (THRESHOLD_PERCENTAGE * evaluator.getMatchManager().size());
        Predicate<? super EvolutionResult<IntegerGene, Integer>> limit = Limits.byFitnessThreshold(threshold);
        log.info("this run's threshold is {}", threshold);
        Genotype<IntegerGene> result = engine.stream()
        		.limit(limit)
        		.parallel()
        		.peek(evolutionResult -> {
        			DecimalFormat df=new DecimalFormat("#.##");
        			Runtime runtime = Runtime.getRuntime();
        			Double currentMemoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory());
        			Double memoryUsageInGigabytes = (((currentMemoryUsage/1024.0)/1024.0)/1024.0);
        			log.info("current generation: {}.  current memory usage: {}G", evolutionResult.generation(), df.format(memoryUsageInGigabytes));
        		})
            .collect(EvolutionResult.toBestGenotype());
        
		List<Integer> genomeIntegers = result.stream().map(chromosome -> chromosome.gene().intValue()).collect(Collectors.toList());
		BotGenome winningBotGenome = new BotGenome(tips);
		winningBotGenome.ingestGeneArray(genomeIntegers);
		
		log.info("logging winner to winner.txt");
		
		List<Integer> winningBotGenomeIntegerList = winningBotGenome.toIntegerList();
		Optional<Integer> maxGene = winningBotGenomeIntegerList.parallelStream().max(Double::compare);
		if(maxGene.isPresent()) {
			long countAtThatLevel = winningBotGenomeIntegerList.parallelStream().filter(gene -> gene.equals(maxGene.get())).count();
			log.info("winner's highest gene is {} with {} genes having that value", maxGene.get().toString(), countAtThatLevel);
		}
		
		
		return winningBotGenome;
	}
	
	@SneakyThrows
	private GeneEvaluator createGeneEvaluator(TournamentService service, BotGenome genome) {
		List<TournamentInfo> tournamentInfoList = service.getLatestTournamentInfo(NUMBER_OF_TOURNAMENTS_TO_ANALYZE);
		List<Tournament> tournaments = service.getTournaments(tournamentInfoList);
		GeneEvaluator evaluator = new GeneEvaluator(tournaments, this.coreGenome);
		return evaluator;
	}
	
	private int eval(Genotype<IntegerGene> genotype, GeneEvaluator evaluator) {
		int[] genomeIntegers = genotype.stream().mapToInt(chromosome -> chromosome.gene().intValue()).toArray();
        int correctlyGuessedWinners = evaluator.scoreBot(genotype, genomeIntegers, this.coreGenome);
        return correctlyGuessedWinners;
    }
}
