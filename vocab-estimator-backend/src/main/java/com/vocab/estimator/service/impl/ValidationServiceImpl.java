package com.vocab.estimator.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocab.estimator.algorithm.AlgorithmFactory;
import com.vocab.estimator.algorithm.AlgorithmResult;
import com.vocab.estimator.algorithm.AlgorithmValidator;
import com.vocab.estimator.dto.ValidationDTO;
import com.vocab.estimator.entity.ValidationSample;
import com.vocab.estimator.entity.VocWord;
import com.vocab.estimator.mapper.ValidationSampleMapper;
import com.vocab.estimator.mapper.VocWordMapper;
import com.vocab.estimator.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ValidationServiceImpl implements ValidationService {

    @Autowired private AlgorithmFactory algorithmFactory;
    @Autowired private AlgorithmValidator algorithmValidator;
    @Autowired private ValidationSampleMapper sampleMapper;
    @Autowired private VocWordMapper vocWordMapper;

    private final ObjectMapper om = new ObjectMapper();
    private final Random random = new Random();
    private Map<String, VocWord> wordCache = new HashMap<>();



    private String getChartDir() {
        String dir = System.getProperty("user.dir") + "/charts";
        new File(dir).mkdirs();
        return dir;
    }

    @Override
    public ValidationDTO validateAlgorithm(List<Map<String, Object>> validationData) {
        var validator = algorithmFactory.getAllAlgorithms().get(0);
        var report = algorithmValidator.validate(validationData, validator);
        List<ValidationDTO.ValidationItem> items = new ArrayList<>();
        for (var item : report.getItems()) {
            // item.getAlgorithmEstimate() is our algorithm's raw estimate (0-9000 from AlgorithmValidator)
            int di = item.getAlgorithmEstimate();
            int ci = item.getStandardEstimate();
            int diff = di - ci;
            int absErr = Math.abs(diff);
            double relErr = ci > 0 ? (double) absErr / ci : 0;
            items.add(new ValidationDTO.ValidationItem(
                item.getKnownWords(), item.getUnknownWords(),
                ci, di,
                di,
                di, diff, absErr, relErr));
        }
        return buildDTO(items, report.getMeanError(), report.getMeanBias(), report.getCorrelation(), report.getSampleCount());
    }

    @Override
    public ValidationDTO importAndValidate(String jsonData) {
        try {
            List<Map<String, Object>> data = om.readValue(jsonData, new TypeReference<List<Map<String, Object>>>(){});
            return validateAlgorithm(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse: " + e.getMessage());
        }
    }

    @Override
    public ValidationSample collectOne() {
        try {
            String rootDir = getRootDir();
            String scriptPath = rootDir + "/backend-scrape.js";
            String resultPath = rootDir + "/vocab_single.json";
            
            // Delete old result file before starting
            try { Files.deleteIfExists(Paths.get(resultPath)); } catch (Exception ignored) {}
            
            System.out.println("[collectOne] Launching scraper in NEW window...");
            // Use cmd /c start to open a new visible console window
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "\"Vocab Scraper\"", "node", scriptPath);
            pb.directory(new File(rootDir));
            pb.start();
            
            // Poll for result file (start returns immediately, cannot read stdout)
            long deadline = System.currentTimeMillis() + 120_000;
            File resultFile = new File(resultPath);
            while (System.currentTimeMillis() < deadline) {
                if (resultFile.exists() && resultFile.length() > 10) {
                    Thread.sleep(500);
                    break;
                }
                int secsLeft = (int)((deadline - System.currentTimeMillis()) / 1000);
                System.out.println("[collectOne] Waiting for result file... (" + secsLeft + "s left)");
                Thread.sleep(3000);
            }
            if (!resultFile.exists() || resultFile.length() < 10) {
                throw new RuntimeException("Scraper timed out or failed after 120s");
            }
            // resultFile already declared above
            if (!resultFile.exists()) throw new RuntimeException("Scraper failed");
            String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(resultPath)), java.nio.charset.StandardCharsets.UTF_8);
            com.fasterxml.jackson.databind.JsonNode resultNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            List<Map<String, Object>> wordResults = new ArrayList<>();
            int standardEstimate = resultNode.has("standardEstimate") ? resultNode.get("standardEstimate").asInt() : 0;
            // Read word+difficulty directly from scraper output (no guessDifficulty)
                // Step 1: Collect all word strings from scraper JSON
                List<String> allScrapedWords = new ArrayList<>();
                if (resultNode.has("knownWords")) {
                    for (var n : resultNode.get("knownWords")) {
                        allScrapedWords.add(n.isTextual() ? n.asText() : n.get("word").asText());
                    }
                }
                if (resultNode.has("unknownWords")) {
                    for (var n : resultNode.get("unknownWords")) {
                        allScrapedWords.add(n.isTextual() ? n.asText() : n.get("word").asText());
                    }
                }
                // Step 2: Look up actual difficulties from database
                Map<String, String> difficultyMap = lookupDifficulties(allScrapedWords);
                // Step 3: Build word items with correct difficulty from DB lookup
                if (resultNode.has("knownWords")) {
                    for (var n : resultNode.get("knownWords")) {
                        Map<String, Object> item = new HashMap<>();
                        String w = n.isTextual() ? n.asText() : n.get("word").asText();
                        item.put("word", w);
                        item.put("known", true);
                        String diff = difficultyMap.getOrDefault(w, "K");
                        // Skip words not in our database (UNKNOWN difficulty) to avoid polluting stats
                        if ("UNKNOWN".equals(diff)) continue;
                        item.put("difficulty", diff);
                        item.put("frequency", 0.5);
                        wordResults.add(item);
                    }
                }
                if (resultNode.has("unknownWords")) {
                    for (var n : resultNode.get("unknownWords")) {
                        Map<String, Object> item = new HashMap<>();
                        String w = n.isTextual() ? n.asText() : n.get("word").asText();
                        item.put("word", w);
                        item.put("known", false);
                        String diff = difficultyMap.getOrDefault(w, "K");
                        // Skip words not in our database (UNKNOWN difficulty) to avoid polluting stats
                        if ("UNKNOWN".equals(diff)) continue;
                        item.put("difficulty", diff);
                        item.put("frequency", 0.5);
                        wordResults.add(item);
                    }
                }
            int knownWordsSize = resultNode.has("knownWords") ? resultNode.get("knownWords").size() : 0;
            int unknownWordsSize = resultNode.has("unknownWords") ? resultNode.get("unknownWords").size() : 0;
            System.out.println("[collectOne] Known: " + knownWordsSize + ", Unknown: " + unknownWordsSize + ", Ci=" + standardEstimate);
            AlgorithmResult algorithmResult = algorithmFactory.estimateAll(wordResults);            // Debug: print difficulty distribution
            {
                Map<String,int[]> ds = new LinkedHashMap<>();
                ds.put("K",new int[]{0,0}); ds.put("P",new int[]{0,0});
                ds.put("F",new int[]{0,0}); ds.put("C",new int[]{0,0});
                for (Map<String,Object> wi : wordResults) {
                    String d = (String)wi.getOrDefault("difficulty","K");
                    if (!ds.containsKey(d)) d = "K";
                    boolean ik = (Boolean)wi.getOrDefault("known",false);
                    ds.get(d)[1]++; if (ik) ds.get(d)[0]++;
                }
                StringBuilder log = new StringBuilder("[collectOne] Difficulty breakdown:\n");
                for (String d : new String[]{"K","P","F","C"}) {
                    int[] s = ds.get(d);
                    log.append("  ").append(d).append(": ").append(s[0]).append("/").append(s[1])
                       .append(" (").append(s[1]>0?s[0]*100/s[1]:0).append("%)\n");
                }
                int kc = algorithmResult.getKnownCount();
                int tc = algorithmResult.getKnownCount() + algorithmResult.getUnknownCount();
                log.append("  Total: ").append(kc).append("/").append(tc).append(" known\n");
                log.append("  Di=").append(algorithmResult.getEstimate());
                System.out.println(log.toString());
            }

            int di = algorithmResult.getEstimate();
            int diff = di - standardEstimate;
            int absErr = Math.abs(diff);
            double relErr = standardEstimate > 0 ? (double) absErr / standardEstimate : 0;
            System.out.println("[collectOne] Di=" + di + ", Ci=" + standardEstimate + ", diff=" + diff);
            ValidationSample sample = new ValidationSample();
            java.util.List<String> knownStrList = new java.util.ArrayList<>();
            if (resultNode.has("knownWords")) for (var n : resultNode.get("knownWords")) knownStrList.add(n.isTextual() ? n.asText() : n.get("word").asText());
            java.util.List<String> unknownStrList = new java.util.ArrayList<>();
            if (resultNode.has("unknownWords")) for (var n : resultNode.get("unknownWords")) unknownStrList.add(n.isTextual() ? n.asText() : n.get("word").asText());
            sample.setKnownWords(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(knownStrList));
            sample.setUnknownWords(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(unknownStrList));
            sample.setStandardEstimate(standardEstimate);
            sample.setAlgorithmEstimate(di);  // Our algorithm estimate (0-40000 range)
            sample.setKnownCount(knownStrList.size());
            sample.setUnknownCount(unknownStrList.size());
            sample.setDiff(diff);
            sample.setAbsoluteError(absErr);
            sample.setRelativeError(relErr);
            sampleMapper.insert(sample);
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(resultPath)); } catch (Exception e) {}
            return sample;
        } catch (Exception e) {
            throw new RuntimeException("Collect failed: " + e.getMessage(), e);
        }
    }

    private String getRootDir() {
        String userDir = System.getProperty("user.dir");
        java.io.File dir = new java.io.File(userDir);
        while (dir != null && !new java.io.File(dir, "backend-scrape.js").exists()
               && !new java.io.File(new java.io.File(dir, "vocab-estimator-frontend"), "package.json").exists()) {
            dir = dir.getParentFile();
        }
        if (dir != null && dir.exists()) return dir.getAbsolutePath();
        return userDir;
    }

    private void preloadWordCache() {
        if (wordCache.isEmpty()) {
            List<VocWord> all = vocWordMapper.selectList(null);
            for (VocWord w : all) wordCache.put(w.getWord().toLowerCase(), w);
        }
    }

    private String guessDifficulty(String word) {
        int len = word.length();
        if (len <= 4) return "K";
        if (len <= 6) return "P";
        if (len <= 8) return "F";
        return "C";
    }

    @Override
    public List<ValidationSample> getHistory() {
        return sampleMapper.findAllOrderByTime();
    }

    @Override
    public ValidationDTO getStats() {
        List<ValidationSample> samples = sampleMapper.findAllOrderByTime();
        if (samples.isEmpty()) {
            ValidationDTO empty = new ValidationDTO();
            empty.setSampleCount(0);
            return empty;
        }
        return computeStats(samples);
    }

    @Override
    public ValidationDTO recalculateAllStats() {
        List<ValidationSample> samples = sampleMapper.findAllOrderByTime();
        if (samples.isEmpty()) {
            ValidationDTO empty = new ValidationDTO();
            empty.setSampleCount(0);
            return empty;
        }

        // Update abs/rel error for all samples (keep original Di, only recompute errors)
        for (ValidationSample s : samples) {
            int di = s.getAlgorithmEstimate() != null ? s.getAlgorithmEstimate() : 0;
            int ci = s.getStandardEstimate() != null ? s.getStandardEstimate() : 0;
            int diff = di - ci;
            int absErr = Math.abs(diff);
            double relErr = ci > 0 ? (double) absErr / ci : 0;
            s.setDiff(diff);
            s.setAbsoluteError(absErr);
            s.setRelativeError(relErr);
            sampleMapper.updateById(s);
        }

        return computeStats(samples);
    }

    private ValidationDTO computeStats(List<ValidationSample> samples) {
        int n = samples.size();

        // Basic stats
        double meanError = samples.stream().mapToInt(ValidationSample::getAbsoluteError).average().orElse(0);
        double meanBias = samples.stream().mapToInt(ValidationSample::getDiff).average().orElse(0);
        double mse = samples.stream().mapToDouble(s -> Math.pow(s.getDiff(), 2)).average().orElse(0);
        double rmse = Math.sqrt(mse);
        double maxError = samples.stream().mapToInt(ValidationSample::getAbsoluteError).max().orElse(0);
        double minError = samples.stream().mapToInt(ValidationSample::getAbsoluteError).min().orElse(0);
        double meanRelErr = samples.stream().mapToDouble(ValidationSample::getRelativeError).average().orElse(0);

        // Correlation
        double correlation = computeCorrelation(samples);

        // Error distribution
        int within500 = 0, within1000 = 0, within2000 = 0, beyond2000 = 0;
        for (ValidationSample s : samples) {
            int ae = s.getAbsoluteError();
            if (ae <= 500) within500++;
            else if (ae <= 1000) within1000++;
            else if (ae <= 2000) within2000++;
            else beyond2000++;
        }

        // Build items
        List<ValidationDTO.ValidationItem> items = new ArrayList<>();
        for (ValidationSample s : samples) {
            List<String> known = parseList(s.getKnownWords());
            List<String> unknown = parseList(s.getUnknownWords());
            int scaledAlg = s.getAlgorithmEstimate() != null ? s.getAlgorithmEstimate() : 0;
            items.add(new ValidationDTO.ValidationItem(
                known, unknown, s.getStandardEstimate(), scaledAlg,
                scaledAlg,
                scaledAlg, s.getDiff(), s.getAbsoluteError(), s.getRelativeError()));
        }

        // Chart data
        List<ValidationDTO.ChartPoint> scatterData = samples.stream()
            .map(s -> new ValidationDTO.ChartPoint(s.getStandardEstimate(), s.getAlgorithmEstimate() != null ? s.getAlgorithmEstimate() : 0))
            .collect(Collectors.toList());

        List<ValidationDTO.DistributionBin> histogramData = buildHistogram(samples);

        // Generate chart images
        String scatterUrl = generateScatterChart(samples);
        String histUrl = generateHistogramChart(samples);

        ValidationDTO dto = buildDTO(items, meanError, meanBias, correlation, n);
        dto.setMse(mse);
        dto.setRmse(rmse);
        dto.setMaxError(maxError);
        dto.setMinError(minError);
        dto.setMeanRelativeError(meanRelErr);
        dto.setErrorDistribution(new ValidationDTO.ErrorDistribution(within500, within1000, within2000, beyond2000));
        dto.setScatterData(scatterData);
        dto.setHistogramData(histogramData);
        dto.setScatterChartUrl(scatterUrl);
        dto.setHistogramChartUrl(histUrl);

        return dto;
    }

    private List<String> parseList(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        if (json.startsWith("[")) {
            try {
                return om.readValue(json, new TypeReference<List<String>>(){});
            } catch (Exception e) {
                // fall through
            }
        }
        return Arrays.asList(json.split("[,;]"));
    }

    private double computeCorrelation(List<ValidationSample> samples) {
        int n = samples.size();
        if (n < 2) return 0;
        double sumStd = samples.stream().mapToInt(ValidationSample::getStandardEstimate).sum();
        double sumAlg = samples.stream().mapToInt(ValidationSample::getAlgorithmEstimate).sum();
        double meanStd = sumStd / n;
        double meanAlg = sumAlg / n;
        double cov = 0, varStd = 0, varAlg = 0;
        for (ValidationSample s : samples) {
            double dStd = s.getStandardEstimate() - meanStd;
            double dAlg = s.getAlgorithmEstimate() - meanAlg;
            cov += dStd * dAlg;
            varStd += dStd * dStd;
            varAlg += dAlg * dAlg;
        }
        double denom = Math.sqrt(varStd * varAlg);
        return denom > 0 ? cov / denom : 0;
    }

    private List<ValidationDTO.DistributionBin> buildHistogram(List<ValidationSample> samples) {
        int[] diffs = samples.stream().mapToInt(ValidationSample::getDiff).toArray();
        if (diffs.length == 0) return new ArrayList<>();

        int min = Arrays.stream(diffs).min().orElse(-3000);
        int max = Arrays.stream(diffs).max().orElse(3000);
        int range = max - min;
        if (range == 0) range = 1000;
        int binCount = Math.min(10, Math.max(5, diffs.length / 10));
        double binSize = (double) range / binCount;

        List<ValidationDTO.DistributionBin> bins = new ArrayList<>();
        for (int i = 0; i < binCount; i++) {
            double binMin = min + i * binSize;
            double binMax = binMin + binSize;
            int finalI = i;
            int count = (int) Arrays.stream(diffs)
                .filter(d -> d >= binMin && (finalI == binCount - 1 ? d <= binMax : d < binMax))
                .count();
            bins.add(new ValidationDTO.DistributionBin(binMin, binMax, count));
        }
        return bins;
    }

    private String generateScatterChart(List<ValidationSample> samples) {
        try {
            int w = 600, h = 400;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);

            // Margins
            int ml = 60, mr = 20, mt = 40, mb = 50;
            int pw = w - ml - mr, ph = h - mt - mb;

            // Title
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("Ci-Di Scatter Plot", ml, 25);

            // Find ranges
            int maxCi = samples.stream().mapToInt(ValidationSample::getStandardEstimate).max().orElse(15000);
            int maxDi = samples.stream().mapToInt(ValidationSample::getAlgorithmEstimate).max().orElse(15000);
            int maxVal = Math.max(maxCi, maxDi) + 1000;

            // Draw axes
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(ml, mt, ml, mt + ph);
            g.drawLine(ml, mt + ph, ml + pw, mt + ph);

            // Axis labels
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int i = 0; i <= 5; i++) {
                int val = i * maxVal / 5;
                int x = ml + i * pw / 5;
                int y = mt + ph - i * ph / 5;
                g.drawString(String.valueOf(val), ml - 35, y + 4);
                g.drawString(String.valueOf(val), x - 10, mt + ph + 15);
            }

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString("Ci (TestYourVocab)", ml + pw / 2 - 50, mt + ph + 35);
            g.translate(12, mt + ph / 2 + 20);
            g.rotate(-Math.PI / 2);
            g.drawString("Di_prop (Proportion)", -50, 0);
            g.rotate(Math.PI / 2);
            g.translate(-12, -(mt + ph / 2 + 20));

            // Ideal line y=x
            g.setColor(new Color(200, 200, 200));
            g.drawLine(ml, mt + ph, ml + pw, mt);

            // Scatter points
            for (ValidationSample s : samples) {
                int ci = s.getStandardEstimate();
                int di = s.getAlgorithmEstimate() != null ? s.getAlgorithmEstimate() : 0;
                int px = ml + (int)((double) ci / maxVal * pw);
                int py = mt + ph - (int)((double) di / maxVal * ph);
                g.setColor(new Color(64, 158, 255, 180));
                g.fillOval(px - 3, py - 3, 6, 6);
            }

            g.dispose();
            String path = getChartDir() + "/scatter.png";
            ImageIO.write(img, "PNG", new File(path));
            return "/charts/scatter.png";
        } catch (Exception e) {
            return "";
        }
    }

    private String generateHistogramChart(List<ValidationSample> samples) {
        try {
            int w = 600, h = 400;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);

            int ml = 60, mr = 20, mt = 40, mb = 50;
            int pw = w - ml - mr, ph = h - mt - mb;

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("Error Distribution (Di - Ci)", ml, 25);

            int[] diffs = samples.stream().mapToInt(ValidationSample::getDiff).toArray();
            int min = Arrays.stream(diffs).min().orElse(-3000);
            int max = Arrays.stream(diffs).max().orElse(3000);
            int range = max - min;
            if (range == 0) range = 1000;
            int binCount = Math.min(10, Math.max(5, diffs.length / 10));
            double binSize = (double) range / binCount;

            // Count frequencies
            int[] bins = new int[binCount];
            for (int d : diffs) {
                int idx = (int)((d - min) / binSize);
                if (idx >= binCount) idx = binCount - 1;
                if (idx < 0) idx = 0;
                bins[idx]++;
            }
            int maxFreq = Arrays.stream(bins).max().orElse(1);

            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int i = 0; i <= 5; i++) {
                int val = i * maxFreq / 5;
                int y = mt + ph - i * ph / 5;
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(ml, y, ml + pw, y);
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(val), ml - 35, y + 4);
            }

            // Draw bars
            for (int i = 0; i < binCount; i++) {
                int barW = pw / binCount - 4;
                int barH = (int)((double) bins[i] / maxFreq * ph);
                int x = ml + i * pw / binCount + 2;
                int y = mt + ph - barH;
                g.setColor(new Color(64, 158, 255, 200));
                g.fillRect(x, y, barW, barH);
                g.setColor(Color.BLACK);
                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                String label = String.valueOf((int)(min + i * binSize));
                g.drawString(label, x - 5, mt + ph + 15);
            }

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString("Error (Di - Ci)", ml + pw / 2 - 30, mt + ph + 35);
            g.translate(12, mt + ph / 2);
            g.rotate(-Math.PI / 2);
            g.drawString("Frequency", -20, 0);
            g.rotate(Math.PI / 2);
            g.translate(-12, -(mt + ph / 2));

            g.dispose();
            String path = getChartDir() + "/histogram.png";
            ImageIO.write(img, "PNG", new File(path));
            return "/charts/histogram.png";
        } catch (Exception e) {
            return "";
        }
    }

    private ValidationDTO buildDTO(List<ValidationDTO.ValidationItem> items,
                                   double meanError, double meanBias, double correlation, int n) {
        ValidationDTO dto = new ValidationDTO();
        dto.setItems(items);
        dto.setMeanError(meanError);
        dto.setMeanBias(meanBias);
        dto.setCorrelation(correlation);
        dto.setSampleCount(n);
        dto.setOurMaxVocab(40000);
        dto.setTvyMaxVocab(40000);
        dto.setScaleFactor(1.0);
        return dto;
    }

    @Override
    public Map<String, String> lookupDifficulties(List<String> words) {
        preloadWordCache();
        Map<String, String> result = new HashMap<>();
        for (String w : words) {
            String key = w.toLowerCase().trim();
            VocWord vw = wordCache.get(key);
            result.put(w, vw != null ? vw.getDifficulty() : "UNKNOWN");
        }
        return result;
    }

    @Override
    public void clearAll() {
        List<ValidationSample> all = sampleMapper.findAllOrderByTime();
        for (ValidationSample s : all) {
            sampleMapper.deleteById(s.getId());
        }
    }
}

