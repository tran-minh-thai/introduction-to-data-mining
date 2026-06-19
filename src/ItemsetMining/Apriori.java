package ItemsetMining;

import java.io.*;
import java.util.*;

public class Apriori {

    private static long peakMemory = 0;
    private static long ioTime = 0;
    private static long algoTime = 0;
    private static int totalTransactions = 0;

    public static void main(String[] args) {
        String filePath = "datasets/chess.txt";
        double minSupport = 0.5;
        // Đã thêm minSupport vào tên file
        String baseOutputPath = "results/apriori_FP_chess_" + minSupport;

        try {
            executeAndExport(filePath, baseOutputPath, minSupport);
            System.out.println("Đã chạy xong ItemsetMining.Apriori! Vui lòng kiểm tra các file kết quả (.txt và .csv) trong thư mục results.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void executeAndExport(String filePath, String baseOutputPath, double minSupport) throws IOException {
        peakMemory = 0;
        updatePeakMemory();
        List<String[]> csvRows = new ArrayList<>();

        String datasetName = new File(filePath).getName().replaceFirst("[.][^.]+$", "");

        long startIO = System.currentTimeMillis();
        List<Set<Integer>> transactions = loadData(filePath);
        ioTime = System.currentTimeMillis() - startIO;
        totalTransactions = transactions.size();

        int minCount = (int) Math.ceil(minSupport * totalTransactions);

        long startAlgo = System.currentTimeMillis();
        Map<Set<Integer>, Integer> finalFrequentItemsets = new LinkedHashMap<>();

        Map<Set<Integer>, Integer> candidates = new HashMap<>();
        for (Set<Integer> trans : transactions) {
            for (Integer item : trans) {
                Set<Integer> s = new HashSet<>(Collections.singletonList(item));
                candidates.put(s, candidates.getOrDefault(s, 0) + 1);
            }
        }
        Map<Set<Integer>, Integer> currentFrequent = filter(candidates, minCount);
        finalFrequentItemsets.putAll(currentFrequent);

        updatePeakMemory();

        csvRows.add(new String[]{
                datasetName, String.valueOf(minSupport), String.valueOf(totalTransactions),
                "1", String.valueOf(candidates.size()), String.valueOf(currentFrequent.size()), "0", "0"
        });

        int k = 2;
        while (!currentFrequent.isEmpty()) {
            long[] pruneStats = new long[2];

            Map<Set<Integer>, Integer> nextCandidates = generate(currentFrequent.keySet(), k, pruneStats);

            for (Set<Integer> trans : transactions) {
                for (Set<Integer> cand : nextCandidates.keySet()) {
                    if (trans.containsAll(cand)) {
                        nextCandidates.put(cand, nextCandidates.get(cand) + 1);
                    }
                }
            }

            currentFrequent = filter(nextCandidates, minCount);
            if (currentFrequent.isEmpty()) break;

            finalFrequentItemsets.putAll(currentFrequent);
            updatePeakMemory();

            csvRows.add(new String[]{
                    datasetName, String.valueOf(minSupport), String.valueOf(totalTransactions),
                    String.valueOf(k), String.valueOf(nextCandidates.size() + pruneStats[0]),
                    String.valueOf(currentFrequent.size()), String.valueOf(pruneStats[0]), String.valueOf(pruneStats[1])
            });

            k++;
        }

        algoTime = System.currentTimeMillis() - startAlgo;

        saveResult(baseOutputPath + ".txt", finalFrequentItemsets);
        saveMetricsToCSV(baseOutputPath + ".csv", csvRows);
    }

    private static Map<Set<Integer>, Integer> generate(Set<Set<Integer>> prevFrequent, int k, long[] pruneStats) {
        Map<Set<Integer>, Integer> candidates = new HashMap<>();
        List<Set<Integer>> list = new ArrayList<>(prevFrequent);

        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                Set<Integer> union = new HashSet<>(list.get(i));
                union.addAll(list.get(j));

                if (union.size() == k && !candidates.containsKey(union)) {
                    boolean isPruned = false;
                    for (Integer item : union) {
                        pruneStats[1]++;

                        Set<Integer> subset = new HashSet<>(union);
                        subset.remove(item);

                        if (!prevFrequent.contains(subset)) {
                            isPruned = true;
                            break;
                        }
                    }

                    if (isPruned) {
                        pruneStats[0]++;
                    } else {
                        candidates.put(union, 0);
                    }
                }
            }
        }
        return candidates;
    }

    private static List<Set<Integer>> loadData(String path) throws IOException {
        List<Set<Integer>> transactions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Set<Integer> transaction = new HashSet<>();
                for (String s : line.trim().split("\\s+")) {
                    transaction.add(Integer.parseInt(s));
                }
                transactions.add(transaction);
            }
        }
        return transactions;
    }

    private static Map<Set<Integer>, Integer> filter(Map<Set<Integer>, Integer> candidates, int minCount) {
        Map<Set<Integer>, Integer> filtered = new HashMap<>();
        candidates.forEach((key, v) -> { if (v >= minCount) filtered.put(key, v); });
        return filtered;
    }

    private static void saveResult(String outputPath, Map<Set<Integer>, Integer> result) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            for (Map.Entry<Set<Integer>, Integer> entry : result.entrySet()) {
                String items = entry.getKey().toString().replaceAll("[\\[\\],]", "");
                writer.println(items + " : " + entry.getValue());
            }
        }
    }

    private static void saveMetricsToCSV(String outputPath, List<String[]> loopStats) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("Dataset,MinSupport,TotalTxns,k,CandidateSize(C_k),FrequentSize(L_k),PrunedCandidates,SubsetChecks,IO_Time(ms),Algo_Time(ms),PeakMemory(MB)");
            for (String[] stat : loopStats) {
                String row = String.join(",", stat) + "," +
                        ioTime + "," +
                        algoTime + "," +
                        String.format(Locale.US, "%.2f", peakMemory / (1024.0 * 1024.0));
                writer.println(row);
            }
        }
    }

    private static void updatePeakMemory() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        if (usedMemory > peakMemory) {
            peakMemory = usedMemory;
        }
    }
}