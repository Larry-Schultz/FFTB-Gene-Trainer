package fft_battleground.controller;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fft_battleground.gene.GeneService;
import fft_battleground.service.model.GeneStats;
import lombok.SneakyThrows;

@Controller
public class HomeController {

	@Autowired
	private GeneService geneService;
	
	private AtomicBoolean started = new AtomicBoolean(false);
	
	@GetMapping("/start")
	@SneakyThrows
	public ResponseEntity<GeneStats> startProcess(@RequestParam(name = "population") Optional<Integer> population, 
			@RequestParam(name="tournaments") Optional<Integer> tournaments) {
		if(!this.started.get()) {
			if(population.isPresent()) {
				GeneService.POPULATION = population.get();
			} //else just use the default
			if(tournaments.isPresent()) {
				GeneService.NUMBER_OF_TOURNAMENTS_TO_ANALYZE = tournaments.get();
			} //else just use the default
			
			this.started.set(true);
			this.geneService.start();
		}
		
		GeneStats stats = new GeneStats(GeneService.NUMBER_OF_TOURNAMENTS_TO_ANALYZE, GeneService.POPULATION, GeneService.THREAD_COUNT, 
				GeneService.MAX_RANGE_OF_GENE, GeneService.MIN_RANGE_OF_GENE, GeneService.THRESHOLD_PERCENTAGE);
		return new ResponseEntity<GeneStats>(stats, HttpStatus.OK);
	}
}
