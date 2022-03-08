package fft_battleground.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;

import fft_battleground.exception.TournamentApiException;
import fft_battleground.service.tournament.model.Tournament;
import fft_battleground.service.tournament.model.TournamentInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TournamentService {
	private static final String tournamentInfoApiUri = "https://fftbg.com/api/tournaments?limit=";
	private static final String tournamentApiBaseUri = "https://fftbg.com/api/tournament/";
	private static final String tipsApiUrl = "https://fftbg.com/api/tips";
	
	private RateLimiter limit = RateLimiter.create(1);
	
	private final Map<Long, File> tournamentFileMap = this.walkTournamentsFileCache();
	
	public TournamentService() {}
	
	public List<Tournament> getTournaments(List<TournamentInfo> tournamentInfoList) throws TournamentApiException {
		List<Tournament> tournaments = new ArrayList<Tournament>();
		tournamentInfoList.parallelStream().forEach(tournamentInfo -> {
			try {
				Tournament tournament = this.getTournamentById(tournamentInfo.getID());
				tournaments.add(tournament);
			} catch (TournamentApiException e) {
				log.error("Could not pull tournament data for id {}", tournamentInfo.getID(), e);
			}
		});
		Collections.sort(tournaments);
		
		return tournaments;
	}
	
	public List<TournamentInfo> getLatestTournamentInfo(Integer count) throws TournamentApiException {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<TournamentInfo[]> tournamentInfo;
		String url = tournamentInfoApiUri + count.toString();
		try {
			limit.acquire();
			tournamentInfo = restTemplate.getForEntity(url, TournamentInfo[].class);
		} catch(Exception e) {
			log.error("Error found getting latest tournament info", e);
			throw new TournamentApiException(e);
		}
		//TournamentInfo tournamentInfo = restTemplate.getForObject(tournamentInfoApiUri, TournamentInfo.class);
		List<TournamentInfo> tournamentInfoList = Arrays.asList(tournamentInfo.getBody());
		Collections.sort(tournamentInfoList);
		return tournamentInfoList;
	}
	
	public Tips getCurrentTips() throws TournamentApiException {
		Tips currentTip;
		Resource resource;
		try {
			limit.acquire();
			resource = new UrlResource(tipsApiUrl);
		} catch (MalformedURLException e) {
			log.error("Error found getting latest tournament info", e);
			throw new TournamentApiException(e);
		}
		Tips tips = new Tips(resource);
		currentTip = tips;
		
		return currentTip;
	}
	
	protected Tournament getTournamentById(Long id) throws TournamentApiException {
		Tournament tournament = null;
		if(this.tournamentFileMap.containsKey(id)) {
			tournament = this.readTournamentCacheFileById(id);
		} else {
			tournament = this.getTournamentFromApiById(id);
			this.writeTournamentCacheFileForId(id, tournament);
			log.info("Loading tournament {} from api", id);
		}
		
		return tournament;
	}
	
	protected Tournament getTournamentFromApiById(Long id) throws TournamentApiException {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<Tournament> latestTournament;
		try {
			limit.acquire();
			latestTournament = restTemplate.getForEntity(tournamentApiBaseUri + id.toString(), Tournament.class);
		} catch(Exception e) {
			log.error("Error found getting latest tournament info", e);
			throw new TournamentApiException(e);
		}
		return latestTournament.getBody();
	}
	
	protected Tournament readTournamentCacheFileById(Long id) throws TournamentApiException {
		File tournamentCacheFile = this.tournamentFileMap.get(id);
		Tournament tournament = null;
		try {
			FileInputStream fStream = new FileInputStream(tournamentCacheFile);
			GZIPInputStream zStream = new GZIPInputStream(new BufferedInputStream(fStream));
			ObjectMapper mapper = new ObjectMapper();
			tournament = mapper.readValue(zStream, Tournament.class);
		} catch (IOException e) {
			throw new TournamentApiException(e);
		}
		
		return tournament;
	}
	
	protected void writeTournamentCacheFileForId(Long id, Tournament tournament) throws TournamentApiException {
		String fileLocation = "tournamentsCache" + File.separator + id.toString() + ".zip";

		try {
			File newTournamentCacheFile = new File(fileLocation);
			FileOutputStream fStream = new FileOutputStream(newTournamentCacheFile);
			GZIPOutputStream zStream = new GZIPOutputStream(new BufferedOutputStream(fStream));
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(zStream, tournament);
		} catch (IOException e) {
			throw new TournamentApiException(e);
		}
	}
	
	protected Map<Long, File> walkTournamentsFileCache() {
		Map<Long, File> tournamentFileMap = new HashMap<>();
		
		File tournamentFileCacheFolder = new File("tournamentsCache");
		List<String> filenames = Arrays.asList(tournamentFileCacheFolder.list());
		for(String filename: filenames) {
			File tournamentFile = new File("tournamentsCache" + File.separator + filename);
			String cleanedFilename = StringUtils.replace(filename, ".zip", "");
			Long tournamentId = Long.valueOf(cleanedFilename);
			tournamentFileMap.put(tournamentId, tournamentFile);
		}
		
		return tournamentFileMap;
	}
	

}
