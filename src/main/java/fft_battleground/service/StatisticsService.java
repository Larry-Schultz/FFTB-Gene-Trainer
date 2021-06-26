package fft_battleground.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fft_battleground.gene.MatchManager;
import fft_battleground.service.model.ApplicationStatistics;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

public class StatisticsService {

    private static final Logger log_ = LoggerFactory.getLogger(StatisticsService.class);

    private SystemInfo si;
    private MatchManager matchManagerRef;

    public StatisticsService(MatchManager matchManager) {
        this.si = new SystemInfo();
        this.matchManagerRef = matchManager;
    }

    public ApplicationStatistics getApplicationStatistics() {
        HardwareAbstractionLayer hardware = this.si.getHardware();
        Double cpuLoad = this.getProcessorLoad(hardware.getProcessor());
        Double memoryUsage = this.getMemoryUsage(hardware.getMemory());

        DecimalFormat decimalFormat = new DecimalFormat("#.00");

        String cpuLoadString = null;
        String memoryUsageString = null;
        String totalMarkovChainsString = null;

        if (cpuLoad != null && cpuLoad > 0) {
            cpuLoadString = decimalFormat.format(cpuLoad) + "%";
        }
        if (memoryUsage != null && memoryUsage > 0) {
            memoryUsageString = decimalFormat.format(memoryUsage) + "%";
        }

        DecimalFormat decimalAndCommaFormat = new DecimalFormat("#,###");
        ApplicationStatistics applicationStatistics = new ApplicationStatistics(cpuLoadString, memoryUsageString, 
        		this.matchManagerRef.getGeneration(), this.matchManagerRef.getHighScore(), false);

        return applicationStatistics;
    }

    public Double getProcessorLoad(CentralProcessor processor) {
        double processorLoad = processor.getSystemCpuLoadBetweenTicks() * 100;
        if (processorLoad > 0) {
            return processorLoad;
        } else {
            return null;
        }
    }

    public Double getMemoryUsage(GlobalMemory memory) {
        long availableMemory = memory.getAvailable();
        long totalMemory = memory.getTotal();

        if (availableMemory > 0 && totalMemory > 0) {
            BigDecimal availableMemoryDouble = new BigDecimal(availableMemory);
            BigDecimal totalMemoryDouble = new BigDecimal(totalMemory);
            BigDecimal percentAvailable = availableMemoryDouble.divide(totalMemoryDouble, 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
            BigDecimal percentUsed = (new BigDecimal(100)).subtract(percentAvailable);
            return percentUsed.doubleValue();
        } else {
            return null;
        }
    }

}