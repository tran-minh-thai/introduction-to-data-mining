package ItemsetMining;

import java.io.*;
import java.util.*;

/**
 * FP-GROWTH ALGORITHM - Frequent Itemset Mining
 * 
 * Mô tả: FP-Growth (Frequent Pattern Growth) là một thuật toán khai phá tập phổ biến không sinh ứng viên.
 * Thay vì sinh ứng viên như Apriori, FP-Growth:
 *   1. Xây dựng FP-Tree (Frequent Pattern Tree) - một cấu trúc dữ liệu nén dữ liệu giao dịch
 *   2. Khai phá trực tiếp từ cây mà không cần sinh ứng viên
 * 
 * Ưu điểm:
 *   - Chỉ quét file 2 lần (1 lần đếm, 1 lần xây cây)
 *   - Không sinh ứng viên -> tiết kiệm memory và thời gian
 *   - Tốc độ nhanh hơn Apriori với dataset lớn
 * 
 * Độ phức tạp: O(n*m) - n: số item, m: số giao dịch
 * 
 * @author Data Mining Course
 * @version 1.0
 */
public class FPGrowth {

    // ========== BIẾN THEO DÕI HIỆU NĂNG ==========
    /** Bộ nhớ đỉnh sử dụng (bytes) */
    private static long peakMemory = 0;
    
    /** Thời gian I/O đọc file (milliseconds) */
    private static long ioTime = 0;
    
    /** Thời gian thực thi thuật toán (milliseconds) */
    private static long algoTime = 0;
    
    /** Tổng số giao dịch */
    private static int totalTransactions = 0;
    
    /** Số node toàn cục trong FP-Tree */
    private static int globalNodeCount = 0;
    
    /** Số conditional tree được tạo trong quá trình khai phá */
    private static int conditionalTreeCount = 0;
    
    /** Tổng số pattern tìm được */
    private static int totalPatternsFound = 0;

    /**
     * Phương thức main - Điểm vào chương trình
     * Cấu hình: Chess dataset với minSupport = 0.5 (50%)
     */
    public static void main(String[] args) {
        String filePath = "datasets/chess.txt";
        double minSupport = 0.5;
        String baseOutputPath = "results/fpgrowth_FP_chess_" + minSupport;

        try {
            executeAndExport(filePath, baseOutputPath, minSupport);
            System.out.println("Đã chạy xong FP-Growth! Vui lòng kiểm tra các file .txt và .csv trong thư mục results");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Thực thi thuật toán FP-Growth đầy đủ
     * 
     * Các bước chính:
     * 1. Đọc dữ liệu
     * 2. Tìm L1 (các item phổ biến 1-item)
     * 3. Sắp xếp lại giao dịch theo tần suất item giảm dần
     * 4. Xây dựng FP-Tree toàn cục
     * 5. Khai phá FP-Tree bằng đệ quy
     * 6. Xuất kết quả
     * 
     * @param filePath Đường dẫn file dữ liệu SPMF
     * @param baseOutputPath Đường dẫn file xuất
     * @param minSupport Ngưỡng support tối thiểu
     * @throws IOException Nếu không thể đọc/ghi file
     */
    public static void executeAndExport(String filePath, String baseOutputPath, double minSupport) throws IOException {
        peakMemory = 0; 
        updatePeakMemory();
        globalNodeCount = 0; 
        conditionalTreeCount = 0; 
        totalPatternsFound = 0;

        String datasetName = new File(filePath).getName().replaceFirst("[.][^.]+$", "");

        // PHASE 1: ĐỌC DỮ LIỆU
        long startIO = System.currentTimeMillis();
        List<List<Integer>> transactions = loadData(filePath);
        totalTransactions = transactions.size();
        ioTime = System.currentTimeMillis() - startIO;

        int minCount = (int) Math.ceil(minSupport * totalTransactions);

        // PHASE 2: THUẬT TOÁN
        long startAlgo = System.currentTimeMillis();

        // BƯỚC 1: Đếm tần suất 1-item
        Map<Integer, Integer> frequencyMap = new HashMap<>();
        for (List<Integer> trans : transactions) {
            for (Integer item : trans) {
                frequencyMap.put(item, frequencyMap.getOrDefault(item, 0) + 1);
            }
        }

        // BƯỚC 2: Tạo L1 (các item phổ biến) và sắp xếp theo tần suất giảm dần
        List<Integer> L1 = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() >= minCount) {
                L1.add(entry.getKey());
            }
        }
        // Sắp xếp: item có tần suất cao nhất đứng trước
        L1.sort((a, b) -> {
            int cmp = frequencyMap.get(b).compareTo(frequencyMap.get(a));
            if (cmp == 0) return a.compareTo(b);
            return cmp;
        });

        // BƯỚC 3: Tạo header table với tần suất
        Map<Integer, Integer> headerTableFreq = new HashMap<>();
        for (Integer item : L1) 
            headerTableFreq.put(item, frequencyMap.get(item));

        // BƯỚC 4: Lọc giao dịch - chỉ giữ lại item phổ biến và sắp xếp
        List<List<Integer>> filteredTransactions = new ArrayList<>();
        int totalItemsInFilteredDB = 0;
        for (List<Integer> trans : transactions) {
            List<Integer> filteredTrans = new ArrayList<>();
            for (Integer item : trans) {
                if (headerTableFreq.containsKey(item)) 
                    filteredTrans.add(item);
            }
            if (!filteredTrans.isEmpty()) {
                // Sắp xếp theo thứ tự L1 (giảm dần tần suất)
                filteredTrans.sort((a, b) -> {
                    int cmp = headerTableFreq.get(b).compareTo(headerTableFreq.get(a));
                    if (cmp == 0) return a.compareTo(b);
                    return cmp;
                });
                filteredTransactions.add(filteredTrans);
                totalItemsInFilteredDB += filteredTrans.size();
            }
        }

        // BƯỚC 5: Xây dựng FP-Tree toàn cục
        FPTree globalTree = new FPTree();
        for (List<Integer> trans : filteredTransactions) {
            globalTree.insertTransaction(trans);
        }
        globalNodeCount = globalTree.nodeCount;

        // BƯỚC 6: Khai phá FP-Tree bằng đệ quy
        Map<Set<Integer>, Integer> frequentPatterns = new LinkedHashMap<>();
        mineTree(globalTree, headerTableFreq, minCount, new HashSet<>(), frequentPatterns);

        totalPatternsFound = frequentPatterns.size();
        algoTime = System.currentTimeMillis() - startAlgo;

        // PHASE 3: XUẤT KẾT QUẢ
        saveResult(baseOutputPath + ".txt", frequentPatterns);
        double compressionRatio = globalNodeCount == 0 ? 0 : (double) totalItemsInFilteredDB / globalNodeCount;
        saveMetricsToCSV(baseOutputPath + ".csv", datasetName, minSupport, L1.size(), compressionRatio);
    }

    /**
     * Khai phá FP-Tree bằng cách đệ quy
     * 
     * Thuật toán:
     * 1. Với mỗi item trong header table
     * 2. Xây dựng Conditional Pattern Base (CPB)
     * 3. Xây dựng Conditional FP-Tree từ CPB
     * 4. Gọi đệ quy với tree mới
     * 
     * @param tree FP-Tree hiện tại
     * @param headerTable Header table của tree
     * @param minCount Ngưỡng support tối thiểu
     * @param prefix Prefix hiện tại (các item đã xử lý)
     * @param result Map lưu kết quả tất cả pattern tìm được
     */
    private static void mineTree(FPTree tree, Map<Integer, Integer> headerTable, int minCount,
                                 Set<Integer> prefix, Map<Set<Integer>, Integer> result) {
        updatePeakMemory();

        // Sắp xếp items trong header table theo tần suất tăng dần
        // (xử lý item hiếm trước để tìm pattern nhỏ hơn trước)
        List<Integer> items = new ArrayList<>(headerTable.keySet());
        items.sort((a, b) -> {
            int cmp = headerTable.get(a).compareTo(headerTable.get(b));
            if (cmp == 0) return b.compareTo(a);
            return cmp;
        });

        // Xử lý từng item trong header table
        for (Integer item : items) {
            // Tạo pattern bằng cách thêm item vào prefix
            Set<Integer> newPattern = new HashSet<>(prefix);
            newPattern.add(item);
            result.put(newPattern, headerTable.get(item));

            // ===== XÂY DỰNG CONDITIONAL PATTERN BASE (CPB) =====
            List<List<Integer>> cpb = new ArrayList<>();
            List<Integer> cpbCounts = new ArrayList<>();
            
            // Lặp qua tất cả các node của item trong FP-Tree (qua node link)
            FPNode currentNode = tree.headerTableMap.get(item);
            while (currentNode != null) {
                // Tìm đường dẫn từ node này đến root
                List<Integer> path = new ArrayList<>();
                FPNode parent = currentNode.parent;
                while (parent != null && parent.item != null) {
                    path.add(parent.item);
                    parent = parent.parent;
                }
                
                if (!path.isEmpty()) {
                    // Đảo ngược path để theo thứ tự từ root đến node
                    Collections.reverse(path);
                    cpb.add(path);
                    cpbCounts.add(currentNode.count);
                }
                currentNode = currentNode.nodeLink;
            }

            // ===== XÂY DỰNG CONDITIONAL FP-TREE =====
            // Tính tần suất của các item trong CPB
            Map<Integer, Integer> condHeaderFreq = new HashMap<>();
            for (int i = 0; i < cpb.size(); i++) {
                List<Integer> path = cpb.get(i);
                int count = cpbCounts.get(i);
                for (Integer pItem : path) {
                    condHeaderFreq.put(pItem, condHeaderFreq.getOrDefault(pItem, 0) + count);
                }
            }

            // Lọc: chỉ giữ lại item có support >= minCount
            condHeaderFreq.entrySet().removeIf(entry -> entry.getValue() < minCount);

            // Nếu có item phổ biến trong CPB -> xây dựng conditional tree
            if (!condHeaderFreq.isEmpty()) {
                FPTree condTree = new FPTree();
                conditionalTreeCount++;

                // Chèn các path từ CPB vào conditional tree
                for (int i = 0; i < cpb.size(); i++) {
                    List<Integer> path = cpb.get(i);
                    int count = cpbCounts.get(i);
                    
                    // Lọc path: chỉ giữ item phổ biến
                    List<Integer> filteredPath = new ArrayList<>();
                    for (Integer pItem : path) {
                        if (condHeaderFreq.containsKey(pItem)) 
                            filteredPath.add(pItem);
                    }
                    
                    if (!filteredPath.isEmpty()) {
                        // Sắp xếp theo thứ tự conditional header table
                        filteredPath.sort((a, b) -> {
                            int cmp = condHeaderFreq.get(b).compareTo(condHeaderFreq.get(a));
                            if (cmp == 0) return a.compareTo(b);
                            return cmp;
                        });
                        condTree.insertTransactionWithCount(filteredPath, count);
                    }
                }

                // Gọi đệ quy nếu tree có nhánh
                if (condTree.root.children.size() > 0) {
                    mineTree(condTree, condHeaderFreq, minCount, newPattern, result);
                }
            }
        }
    }

    /**
     * FPNode - Node trong FP-Tree
     * 
     * Cấu trúc:
     *   - item: giá trị item
     *   - count: số giao dịch chứa item tính từ root đến node này
     *   - parent: node cha
     *   - children: các node con
     *   - nodeLink: liên kết đến node item tiếp theo (cho header table)
     */
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

    /**
     * FPTree - Frequent Pattern Tree
     * 
     * Cấu trúc dữ liệu nén giao dịch:
     *   - root: node gốc (không chứa item)
     *   - headerTableMap: ánh xạ item -> node đầu tiên của item (node link start)
     *   - nodeCount: số node toàn cây (không tính root)
     */
    static class FPTree {
        FPNode root = new FPNode(null, null);
        Map<Integer, FPNode> headerTableMap = new HashMap<>();
        int nodeCount = 1; // Tính root

        /**
         * Chèn một giao dịch vào FP-Tree với support = 1
         */
        void insertTransaction(List<Integer> trans) {
            insertTransactionWithCount(trans, 1);
        }

        /**
         * Chèn một giao dịch vào FP-Tree với support bất kỳ
         * 
         * Thuật toán:
         * 1. Bắt đầu từ root
         * 2. Với mỗi item trong giao dịch:
         *    - Nếu node con chứa item đã tồn tại -> di chuyển xuống
         *    - Nếu chưa tồn tại -> tạo node mới
         *    - Tăng count của node hiện tại
         * 3. Cập nhật node link cho header table
         * 
         * @param trans Giao dịch (danh sách item)
         * @param count Support count của giao dịch
         */
        void insertTransactionWithCount(List<Integer> trans, int count) {
            FPNode currentNode = root;
            for (Integer item : trans) {
                if (!currentNode.children.containsKey(item)) {
                    // Tạo node mới
                    FPNode newNode = new FPNode(item, currentNode);
                    currentNode.children.put(item, newNode);
                    nodeCount++;

                    // Cập nhật header table
                    if (!headerTableMap.containsKey(item)) {
                        headerTableMap.put(item, newNode);
                    } else {
                        // Chèn vào cuối danh sách node link
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

    /**
     * Đọc dữ liệu từ file SPMF format
     * Trả về List<List<Integer>> (mỗi giao dịch là List, không phải Set như Apriori)
     * Sở dĩ dùng List vì thứ tự item quan trọng trong FP-Tree
     * 
     * @param path Đường dẫn file dữ liệu
     * @return List các giao dịch (mỗi giao dịch là List<Integer>)
     * @throws IOException Nếu không thể đọc file
     */
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

    /**
     * Lưu kết quả pattern phổ biến vào file text
     * 
     * Format:
     *   1 3 4 : 250
     *   2 3 5 : 180
     * 
     * @param outputPath Đường dẫn file xuất
     * @param result Map các pattern phổ biến với support count
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
     * Lưu metrics hiệu năng vào file CSV
     * 
     * Columns:
     *   - CompressionRatio: Tỷ lệ nén = totalItems / nodeCount
     *     (càng cao = tree nén tốt hơn)
     * 
     * @param outputPath Đường dẫn file CSV
     * @param dataset Tên dataset
     * @param minSup Ngưỡng support
     * @param l1Size Kích thước L1 (số item phổ biến 1-item)
     * @param compRatio Tỷ lệ nén của FP-Tree
     * @throws IOException Nếu không thể ghi file
     */
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

    /**
     * Cập nhật bộ nhớ đỉnh sử dụng
     */
    private static void updatePeakMemory() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        if (usedMemory > peakMemory) {
            peakMemory = usedMemory;
        }
    }
}
