package fft_battleground.service.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HighScore {
	private Long highScore;
	private Integer percentage;
	private Integer numberOfMatchesCorrect;
	private Integer numberOfMatches;
	private Integer maxLevel;
	private Long numberAtMaxLevel;
	private Integer minLevel;
	private Long numberAtMinLevel;
	private Integer numberOfGenes;
	private String updateDate;
}
