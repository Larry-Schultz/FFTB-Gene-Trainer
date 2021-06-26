package fft_battleground.gene.model;

import org.apache.commons.lang3.StringUtils;

import fft_battleground.model.BattleGroundTeam;
import fft_battleground.service.tournament.model.Team;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Match {
	private BattleGroundTeam leftTeam;
	private int[] leftTeamAbilityCodes;
	private short[] leftTeamFaiths;
	private short[] leftTeamBraves;
	
	private BattleGroundTeam rightTeam;
	private int[] rightTeamAbilityCodes;
	private short[] rightTeamFaiths;
	private short[] rightTeamBraves;
	
	private Integer mapNumber;
	
	public Match(BattleGroundTeam leftTeamName, Team leftTeamData, BattleGroundTeam rightTeamName, Team rightTeamData, 
			Integer mapNumber, BotGenome genome) {
		this.leftTeam = leftTeamName;
		this.rightTeam = rightTeamName;
		this.mapNumber = mapNumber;
		
		this.leftTeamAbilityCodes = leftTeamData.getAbilityCodes(genome);
		this.rightTeamAbilityCodes = rightTeamData.getAbilityCodes(genome);
		
		this.leftTeamFaiths = leftTeamData.faiths();
		this.leftTeamBraves = leftTeamData.braves();
		
		this.rightTeamFaiths = rightTeamData.faiths();
		this.rightTeamBraves = rightTeamData.braves();
	}
	
	public static Integer mapNumber(String mapName) {
		String numberString = StringUtils.substringBefore(mapName, ")");
		Integer value = Integer.valueOf(numberString);
		return value;
	}
}