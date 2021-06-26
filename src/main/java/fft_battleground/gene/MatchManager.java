package fft_battleground.gene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tomcat.util.buf.StringUtils;

import fft_battleground.gene.model.BotGenome;
import fft_battleground.gene.model.Match;
import fft_battleground.model.BattleGroundTeam;
import fft_battleground.service.model.HighScore;
import fft_battleground.service.tournament.model.Team;
import fft_battleground.service.tournament.model.Tournament;
import fft_battleground.util.GenericPairing;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MatchManager {
	
	@Getter private short[] grandWinnerCodeArray;
	@Getter private List<Match> matchList;
	
	@Getter private Set<Integer> problematicIndexes;
	
	@Getter @Setter 
	private HighScore highScore = null;
	
	@Getter @Setter
	private Long generation = null;
	
	public MatchManager() {}
	
	public MatchManager(List<Tournament> tournaments, BotGenome genome) {
		List<BattleGroundTeam> grandWinnersList = this.buildGrandWinnersList(tournaments);
		this.grandWinnerCodeArray = this.buildTeamCodeArray(grandWinnersList);
		this.matchList = this.generateMatchList(genome, tournaments);
		
		this.problematicIndexes = this.verifyAndFindProblematicMatches(grandWinnersList);
		this.setWinnerToNoneForBadMatches(this.problematicIndexes, grandWinnersList);
		
		if(this.problematicIndexes != null && this.problematicIndexes.size() > 0) {
			Collection<String> problematicIndexStrings = this.problematicIndexes.parallelStream()
					.map(problematicIndex -> problematicIndex.toString()).collect(Collectors.toList());
			String matchNumbers = StringUtils.join(problematicIndexStrings, ',');
			log.warn("The problematic match numbers are: {}", matchNumbers);
		}
	}
	
	public int size() {
		return this.matchList.size();
	}
	
	private List<BattleGroundTeam> buildGrandWinnersList(List<Tournament> tournaments) {
		List<BattleGroundTeam> grandWinnersList = new ArrayList<>();
		for(int j = 1; j < tournaments.size(); j++) { //ignore index 0
			Tournament tournament = tournaments.get(j);
			if(tournament.getWinners().size() == 8) {
				for(int i = 0; i <= 7; i++) {
					String winner = tournament.getWinners().get(i);
					BattleGroundTeam team = BattleGroundTeam.parse(winner);
					grandWinnersList.add(team);
				}
			}
		}
		
		return grandWinnersList;
	}
	
	private short[] buildTeamCodeArray(List<BattleGroundTeam> grandWinnersList) {
		short[] winnerCodeList = new short[grandWinnersList.size()];
		for(int i = 0; i < grandWinnersList.size(); i++) {
			winnerCodeList[i] = grandWinnersList.get(i).getTeamCode();
		}
		return winnerCodeList;
	}
	
	protected List<Match> generateMatchList(BotGenome genome, List<Tournament> tournaments) {
		List<Match> matches = new ArrayList<>();
		for(int j = 1; j < tournaments.size(); j++) {
			Tournament currentTournament = tournaments.get(j);
			if(currentTournament.getWinners().size() == 8) {
				for(int i = 0; i <= 7; i++) {
					Match match = this.getMatchByTournamentAndNumber(currentTournament, i, genome);
					matches.add(match);
				}
			}
		}
		
		return matches;
	}
	
	protected Match getMatchByTournamentAndNumber(Tournament tournament, int matchNumber, BotGenome genome) {
		Match match = null;
		BattleGroundTeam leftTeamName = BattleGroundTeam.NONE;
		BattleGroundTeam rightTeamName = BattleGroundTeam.NONE;
		Team leftTeam = null;
		Team rightTeam = null;
		Integer map = null;
		if(matchNumber == 0) {
			leftTeamName = BattleGroundTeam.RED; 
			rightTeamName = BattleGroundTeam.BLUE;
		} else if(matchNumber == 1) {
			leftTeamName = BattleGroundTeam.GREEN;
			rightTeamName = BattleGroundTeam.YELLOW;
		} else if(matchNumber == 2) {
			leftTeamName = BattleGroundTeam.WHITE;
			rightTeamName = BattleGroundTeam.BLACK;
		} else if(matchNumber == 3) {
			leftTeamName = BattleGroundTeam.PURPLE;
			rightTeamName = BattleGroundTeam.BROWN;
		} else if(matchNumber == 4) {
			BattleGroundTeam[] possibleSurvivors = new BattleGroundTeam[] {BattleGroundTeam.RED, BattleGroundTeam.BLUE, BattleGroundTeam.GREEN, BattleGroundTeam.YELLOW};
			List<String> relevantWinners = this.getRelevantEntries(tournament.getWinners(), 0, 1);
			GenericPairing<BattleGroundTeam, BattleGroundTeam> teams = this.getTeamsByDeterminingSurvivors(relevantWinners, possibleSurvivors);
			if(teams != null) {
				leftTeamName = teams.getLeft();
				rightTeamName = teams.getRight();
			} 
		} else if(matchNumber == 5) {
			BattleGroundTeam[] possibleSurvivors = new BattleGroundTeam[] {BattleGroundTeam.WHITE, BattleGroundTeam.BLACK, BattleGroundTeam.PURPLE, BattleGroundTeam.BROWN};
			List<String> relevantWinners = this.getRelevantEntries(tournament.getWinners(), 2, 3);
			GenericPairing<BattleGroundTeam, BattleGroundTeam> teams = this.getTeamsByDeterminingSurvivors(relevantWinners, possibleSurvivors);
			if(teams != null) {
				leftTeamName = teams.getLeft();
				rightTeamName = teams.getRight();
			} 
		} else if(matchNumber == 6) {
			BattleGroundTeam[] possibleSurvivors = new BattleGroundTeam[] {BattleGroundTeam.RED, BattleGroundTeam.BLUE, BattleGroundTeam.GREEN, BattleGroundTeam.YELLOW,
					BattleGroundTeam.WHITE, BattleGroundTeam.BLACK, BattleGroundTeam.PURPLE, BattleGroundTeam.BROWN};
			List<String> relevantWinners = this.getRelevantEntries(tournament.getWinners(), 4, 5);
			GenericPairing<BattleGroundTeam, BattleGroundTeam> teams = this.getTeamsByDeterminingSurvivors(relevantWinners, possibleSurvivors);
			if(teams != null) {
				leftTeamName = teams.getLeft();
				rightTeamName = teams.getRight();
			} 
		} else if(matchNumber == 7) {
			List<String> relevantWinners = this.getRelevantEntries(tournament.getWinners(), 6);
			if(relevantWinners.size() == 1) {
				leftTeamName = BattleGroundTeam.parse(relevantWinners.get(0));
				rightTeamName = BattleGroundTeam.CHAMPION;
			}
			
		}
		
		if(leftTeamName != null && rightTeamName != null) {
			leftTeam = tournament.getTeams().getTeamByBattleGroundTeam(leftTeamName);
			rightTeam = tournament.getTeams().getTeamByBattleGroundTeam(rightTeamName);
			map = Match.mapNumber(tournament.getMaps().get(matchNumber));
			match = new Match(leftTeamName, leftTeam, rightTeamName, rightTeam, map, genome);
		} else {
			match = new Match(BattleGroundTeam.NONE, null, BattleGroundTeam.NONE, null, map, genome);
		}
		
		return match;
	}
	
	protected List<String> getRelevantEntries(List<String> winners, int... indexes) {
		List<String> relevantWinners = new ArrayList<>();
		for(int index: indexes) {
			relevantWinners.add(winners.get(index));
		}
		
		return relevantWinners;
	}
	
	protected GenericPairing<BattleGroundTeam, BattleGroundTeam> getTeamsByDeterminingSurvivors(List<String> winners, BattleGroundTeam[] possibleSurvivors) {
		GenericPairing<BattleGroundTeam, BattleGroundTeam> teams = null;
		
		List<BattleGroundTeam> winnerTeams = winners.parallelStream().map(winner -> BattleGroundTeam.parse(winner)).collect(Collectors.toList());
		List<BattleGroundTeam> possibleSurvivorList = new ArrayList<>(Arrays.asList(possibleSurvivors));
		
		List<BattleGroundTeam> teamsToRemoveFromSurvivorList = new LinkedList<>();
		for(BattleGroundTeam possibleSurvivorTeam: possibleSurvivorList) {
			if(!winnerTeams.contains(possibleSurvivorTeam)) {
				teamsToRemoveFromSurvivorList.add(possibleSurvivorTeam);
			}
		}
		for(BattleGroundTeam teamToRemove: teamsToRemoveFromSurvivorList) {
			possibleSurvivorList.remove(teamToRemove);
		}
		
		if(possibleSurvivorList.size() == 2) {
			teams = new GenericPairing<>(possibleSurvivorList.get(0), possibleSurvivorList.get(1));
		}
		
		return teams;
	}
	
	protected Set<Integer> verifyAndFindProblematicMatches(List<BattleGroundTeam> grandWinnersList) {
		//first check if grand winners list and match count match
		if(grandWinnersList.size() != this.matchList.size()) {
			log.error("The grand winner list and match list sizes do not match.  Grand winners: {}  Matches: {}", 
					grandWinnersList.size(), this.matchList.size());
		}
		
		Set<Integer> problematicIndexes = new HashSet<>();
		
		//now lets look for matches that are null, or have null teams
		for(int i = 0; i < this.matchList.size(); i++) {
			Match currentMatch = this.matchList.get(i);
			boolean problemFound = currentMatch == null || currentMatch.getLeftTeam() == BattleGroundTeam.NONE || currentMatch.getLeftTeam() == null
					|| currentMatch.getRightTeam() == BattleGroundTeam.NONE || currentMatch.getRightTeam() == null;
			if(problemFound) {
				problematicIndexes.add(i);
			}
		}
		
		//now lets look for winners that are impossible to achieve
		for(int i = 0; i < grandWinnersList.size(); i++) {
			BattleGroundTeam winner = grandWinnersList.get(i);
			Match currentMatch = this.matchList.get(i);
			List<BattleGroundTeam> matchParticipants = Arrays.asList(new BattleGroundTeam[] {currentMatch.getLeftTeam(), currentMatch.getRightTeam()});
			if(!matchParticipants.contains(winner)) {
				problematicIndexes.add(i);
			}
		}
		
		return problematicIndexes;
	}
	
	protected void setWinnerToNoneForBadMatches(Set<Integer> problematicIndexes, List<BattleGroundTeam> grandWinnersList) {
		for(Integer i: problematicIndexes) {
			grandWinnersList.set(i, BattleGroundTeam.NONE);
		}
	}
}
