package ItemsetMining;

import java.io.*;
import java.util.*;

public class FPGrowth {

    private static long peakMemory = 0;
    private static long ioTime = 0;
    private static long algoTime = 0;
    private static int totalTransactions = 0;
    private static int globalNodeCount = 0;
    private static int conditionalTreeCount = 0;
    private static int totalPatternsFound = 0;

    public static void main(String[] args) {
        String filePath = "datasets/chess.txt";
        double minSupport = 0.5;
        // Đã thêm minSupport vào tên file
        String baseOutputPath = "results/fpgrowth_FP_chess_" + minSupport;

        try {
            executeAndExport(filePath, baseOutputPath, minSupport);
            System.out.println("Đã chạy xong FP-Growth! Vui lòng kiểm tra các file .txt và .csv trong thư mục results");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void executeAndExport(String filePath, String baseOutputPath, double minSupport) throws IOException {
        peakMemory = 0; updatePeakMemory();
        globalNodeCount = 0; conditionalTreeCount = 0; totalPatternsFound = 0;

        String datasetName = new File(filePath).getName().replaceFirst("[.][^.]+$", "");

        long startIO = System.currentTimeMillis();
        List<List<Integer>> transactions = loadData(filePath);
        totalTransactions = transactions.size();
        ioTime = System.currentTimeMillis() - startIO;

        int minCount = (int) Math.ceil(minSupport * totalTransactions);

        long startAlgo = System.currentTimeMillis();

        Map<Integer, Integer> frequencyMap = new HashMap<>();
        for (List<Integer> trans : transactions) {
            for (Integer item : trans) {
                frequencyMap.put(item, frequencyMap.getOrDefault(item, 0) + 1);
            }
        }

        List<Integer> L1 = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() >= minCount) {
                L1.add(entry.getKey());
            }
        }
        L1.sort((a, b) -> {
            int cmp = frequencyMap.get(b).compareTo(frequencyMap.get(a));
            if (cmp == 0) return a.compareTo(b);
            return cmp;
        });

        Map<Integer, Integer> headerTableFreq = new HashMap<>();
        for (Integer item : L1) headerTableFreq.put(item, frequencyMap.get(item));

        List<List<Integer>> filteredTransactions = new ArrayList<>();
        int totalItemsInFilteredDB = 0;
        for (List<Integer> trans : transactions) {
            List<Integer> filteredTrans = new ArrayList<>();
            for (Integer item : trans) {
                if (headerTableFreq.containsKey(item)) filteredTrans.add(item);
            }
            if (!filteredTrans.isEmpty()) {
                filteredTrans.sort((a, b) -> {
                    int cmp = headerTableFreq.get(b).compareTo(headerTableFreq.get(a));
                    if (cmp == 0) return a.compareTo(b);
                    return cmp;
                });
                filteredTransactions.add(filteredTrans);
                totalItemsInFilteredDB += filteredTrans.size();
            }
        }

        FPTree globalTree = new FPTree();
        for (List<Integer> trans : filteredTransactions) {
            globalTree.insertTransaction(trans);
        }
        globalNodeCount = globalTree.nodeCount;

        Map<Set<Integer>, Integer> frequentPatterns = new LinkedHashMap<>();
        mineTree(globalTree, headerTableFreq, minCount, new HashSet<>(), frequentPatterns);

        totalPatternsFound = frequentPatterns.size();
        algoTime = System.currentTimeMillis() - startAlgo;

        saveResult(baseOutputPath + ".txt", frequentPatterns);
        double compressionRatio = globalNodeCount == 0 ? 0 : (double) totalItemsInFilteredDB / globalNodeCount;
        saveMetricsToCSV(baseOutputPath + ".csv", datasetName, minSupport, L1.size(), compressionRatio);
    }

    private static void mineTree(FPTree tree, Map<Integer, Integer> headerTable, int minCount,
                                 Set<Integer> prefix, Map<Set<Integer>, Integer> result) {
        updatePeakMemory();

        List<Integer> items = new ArrayList<>(headerTable.keySet());
        items.sort((a, b) -> {
            int cmp = headerTable.get(a).compareTo(headerTable.get(b));
            if (cmp == 0) return b.compareTo(a);
            return cmp;
        });

        for (Integer item : items) {
            Set<Integer> newPattern = new HashSet<>(prefix);
            newPattern.add(item);
            result.put(newPattern, headerTable.get(item));

            List<List<Integer>> cpb = new ArrayList<>();
            List<Integer> cpbCounts = new ArrayList<>();
            FPNode currentNode = tree.headerTableMap.get(item);

            while (currentNode != null) {
                List<Integer> path = new ArrayList<>();
                FPNode parent = currentNode.parent;
                while (parent != null && parent.item != null) {
                    path.add(parent.item);
                    parent = parent.parent;
                }
                if (!path.isEmpty()) {
                    Collections.reverse(path);
                    cpb.add(path);
                    cpbCounts.add(currentNode.count);
                }
                currentNode = currentNode.nodeLink;
            }

            Map<Integer, Integer> condHeaderFreq = new HashMap<>();
            for (int i = 0; i < cpb.size(); i++) {
                List<Integer> path = cpb.get(i);
                int count = cpbCounts.get(i);
                for (Integer pItem : path) {
                    condHeaderFreq.put(pItem, condHeaderFreq.getOrDefault(pItem, 0) + count);
                }
            }

            condHeaderFreq.entrySet().removeIf(entry -> entry.getValue() < minCount);

            if (!condHeaderFreq.isEmpty()) {
                FPTree condTree = new FPTree();
                conditionalTreeCount++;

                for (int i = 0; i < cpb.size(); i++) {
                    List<Integer> path = cpb.get(i);
                    int count = cpbCounts.get(i);
                    List<Integer> filteredPath = new ArrayList<>();
                    for (Integer pItem : path) {
                        if (condHeaderFreq.containsKey(pItem)) filteredPath.add(pItem);
                    }
                    if (!filteredPath.isEmpty()) {
                        filteredPath.sort((a, b) -> {
                            int cmp = condHeaderFreq.get(b).compareTo(condHeaderFreq.get(a));
                            if (cmp == 0) return a.compareTo(b);
                            return cmp;
                        });
                        condTree.insertTransactionWithCount(filteredPath, count);
                    }
                }

                if (condTree.root.children.size() > 0) {
                    mineTree(condTree, condHeaderFreq, minCount, newPattern, result);
                }
            }
        }
    }

    static class FPNode {
        Integer item;
        int count;
        FPNode parent;
        Map<Integer, FPNode> children = new HashMap<>();
        FPNode nodeLink;

        FPNode(Integer item, FPNode parent) {
            this.item = item;
            this.count = 0;
            this.parent = parent;
        }
    }

    static class FPTree {
        FPNode root = new FPNode(null, null);
        Map<Integer, FPNode> headerTableMap = new HashMap<>();
        int nodeCount = 1;

        void insertTransaction(List<Integer> trans) {
            insertTransactionWithCount(trans, 1);
        }

        void insertTransactionWithCount(List<Integer> trans, int count) {
            FPNode currentNode = root;
            for (Integer item : trans) {
                if (!currentNode.children.containsKey(item)) {
                    FPNode newNode = new FPNode(item, currentNode);
                    currentNode.children.put(item, newNode);
                    nodeCount++;

                    if (!headerTableMap.containsKey(item)) {
                        headerTableMap.put(item, newNode);
                    } else {
                        FPNode lastNode = headerTableMap.get(item);
                        while (lastNode.nodeLink != null) {
                            lastNode = lastNode.nodeLink;
                        }
                        lastNode.nodeLink = newNode;
                    }
                }
                currentNode = currentNode.children.get(item);
                currentNode.count += count;
            }
        }
    }

    private static List<List<Integer>> loadData(String path) throws IOException {
        List<List<Integer>> transactions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                List<Integer> transaction = new ArrayList<>();
                for (String s : line.trim().split("\\s+")) {
                    transaction.add(Integer.parseInt(s));
                }
                transactions.add(transaction);
            }
        }
        return transactions;
    }

    private static void saveResult(String outputPath, Map<Set<Integer>, Integer> result) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            for (Map.Entry<Set<Integer>, Integer> entry : result.entrySet()) {
                String items = entry.getKey().toString().replaceAll("[\\[\\],]", "");
                writer.println(items + " : " + entry.getValue());
            }
        }
    }

    private static void saveMetricsToCSV(String outputPath, String dataset, double minSup, int l1Size, double compRatio) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("Dataset,MinSupport,TotalTxns,FrequentItems(L1),TotalPatternsFound,GlobalNodes,ConditionalTreesBuilt,CompressionRatio,IO_Time(ms),Algo_Time(ms),PeakMemory(MB)");
            String memStr = String.format(Locale.US, "%.2f", peakMemory / (1024.0 * 1024.0));
            String compRatioStr = String.format(Locale.US, "%.2f", compRatio);

            writer.println(dataset + "," + minSup + "," + totalTransactions + "," + l1Size + "," +
                    totalPatternsFound + "," + globalNodeCount + "," + conditionalTreeCount + "," +
                    compRatioStr + "," + ioTime + "," + algoTime + "," + memStr);
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