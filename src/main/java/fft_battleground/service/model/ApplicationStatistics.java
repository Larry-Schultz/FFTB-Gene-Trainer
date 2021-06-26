package fft_battleground.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationStatistics {
    private String cpuUsage;
    private String ramUsage;
    private Long generation;
    private HighScore highScore;
    private Boolean applicationComplete;
}
