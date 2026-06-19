package ItemsetMining;

import java.io.*;
import java.util.*;

/**
 * APRIORI ALGORITHM - Frequent Itemset Mining
 * 
 * Mô tả: Apriori là một thuật toán khai phá tập phổ biến cấp độ theo chiều rộng (BFS).
 * Thực hiện tìm kiếm từ trên xuống (top-down) bằng cách:
 *   1. Sinh ứng viên (join & prune)
 *   2. Kiểm tra support của các ứng viên
 *   3. Giữ lại các itemset có support >= minSupport
 *
 * Nguyên lý chính:
 *   - Nếu một itemset là phổ biến, tất cả các tập con của nó cũng phổ biến
 *   - Dùng để loại bỏ (prune) các ứng viên không cần thiết
 * 
 * Độ phức tạp: O(2^n) trong trường hợp xấu nhất (n = số item)
 * 
 * @author Data Mining Course
 * @version 1.0
 */
public class Apriori {

    // ========== BIẾN THEO DÕI HIỆU NĂNG ==========
    /** Bộ nhớ đỉnh sử dụng (bytes) - cập nhật liên tục trong quá trình thực thi */
    private static long peakMemory = 0;
    
    /** Thời gian I/O đọc file dữ liệu (milliseconds) */
    private static long ioTime = 0;
    
    /** Thời gian thực thi thuật toán (milliseconds) */
    private static long algoTime = 0;
    
    /** Tổng số giao dịch trong dataset */
    private static int totalTransactions = 0;

    /**
     * Phương thức main - Điểm vào chương trình
     * Cấu hình: Chess dataset với minSupport = 0.5 (50%)
     */
    public static void main(String[] args) {
        String filePath = "datasets/chess.txt";
        double minSupport = 0.5;
        // Tên file xuất ra chứa minSupport để phân biệt các lần chạy khác nhau
        String baseOutputPath = "results/apriori_FP_chess_" + minSupport;

        try {
            executeAndExport(filePath, baseOutputPath, minSupport);
            System.out.println("Đã chạy xong ItemsetMining.Apriori! Vui lòng kiểm tra các file kết quả (.txt và .csv) trong thư mục results.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Thực thi thuật toán Apriori đầy đủ
     * 
     * Quy trình:
     * 1. Đọc dữ liệu từ file
     * 2. Tính toán minCount = ceil(minSupport * totalTransactions)
     * 3. Sinh các itemset 1-item
     * 4. Vòng lặp: sinh ứng viên -> kiểm tra support -> lọc
     * 5. Xuất kết quả và metrics
     * 
     * @param filePath Đường dẫn file dữ liệu SPMF format
     * @param baseOutputPath Đường dẫn file xuất (không bao gồm extension)
     * @param minSupport Ngưỡng support tối thiểu (0.0 - 1.0)
     * @throws IOException Nếu không thể đọc/ghi file
     */
    public static void executeAndExport(String filePath, String baseOutputPath, double minSupport) throws IOException {
        peakMemory = 0;
        updatePeakMemory();
        List<String[]> csvRows = new ArrayList<>();

        // Trích tên dataset từ đường dẫn (ví dụ: "chess.txt" -> "chess")
        String datasetName = new File(filePath).getName().replaceFirst("[.][^.]+$", "");

        // PHASE 1: ĐỌC DỮ LIỆU (I/O)
        long startIO = System.currentTimeMillis();
        List<Set<Integer>> transactions = loadData(filePath);
        ioTime = System.currentTimeMillis() - startIO;
        totalTransactions = transactions.size();

        // Tính ngưỡng số lượng tối thiểu (support count)
        int minCount = (int) Math.ceil(minSupport * totalTransactions);

        // PHASE 2: THUẬT TOÁN
        long startAlgo = System.currentTimeMillis();
        Map<Set<Integer>, Integer> finalFrequentItemsets = new LinkedHashMap<>();

        // ===== ITERATION 1: TÌM CÁC ITEMSET 1-ITEM =====
        // Lặp qua tất cả giao dịch, đếm tần suất của từng item
        Map<Set<Integer>, Integer> candidates = new HashMap<>();
        for (Set<Integer> trans : transactions) {
            for (Integer item : trans) {
                Set<Integer> s = new HashSet<>(Collections.singletonList(item));
                candidates.put(s, candidates.getOrDefault(s, 0) + 1);
            }
        }
        // Lọc: chỉ giữ lại các item có support >= minCount
        Map<Set<Integer>, Integer> currentFrequent = filter(candidates, minCount);
        finalFrequentItemsets.putAll(currentFrequent);

        updatePeakMemory();

        // Ghi thống kê cho lần lặp k=1
        csvRows.add(new String[]{
                datasetName, String.valueOf(minSupport), String.valueOf(totalTransactions),
                "1", String.valueOf(candidates.size()), String.valueOf(currentFrequent.size()), "0", "0"
        });

        // ===== ITERATION k>=2: TÌM ITEMSET K-ITEM =====
        int k = 2;
        while (!currentFrequent.isEmpty()) {
            long[] pruneStats = new long[2]; // [số bị tỉa, số lần kiểm tra subset]

            // Bước 1: Sinh ứng viên từ các itemset (k-1)-item
            Map<Set<Integer>, Integer> nextCandidates = generate(currentFrequent.keySet(), k, pruneStats);

            // Bước 2: Kiểm tra support của các ứng viên
            // Lặp qua tất cả giao dịch, nếu giao dịch chứa ứng viên -> tăng đếm
            for (Set<Integer> trans : transactions) {
                for (Set<Integer> cand : nextCandidates.keySet()) {
                    if (trans.containsAll(cand)) {
                        nextCandidates.put(cand, nextCandidates.get(cand) + 1);
                    }
                }
            }

            // Bước 3: Lọc các ứng viên có support >= minCount
            currentFrequent = filter(nextCandidates, minCount);
            if (currentFrequent.isEmpty()) break;

            finalFrequentItemsets.putAll(currentFrequent);
            updatePeakMemory();

            // Ghi thống kê cho lần lặp k hiện tại
            csvRows.add(new String[]{
                    datasetName, String.valueOf(minSupport), String.valueOf(totalTransactions),
                    String.valueOf(k), String.valueOf(nextCandidates.size() + pruneStats[0]),
                    String.valueOf(currentFrequent.size()), String.valueOf(pruneStats[0]), String.valueOf(pruneStats[1])
            });

            k++;
        }

        algoTime = System.currentTimeMillis() - startAlgo;

        // PHASE 3: XUẤT KẾT QUẢ
        saveResult(baseOutputPath + ".txt", finalFrequentItemsets);
        saveMetricsToCSV(baseOutputPath + ".csv", csvRows);
    }

    /**
     * Sinh các ứng viên từ itemset (k-1)-item để tạo itemset k-item
     * 
     * Thuật toán:
     * 1. Nối (join): Ghép các itemset (k-1)-item lại
     * 2. Tỉa (prune): Loại bỏ các itemset có tập con không phổ biến
     * 
     * @param prevFrequent Tập hợp các itemset (k-1)-item phổ biến
     * @param k Độ dài của itemset cần sinh (k-item)
     * @param pruneStats Mảng ghi thống kê [số bị tỉa, số lần kiểm tra]
     * @return Map các ứng viên k-item (support = 0 ban đầu, sẽ được cập nhật)
     */
    private static Map<Set<Integer>, Integer> generate(Set<Set<Integer>> prevFrequent, int k, long[] pruneStats) {
        Map<Set<Integer>, Integer> candidates = new HashMap<>();
        List<Set<Integer>> list = new ArrayList<>(prevFrequent);

        // Vòng lặp kép: nối từng cặp itemset (k-1)-item
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                Set<Integer> union = new HashSet<>(list.get(i));
                union.addAll(list.get(j));

                // Chỉ giữ lại nếu kích thước = k (ghép 2 itemset cỡ k-1 được cỡ k)
                if (union.size() == k && !candidates.containsKey(union)) {
                    boolean isPruned = false;
                    
                    // Kiểm tra: tất cả (k-1)-subset của union có phổ biến không?
                    for (Integer item : union) {
                        pruneStats[1]++; // Tăng số lần kiểm tra

                        Set<Integer> subset = new HashSet<>(union);
                        subset.remove(item); // Tạo (k-1)-subset bằng cách bỏ 1 item

                        // Nếu subset không phổ biến -> tỉa union này
                        if (!prevFrequent.contains(subset)) {
                            isPruned = true;
                            break;
                        }
                    }

                    if (isPruned) {
                        pruneStats[0]++; // Tăng số lần tỉa
                    } else {
                        candidates.put(union, 0); // Khởi tạo support = 0
                    }
                }
            }
        }
        return candidates;
    }

    /**
     * Đọc dữ liệu từ file SPMF format
     * 
     * Format: Mỗi dòng là một giao dịch, các item cách nhau bằng khoảng trắng
     * Ví dụ:
     *   1 3 4
     *   2 3 5
     *   1 2 3 5
     * 
     * @param path Đường dẫn file dữ liệu
     * @return List các giao dịch, mỗi giao dịch là một Set<Integer> của các item
     * @throws IOException Nếu không thể đọc file
     */
    private static List<Set<Integer>> loadData(String path) throws IOException {
        List<Set<Integer>> transactions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // Bỏ qua dòng trống
                Set<Integer> transaction = new HashSet<>();
                for (String s : line.trim().split("\\s+")) {
                    transaction.add(Integer.parseInt(s));
                }
                transactions.add(transaction);
            }
        }
        return transactions;
    }

    /**
     * Lọc các ứng viên: chỉ giữ lại những itemset có support >= minCount
     * 
     * @param candidates Map các ứng viên với support count
     * @param minCount Ngưỡng support tối thiểu (số giao dịch)
     * @return Map các itemset phổ biến (support >= minCount)
     */
    private static Map<Set<Integer>, Integer> filter(Map<Set<Integer>, Integer> candidates, int minCount) {
        Map<Set<Integer>, Integer> filtered = new HashMap<>();
        candidates.forEach((key, v) -> { 
            if (v >= minCount) filtered.put(key, v); 
        });
        return filtered;
    }

    /**
     * Lưu kết quả (các itemset phổ biến) vào file text
     * 
     * Format xuất ra:
     *   1 3 4 : 250
     *   2 3 5 : 180
     *   ...
     * 
     * @param outputPath Đường dẫn file xuất
     * @param result Map các itemset phổ biến với support count
     * @throws IOException Nếu không thể ghi file
     */
    private static void saveResult(String outputPath, Map<Set<Integer>, Integer> result) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            for (Map.Entry<Set<Integer>, Integer> entry : result.entrySet()) {
                String items = entry.getKey().toString().replaceAll("[\\[\\],]", "");
                writer.println(items + " : " + entry.getValue());
            }
        }
    }

    /**
     * Lưu các metrics hiệu năng vào file CSV
     * 
     * Columns:
     *   - Dataset: Tên dataset
     *   - MinSupport: Ngưỡng support
     *   - TotalTxns: Số giao dịch
     *   - k: Độ dài itemset (1, 2, 3, ...)
     *   - CandidateSize(C_k): Số ứng viên k-item
     *   - FrequentSize(L_k): Số itemset k-item phổ biến
     *   - PrunedCandidates: Số ứng viên bị tỉa
     *   - SubsetChecks: Số lần kiểm tra subset
     *   - IO_Time(ms): Thời gian đọc file
     *   - Algo_Time(ms): Thời gian thuật toán
     *   - PeakMemory(MB): Bộ nhớ đỉnh sử dụng
     * 
     * @param outputPath Đường dẫn file CSV
     * @param loopStats List mảng String chứa thống kê từng lần lặp
     * @throws IOException Nếu không thể ghi file
     */
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

    /**
     * Cập nhật bộ nhớ đỉnh sử dụng
     * Gọi liên tục để theo dõi memory usage cao nhất
     */
    private static void updatePeakMemory() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        if (usedMemory > peakMemory) {
            peakMemory = usedMemory;
        }
    }
}
