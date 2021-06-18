package fft_battleground.gene.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import fft_battleground.gene.GeneService;
import fft_battleground.tournament.Tips;
import fft_battleground.util.GenericElementOrdering;
import fft_battleground.util.GenericPairing;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@Slf4j
public class BotGenome implements Cloneable {
	
	public static final List<String> itemsToPrefix = Arrays.asList(new String[] {"Bracer", "Elixir", "Kiyomori", "Soft", "X-Potion", "Remedy", "Muramasa",
			"Maiden's Kiss", "Masamune", "Murasame", "Eye Drop", "Shuriken", "Antidote", "Hi-Potion", "Heaven's Cloud", "Bizen Boat",
			"Hi-Ether", "Chirijiraden", "Kikuichimoji", "Potion", "Holy Water", "Spear", "Echo Grass", "Phoenix Down", "Ether"});
	public static final String itemSuffix = "-Item";
	
	
	
	@JsonIgnore
	public static final List<String> braveFaith = Arrays.asList(new String[] {"Brave", "BraveDivide", "Faith", "FaithDivide"});
	
	private List<GenericElementOrdering<GenericPairing<String, Integer>>> elements;
	
	@JsonIgnore
	private int elementId = 0;
	
	private int faithCode;
	private int braveCode;
	private int leftTeamCode;
	private int rightTeamCode;
	private Map<Integer, Integer> leftTeamMapCodes = new HashMap<>();
	private Map<Integer, Integer> rightTeamMapCodes = new HashMap<>();
	
	public BotGenome(Tips tips) {
		this.elements = new LinkedList<>();
		if(GeneService.BRAVE_FAITH_ENABLED) {
			GenericElementOrdering<GenericPairing<String, Integer>> braveElement = this.createBlankValuePairing("Brave");
			this.elements.add(braveElement);
			this.braveCode = braveElement.getId();
			GenericElementOrdering<GenericPairing<String, Integer>> faithElement = this.createBlankValuePairing("Faith");
			this.elements.add(faithElement);
			this.faithCode = faithElement.getId();
		}
		if(GeneService.SIDE_ENABLED) {
			GenericElementOrdering<GenericPairing<String, Integer>> leftElement = this.createBlankValuePairing("LeftTeam");
			this.elements.add(leftElement);
			this.leftTeamCode = leftElement.getId();
			
			GenericElementOrdering<GenericPairing<String, Integer>> rightElement = this.createBlankValuePairing("RightTeam");
			this.elements.add(rightElement);
			this.rightTeamCode = rightElement.getId();
		}
		for(String elementName: BotGenome.elementNames(tips)) {
			GenericElementOrdering<GenericPairing<String, Integer>> element = this.createBlankValuePairing(elementName);
			this.elements.add(element);
		}
		if(GeneService.mapsEnabled) {
			for(Integer mapNumber: BotGenome.mapNumbers()) {
				GenericElementOrdering<GenericPairing<String, Integer>> leftElement = this.createBlankValuePairing(mapNumber.toString() + "-Left");
				this.elements.add(leftElement);
				this.leftTeamMapCodes.put(mapNumber, leftElement.getId());
				
				GenericElementOrdering<GenericPairing<String, Integer>> rightElement = this.createBlankValuePairing(mapNumber.toString() + "-Right");
				this.elements.add(rightElement);
				this.rightTeamMapCodes.put(mapNumber, rightElement.getId());
			}
		}
		//this.ensureGoodOrdering();
	}
	
	public List<IntegerGene> toGeneList() {
		List<IntegerGene> geneList = this.elements.stream().map(orderingElement -> orderingElement.getElement())
				.map(element -> {
					if(!braveFaith.contains(element.getKey())) {
						return IntegerGene.of(element.getValue(), GeneService.MIN_RANGE_OF_GENE, GeneService.MAX_RANGE_OF_GENE);
					} else {
						return IntegerGene.of(element.getValue(), GeneService.MIN_RANGE_OF_BRAVE_FAITH, GeneService.MAX_RANGE_OF_BRAVE_FAITH);
					}
				})
				.collect(Collectors.toList());
		
		return geneList;
	}
	
	public void ensureGoodOrdering() {
		Collections.sort(this.elements, new BotGenomeElementComparator());
	}
	
	public List<Integer> toIntegerList() {
		List<Integer> integerList = this.elements.parallelStream().map(orderingElement -> orderingElement.getElement())
				.map(element -> element.getValue()).collect(Collectors.toList());
		return integerList;
	}
	
	@JsonIgnore
	public Map<Integer, Integer> getAbilityGeneMap(int[] genomeIntegers) {
		Function<GenericElementOrdering<GenericPairing<String, Integer>>, Integer> keyFunction = orderingElement -> orderingElement.getId();
		Function<GenericElementOrdering<GenericPairing<String, Integer>>, Integer> valueFunction = orderingElement -> genomeIntegers[orderingElement.getId()];
		Map<Integer, Integer> abilityCodeAbilityValueMap = this.elements.stream().collect(Collectors.toMap(keyFunction, valueFunction));
		return abilityCodeAbilityValueMap;
	}
	
	public void ingestGeneArray(int[] geneList) {
		for(int i = 0; i < geneList.length; i++) {
			Integer currentValue = geneList[i];
			this.elements.get(i).getElement().setValue(currentValue);
		}
	}
	
	public void ingestGeneArray(List<Integer> geneList) {
		for(int i = 0; i < geneList.size(); i++) {
			Integer currentValue = geneList.get(i);
			this.elements.get(i).getElement().setValue(currentValue);
		}
	}
	
	public static List<String> elementNames(Tips tips) {
		List<String> names = new LinkedList<>();
		if(GeneService.itemEnabled) {
			for(String elementName: tips.getItem().keySet()) {
				List<String> duplicates = itemsToPrefix;
				if(!duplicates.contains(elementName)) {
					names.add(elementName);
				} else {
					names.add(elementName + itemSuffix);
				}
			}
		}
		
		if(GeneService.abilityEnabled) {
			for (String elementName : tips.getAbility().keySet()) {
				if (!tips.getUserSkill().containsKey(elementName)) {
					names.add(elementName);
				}
			} 
		}
		
		if(GeneService.userSkillsEnabled) {
			for(String elementName: tips.getUserSkill().keySet()) {
				Set<String> classes = tips.getClassMap().keySet();
				if(!classes.contains(elementName)) {
					names.add(elementName);
				}
			}
		}
		
		List<String> prestigeSkills = Arrays.asList(new String[] {"RaidBoss","MathSkill","EquipPerfume","Teleport2","BladeGrasp","Doppelganger",});
		for(String prestige: prestigeSkills) {
			names.add(prestige);
		}
		
		if(GeneService.classEnabled) {
			for(String elementName: tips.getClassMap().keySet()) {
				names.add(elementName);
			}
		}
		return names;
	}
	
	public static BotGenome readGenomeFromFile(String filename) {
		BotGenome genome = null;
		File file = new File(filename);
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			genome = mapper.readValue(file, BotGenome.class);
		} catch (IOException e) {
			log.error("Error writing winners file", e);
		}
		genome.ensureGoodOrdering();
		
		return genome;
	}
	
	private GenericElementOrdering<GenericPairing<String, Integer>> createBlankValuePairing(String elementName) {
		GenericPairing<String, Integer> element = new GenericPairing<String, Integer>(elementName, 0);
		GenericElementOrdering<GenericPairing<String, Integer>> ordering = new GenericElementOrdering<>(this.elementId, element);
		this.elementId++;
		return ordering;
	}
	
	private static List<Integer> mapNumbers() {
		List<Integer> mapNumbers = new LinkedList<>();
		File mapFile = new File("Maps.txt");
		try(BufferedReader reader = new BufferedReader(new FileReader(mapFile))) {
			String line;
			while((line = reader.readLine()) != null) {
				Integer mapNumber = Match.mapNumber(line);
				mapNumbers.add(mapNumber);
			}
		} catch (FileNotFoundException e) {
			log.error("Error reading map file", e);
		} catch (IOException e) {
			log.error("Error reading map file", e);
		}
		
		return mapNumbers;
	}
	
}

class BotGenomeElementComparator implements Comparator<GenericElementOrdering<GenericPairing<String, Integer>>> {
	@Override
	public int compare(GenericElementOrdering<GenericPairing<String, Integer>> o1,
			GenericElementOrdering<GenericPairing<String, Integer>> o2) {
		return o1.compareTo(o2);
	}
}

