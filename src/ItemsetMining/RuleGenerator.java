package ItemsetMining;

import java.io.*;
import java.util.*;

public class RuleGenerator {

    private Map<Set<Integer>, Double> frequentItemsets = new HashMap<>();

    private int rulesChecked = 0;
    private int rulesGenerated = 0;
    private int rulesPruned = 0;
    private long algoTime = 0;

    private List<RuleMetrics> validRules = new ArrayList<>();

    class RuleMetrics {
        Set<Integer> antecedent;
        Set<Integer> consequent;
        double support;
        double confidence;
        double lift;
        double conviction;

        RuleMetrics(Set<Integer> a, Set<Integer> c, double sup, double conf, double lift, double conv) {
            this.antecedent = a; this.consequent = c;
            this.support = sup; this.confidence = conf;
            this.lift = lift; this.conviction = conv;
        }
    }

    public static void main(String[] args) {
        RuleGenerator generator = new RuleGenerator();
        double minConfidence = 0.8;

        // Cập nhật inputFile để khớp với cấu trúc tên file mới của Apriori
        String inputFile = "results/apriori_FP_chess_0.5.txt";

        // Thêm minConfidence vào tên file xuất ra
        String baseFileName = "results/apriori_AR_chess_" + minConfidence;

        try {
            generator.loadFrequentItemsets(inputFile);
            generator.generateRulesAndExport(baseFileName, minConfidence);
            System.out.println("Đã sinh luật xong! Vui lòng kiểm tra các file .txt và .csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFrequentItemsets(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            if (line != null && !line.contains(":")) line = br.readLine();

            while (line != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        String[] items = parts[0].trim().split("\\s+");
                        double support = Double.parseDouble(parts[1].trim());

                        Set<Integer> itemSet = new HashSet<>();
                        for (String s : items) itemSet.add(Integer.parseInt(s));

                        frequentItemsets.put(itemSet, support);
                    }
                }
                line = br.readLine();
            }
        }
    }

    public void generateRulesAndExport(String baseFileName, double minConfidence) throws IOException {
        rulesChecked = 0; rulesGenerated = 0; rulesPruned = 0;
        validRules.clear();

        long startTime = System.currentTimeMillis();

        for (Set<Integer> fk : frequentItemsets.keySet()) {
            if (fk.size() < 2) continue;

            List<Set<Integer>> H1 = new ArrayList<>();

            for (Integer item : fk) {
                Set<Integer> consequent = new HashSet<>(Collections.singletonList(item));
                Set<Integer> antecedent = new HashSet<>(fk);
                antecedent.remove(item);

                rulesChecked++;

                double supFK = frequentItemsets.get(fk);
                double supA = frequentItemsets.get(antecedent);
                double conf = supFK / supA;

                if (conf >= minConfidence) {
                    recordRule(antecedent, consequent, supFK, conf);
                    H1.add(consequent);
                } else {
                    rulesPruned++;
                }
            }
            aprioriGenRules(fk, H1, minConfidence);
        }

        algoTime = System.currentTimeMillis() - startTime;

        exportToTXT(baseFileName + ".txt", minConfidence);
        exportRulesToCSV(baseFileName + "_details.csv");
        exportStatsToCSV(baseFileName + "_stats.csv", minConfidence);
    }

    private void aprioriGenRules(Set<Integer> fk, List<Set<Integer>> Hm, double minConfidence) {
        int k = fk.size();
        int m = Hm.isEmpty() ? 0 : Hm.get(0).size();

        if (k > m + 1 && !Hm.isEmpty()) {
            List<Set<Integer>> HmPlus1 = generateAprioriConsequents(Hm);

            Iterator<Set<Integer>> it = HmPlus1.iterator();
            while (it.hasNext()) {
                Set<Integer> consequent = it.next();
                Set<Integer> antecedent = new HashSet<>(fk);
                antecedent.removeAll(consequent);

                rulesChecked++;

                double supFK = frequentItemsets.get(fk);
                double supA = frequentItemsets.get(antecedent);
                double conf = supFK / supA;

                if (conf >= minConfidence) {
                    recordRule(antecedent, consequent, supFK, conf);
                } else {
                    it.remove();
                    rulesPruned++;
                }
            }
            aprioriGenRules(fk, HmPlus1, minConfidence);
        }
    }

    private List<Set<Integer>> generateAprioriConsequents(List<Set<Integer>> Hm) {
        List<Set<Integer>> candidates = new ArrayList<>();
        for (int i = 0; i < Hm.size(); i++) {
            for (int j = i + 1; j < Hm.size(); j++) {
                Set<Integer> c1 = Hm.get(i);
                Set<Integer> c2 = Hm.get(j);
                Set<Integer> union = new HashSet<>(c1);
                union.addAll(c2);

                if (union.size() == c1.size() + 1 && !candidates.contains(union)) {
                    candidates.add(union);
                }
            }
        }
        return candidates;
    }

    private void recordRule(Set<Integer> antecedent, Set<Integer> consequent, double sup, double conf) {
        double supB = frequentItemsets.getOrDefault(consequent, 0.0);

        double lift = (supB > 0) ? conf / supB : 0.0;

        double conviction;
        if (conf == 1.0) {
            conviction = Double.POSITIVE_INFINITY;
        } else {
            conviction = (1.0 - supB) / (1.0 - conf);
        }

        validRules.add(new RuleMetrics(antecedent, consequent, sup, conf, lift, conviction));
        rulesGenerated++;
    }

    private void exportToTXT(String path, double minConf) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("=== DANH SÁCH LUẬT KẾT HỢP (MinConf = " + minConf + ") ===");
            for (RuleMetrics rm : validRules) {
                String rule = formatRule(rm.antecedent) + " -> " + formatRule(rm.consequent);
                writer.println(rule + " : sup = " + String.format(Locale.US, "%.2f", rm.support) +
                        ", conf = " + String.format(Locale.US, "%.2f", rm.confidence) +
                        ", lift = " + String.format(Locale.US, "%.2f", rm.lift));
            }
            writer.println("\n=== BÁO CÁO HIỆU NĂNG ===");
            writer.println("Tổng số luật sinh ra: " + rulesGenerated);
            writer.println("Số luật phải kiểm tra Support/Conf: " + rulesChecked);
            writer.println("Số nhánh bị Tỉa (Pruned): " + rulesPruned);
            writer.println("Thời gian chạy (ms): " + algoTime);
        }
    }

    private void exportRulesToCSV(String path) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("Antecedent,Consequent,Support,Confidence,Lift,Conviction");
            for (RuleMetrics rm : validRules) {
                String convStr = Double.isInfinite(rm.conviction) ? "Infinity" : String.format(Locale.US, "%.4f", rm.conviction);
                writer.println(
                        "\"" + formatRule(rm.antecedent) + "\",\"" + formatRule(rm.consequent) + "\"," +
                                String.format(Locale.US, "%.4f", rm.support) + "," +
                                String.format(Locale.US, "%.4f", rm.confidence) + "," +
                                String.format(Locale.US, "%.4f", rm.lift) + "," + convStr
                );
            }
        }
    }

    private void exportStatsToCSV(String path, double minConf) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("MinConfidence,RulesGenerated,RulesChecked,BranchesPruned,ExecutionTime_ms");
            writer.println(minConf + "," + rulesGenerated + "," + rulesChecked + "," + rulesPruned + "," + algoTime);
        }
    }

    private String formatRule(Set<Integer> set) {
        List<Integer> sorted = new ArrayList<>(set);
        Collections.sort(sorted);
        return sorted.toString().replaceAll("[\\[\\],]", "");
    }
}