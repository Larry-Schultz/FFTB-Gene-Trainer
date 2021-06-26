package fft_battleground.service.tournament.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import fft_battleground.gene.model.BotGenome;
import fft_battleground.util.GenericElementOrdering;
import fft_battleground.util.GenericPairing;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {
	@JsonProperty("Player")
	private String Player;
	@JsonProperty("Name")
	private String Name;
	@JsonProperty("Palettes")
	private String Palettes;
	@JsonProperty("Units")
	private List<Unit> Units;
	
	public Team() {}
	
	@JsonIgnore
	public int[] abilityCodesCache;
	
	@JsonIgnore
	public int[] getAbilityCodes(BotGenome genome) {
		if(abilityCodesCache != null) {
			return this.abilityCodesCache;
		}
		
		Function<GenericElementOrdering<GenericPairing<String, Integer>>, Integer> codeFunction = orderingElement -> (int) orderingElement.getId();
		Function<GenericElementOrdering<GenericPairing<String, Integer>>, String> abilityNameFunction = orderingElement -> orderingElement.getElement().getKey();
		Map<String, Integer> abilityNameAbilityCodeMap = genome.getElements().parallelStream().collect(Collectors.toMap(abilityNameFunction, codeFunction));
		List<String> abilityElements = this.getTeamAbilityElements();
		int[] codes = abilityElements.parallelStream()
				.filter(ability -> abilityNameAbilityCodeMap.containsKey(ability))
				.mapToInt(ability -> abilityNameAbilityCodeMap.get(ability)).toArray();
		
		this.abilityCodesCache = codes;
		
		return codes;
	}
	
	@JsonIgnore
	public List<String> getTeamAbilityElements() {
		List<String> elements = new ArrayList<>();
		for(Unit unit : this.Units) {
			elements.addAll(unit.getUnitGeneAbilityElements());
		}
		
		return elements;
	}
	
	public short[] faiths() {
		short[] faiths = new short[4];
		for(int i = 0; i < 4 && i < Units.size(); i++) {
			faiths[i] = Units.get(i).getFaith();
		}
		
		return faiths;
	}
	
	public short[] braves() {
		short[] braves = new short[4];
		for(int i = 0; i < 4 && i < Units.size(); i++) {
			braves[i] = Units.get(i).getBrave();
		}
		
		return braves;
	}
}
