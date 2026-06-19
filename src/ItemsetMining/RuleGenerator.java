package ItemsetMining;

import java.io.*;
import java.util.*;

/**
 * RULE GENERATOR - Association Rule Generation
 * 
 * Mô tả: Sinh các luật kết hợp (association rules) từ các itemset phổ biến
 * Dùng thuật toán Apriori-based rule generation để sinh luật từ tập phổ biến k-item
 * 
 * Luật kết hợp có dạng: A -> B
 *   - A = antecedent (tiền đề)
 *   - B = consequent (kết luận)
 * 
 * Các metric:
 *   - Support: P(A ∪ B) = số giao dịch chứa cả A và B
 *   - Confidence: P(B|A) = Support(A∪B) / Support(A)
 *   - Lift: Confidence / Support(B) = P(B|A) / P(B)
 *   - Conviction: (1 - Support(B)) / (1 - Confidence)
 * 
 * @author Data Mining Course
 * @version 1.0
 */
public class RuleGenerator {

    // ========== DỮ LIỆU INPUT ==========
    /** Map lưu các itemset phổ biến với support count */
    private Map<Set<Integer>, Double> frequentItemsets = new HashMap<>();

    // ========== THỐNG KÊ QUẢN LÝ QUẢN TRÌNH ==========
    /** Số luật được kiểm tra (checked) */
    private int rulesChecked = 0;
    
    /** Số luật được sinh ra (generated) */
    private int rulesGenerated = 0;
    
    /** Số luật bị tỉa (pruned) */
    private int rulesPruned = 0;
    
    /** Thời gian chạy thuật toán (ms) */
    private long algoTime = 0;

    // ========== KẾT QUẢ ==========
    /** Danh sách các luật hợp lệ với metrics */
    private List<RuleMetrics> validRules = new ArrayList<>();

    /**
     * Inner class lưu metrics của một luật
     */
    class RuleMetrics {
        Set<Integer> antecedent;   // A trong A -> B
        Set<Integer> consequent;   // B trong A -> B
        double support;            // Support(A ∪ B)
        double confidence;         // Confidence(A -> B)
        double lift;               // Lift(A -> B)
        double conviction;         // Conviction(A -> B)

        RuleMetrics(Set<Integer> a, Set<Integer> c, double sup, double conf, double lift, double conv) {
            this.antecedent = a; 
            this.consequent = c;
            this.support = sup; 
            this.confidence = conf;
            this.lift = lift; 
            this.conviction = conv;
        }
    }

    /**
     * Phương thức main - Điểm vào chương trình
     * 
     * Luồng:
     * 1. Đọc itemset phổ biến từ file Apriori đã sinh
     * 2. Sinh luật từ các itemset
     * 3. Xuất luật và metrics
     */
    public static void main(String[] args) {
        RuleGenerator generator = new RuleGenerator();
        double minConfidence = 0.8;

        // Input: file itemset phổ biến từ Apriori (minSupport = 0.5)
        String inputFile = "results/apriori_FP_chess_0.5.txt";

        // Output: file luật với minConfidence = 0.8
        String baseFileName = "results/apriori_AR_chess_" + minConfidence;

        try {
            generator.loadFrequentItemsets(inputFile);
            generator.generateRulesAndExport(baseFileName, minConfidence);
            System.out.println("Đã sinh luật xong! Vui lòng kiểm tra các file .txt và .csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Đọc các itemset phổ biến từ file
     * 
     * Format file đầu vào (từ Apriori):
     *   1 3 4 : 250
     *   2 3 5 : 180
     *   ...
     * 
     * @param filePath Đường dẫn file chứa itemset phổ biến
     * @throws IOException Nếu không thể đọc file
     */
    public void loadFrequentItemsets(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            // Bỏ qua header nếu có
            if (line != null && !line.contains(":")) line = br.readLine();

            while (line != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        // Parse items từ chuỗi "1 3 4"
                        String[] items = parts[0].trim().split("\\s+");
                        // Parse support từ chuỗi "250"
                        double support = Double.parseDouble(parts[1].trim());

                        Set<Integer> itemSet = new HashSet<>();
                        for (String s : items) 
                            itemSet.add(Integer.parseInt(s));

                        frequentItemsets.put(itemSet, support);
                    }
                }
                line = br.readLine();
            }
        }
    }

    /**
     * Sinh các luật kết hợp từ itemset phổ biến
     * 
     * Thuật toán Apriori-based:
     * 1. Với mỗi itemset F_k (k >= 2)
     * 2. Sinh các 1-item consequent (H_1)
     * 3. Sinh các m-item consequent (H_{m+1}) từ H_m theo đệ quy
     * 4. Kiểm tra confidence >= minConfidence
     * 
     * @param baseFileName Đường dẫn base file xuất (không bao gồm extension)
     * @param minConfidence Ngưỡng confidence tối thiểu
     * @throws IOException Nếu không thể ghi file
     */
    public void generateRulesAndExport(String baseFileName, double minConfidence) throws IOException {
        rulesChecked = 0; 
        rulesGenerated = 0; 
        rulesPruned = 0;
        validRules.clear();

        long startTime = System.currentTimeMillis();

        // ===== SINH LUẬT TỪ CÁC ITEMSET =====
        for (Set<Integer> fk : frequentItemsets.keySet()) {
            // Chỉ sinh luật từ itemset có >= 2 item (itemset 1-item không có luật)
            if (fk.size() < 2) continue;

            // BƯỚC 1: Sinh H_1 (các 1-item consequent)
            List<Set<Integer>> H1 = new ArrayList<>();

            for (Integer item : fk) {
                // Tạo luật: (fk \ {item}) -> {item}
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
            
            // BƯỚC 2: Sinh H_{m+1} từ H_m bằng đệ quy
            aprioriGenRules(fk, H1, minConfidence);
        }

        algoTime = System.currentTimeMillis() - startTime;

        // XUẤT KẾT QUẢ
        exportToTXT(baseFileName + ".txt", minConfidence);
        exportRulesToCSV(baseFileName + "_details.csv");
        exportStatsToCSV(baseFileName + "_stats.csv", minConfidence);
    }

    /**
     * Sinh các m-item consequent từ (m-1)-item consequent bằng đệ quy
     * 
     * Quá trình:
     * 1. Nối (join) các consequent trong H_m ��ể tạo H_{m+1}
     * 2. Kiểm tra confidence của luật tương ứng
     * 3. Nếu confidence < minConfidence -> tỉa (không gọi đệ quy)
     * 4. Nếu confidence >= minConfidence -> gọi đệ quy với H_{m+1}
     * 
     * @param fk Itemset gốc F_k
     * @param Hm Danh sách m-item consequent
     * @param minConfidence Ngưỡng confidence
     */
    private void aprioriGenRules(Set<Integer> fk, List<Set<Integer>> Hm, double minConfidence) {
        int k = fk.size();
        int m = Hm.isEmpty() ? 0 : Hm.get(0).size();

        // Điều kiện dừng: k > m + 1 (nếu k <= m + 1, không cần sinh H_{m+1})
        if (k > m + 1 && !Hm.isEmpty()) {
            // Sinh H_{m+1} bằng cách nối các phần tử trong H_m
            List<Set<Integer>> HmPlus1 = generateAprioriConsequents(Hm);

            // Kiểm tra confidence của các luật mới
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
                    // Tỉa: loại bỏ consequent này khỏi H_{m+1}
                    // Tất cả superset của nó cũng sẽ bị loại
                    it.remove();
                    rulesPruned++;
                }
            }
            
            // Gọi đệ quy với H_{m+1}
            aprioriGenRules(fk, HmPlus1, minConfidence);
        }
    }

    /**
     * Sinh m+1 item consequent từ m-item consequent bằng phép nối (join)
     * 
     * Thuật toán:
     * 1. Lặp qua tất cả cặp phần tử trong H_m
     * 2. Hợp (union) hai phần tử để tạo (m+1)-item consequent
     * 3. Chỉ giữ lại nếu union.size() = m + 1
     * 
     * @param Hm Danh sách m-item consequent
     * @return Danh sách (m+1)-item consequent
     */
    private List<Set<Integer>> generateAprioriConsequents(List<Set<Integer>> Hm) {
        List<Set<Integer>> candidates = new ArrayList<>();
        for (int i = 0; i < Hm.size(); i++) {
            for (int j = i + 1; j < Hm.size(); j++) {
                Set<Integer> c1 = Hm.get(i);
                Set<Integer> c2 = Hm.get(j);
                Set<Integer> union = new HashSet<>(c1);
                union.addAll(c2);

                // Chỉ giữ lại nếu kích thước = m + 1
                if (union.size() == c1.size() + 1 && !candidates.contains(union)) {
                    candidates.add(union);
                }
            }
        }
        return candidates;
    }

    /**
     * Ghi nhận (record) một luật hợp lệ
     * 
     * Tính toán các metrics:
     * - Lift = Confidence / Support(B)
     * - Conviction = (1 - Support(B)) / (1 - Confidence)
     * 
     * @param antecedent A trong A -> B
     * @param consequent B trong A -> B
     * @param sup Support(A ∪ B)
     * @param conf Confidence(A -> B)
     */
    private void recordRule(Set<Integer> antecedent, Set<Integer> consequent, double sup, double conf) {
        double supB = frequentItemsets.getOrDefault(consequent, 0.0);

        // Tính Lift
        double lift = (supB > 0) ? conf / supB : 0.0;

        // Tính Conviction
        double conviction;
        if (conf == 1.0) {
            conviction = Double.POSITIVE_INFINITY;
        } else {
            conviction = (1.0 - supB) / (1.0 - conf);
        }

        validRules.add(new RuleMetrics(antecedent, consequent, sup, conf, lift, conviction));
        rulesGenerated++;
    }

    /**
     * Xuất các luật vào file text (định dạng người dùng)
     * 
     * Format:
     *   === DANH SÁCH LUẬT KẾT HỢP (MinConf = 0.8) ===
     *   1 3 -> 4 : sup = 0.50, conf = 0.95, lift = 1.20
     *   2 3 -> 5 : sup = 0.45, conf = 0.88, lift = 1.15
     *   
     *   === BÁO CÁO HIỆU NĂNG ===
     *   Tổng số luật sinh ra: 150
     *   ...
     * 
     * @param path Đường dẫn file xuất
     * @param minConf Ngưỡng confidence sử dụng
     * @throws IOException Nếu không thể ghi file
     */
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

    /**
     * Xuất chi tiết các luật vào file CSV
     * 
     * Columns: Antecedent, Consequent, Support, Confidence, Lift, Conviction
     * 
     * @param path Đường dẫn file CSV
     * @throws IOException Nếu không thể ghi file
     */
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

    /**
     * Xuất thống kê tổng hợp vào file CSV
     * 
     * @param path Đường dẫn file CSV
     * @param minConf Ngưỡng confidence
     * @throws IOException Nếu không thể ghi file
     */
    private void exportStatsToCSV(String path, double minConf) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("MinConfidence,RulesGenerated,RulesChecked,BranchesPruned,ExecutionTime_ms");
            writer.println(minConf + "," + rulesGenerated + "," + rulesChecked + "," + rulesPruned + "," + algoTime);
        }
    }

    /**
     * Định dạng một itemset thành chuỗi (sắp xếp và loại bỏ dấu ngoặc)
     * 
     * Ví dụ: {1, 3, 4} -> "1 3 4"
     * 
     * @param set Set cần định dạng
     * @return Chuỗi items phân tách bằng khoảng trắng
     */
    private String formatRule(Set<Integer> set) {
        List<Integer> sorted = new ArrayList<>(set);
        Collections.sort(sorted);
        return sorted.toString().replaceAll("[\\[\\],]", "");
    }
}
