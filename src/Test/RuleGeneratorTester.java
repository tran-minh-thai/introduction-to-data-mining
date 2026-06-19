package Test;

import ItemsetMining.RuleGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * RULE GENERATOR TESTER - Kiểm thử sinh luật kết hợp
 * 
 * Quy trình:
 * 1. Đọc itemset phổ biến từ file Apriori đã sinh (minSupport = X)
 * 2. Sinh luật từ các itemset (minConfidence = Y)
 * 3. Xuất luật và metrics
 * 
 * Ghi chú:
 * - Luật được sinh từ itemset phổ biến kích thước >= 2
 * - Cần chạy Apriori/FPGrowth TRƯỚC khi chạy RuleGeneratorTester
 * 
 * @author Data Mining Course
 * @version 1.0
 */
public class RuleGeneratorTester {
    /**
     * Phương thức main
     * 
     * Cấu hình kiểm thử:
     * - Chess: Apriori chạy với minSupport=0.9 -> sinh luật với minConfidence=0.6
     * - Retail: Apriori chạy với minSupport=0.007 -> sinh luật với minConfidence=0.5
     * 
     * Lưu ý:
     * - RuleConfig(fileName, minSup, minConf)
     * - minSup: support đã dùng khi chạy Apriori (để tìm file input)
     * - minConf: confidence để sinh luật (ngưỡng mới)
     */
    public static void main(String[] args) {
        // ===== ĐỊNH NGHĨA CÁC TEST CASE =====
        List<RuleConfig> testCases = new ArrayList<>();
        
        // Chess: itemset từ Apriori (minSup=0.9) -> sinh luật (minConf=0.6)
        // Chọn minConf < minSup là bình thường vì:
        // - Confidence = Support(A∪B) / Support(A)
        // - Support luôn <= confidence
        testCases.add(new RuleConfig("chess.txt", 0.9, 0.6));
        
        // Retail: itemset từ Apriori (minSup=0.007) -> sinh luật (minConf=0.5)
        testCases.add(new RuleConfig("retail.txt", 0.007, 0.5));

        System.out.println("==================================================");
        System.out.println(" BẮT ĐẦU CHẠY KIỂM THỬ RULE GENERATOR");
        System.out.println("==================================================\n");

        // Tạo thư mục results nếu chưa có
        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdir();
        }

        // ===== THỰC THI TỪNG TEST CASE =====
        for (RuleConfig testCase : testCases) {
            String baseName = testCase.fileName.replaceFirst("[.][^.]+$", "");

            // Đường dẫn INPUT: file itemset từ Apriori
            // (phải khớp với đầu ra của AprioriTester/FPGrowthTester)
            String fpInputPath = "results/apriori_FP_" + baseName + "_" + testCase.minSup + ".txt";

            // Đường dẫn OUTPUT: file luật sẽ được sinh ra
            String baseOutputPath = "results/apriori_AR_" + baseName + "_" + testCase.minConf;

            System.out.println("-> Đang xử lý: [" + testCase.fileName + "] với ngưỡng minConfidence = " + testCase.minConf);
            long startTime = System.currentTimeMillis();

            try {
                // Khởi tạo RuleGenerator
                RuleGenerator rg = new RuleGenerator();
                
                // Bước 1: Đọc itemset phổ biến từ file Apriori
                rg.loadFrequentItemsets(fpInputPath);
                
                // Bước 2: Sinh luật và xuất kết quả
                rg.generateRulesAndExport(baseOutputPath, testCase.minConf);

                long duration = System.currentTimeMillis() - startTime;
                System.out.println("   + Thời gian sinh luật: " + duration + " ms");
                System.out.println("   + Kết quả đã lưu tại: " + baseOutputPath + ".txt (và các file .csv đính kèm)");
                System.out.println("--------------------------------------------------");

            } catch (IOException e) {
                System.err.println("   [LỖI] Không thể xử lý tập dữ liệu " + testCase.fileName + ": " + e.getMessage());
                System.err.println("   -> Gợi ý: Kiểm tra xem file " + fpInputPath + " đã được sinh ra từ Test.AprioriTester chưa.");
                System.out.println("--------------------------------------------------");
            }
        }

        System.out.println("Hoàn thành toàn bộ quá trình sinh luật!");
    }

    /**
     * Inner class cấu hình cho Rule Generator test
     * 
     * Lưu:
     * - Tên dataset
     * - Ngưỡng support (từ Apriori)
     * - Ngưỡng confidence (cho sinh luật)
     */
    static class RuleConfig {
        String fileName;      // Tên file dataset (ví dụ: "chess.txt")
        double minSup;        // Ngưỡng support dùng cho Apriori (để tìm file input)
        double minConf;       // Ngưỡng confidence dùng cho sinh luật

        RuleConfig(String fileName, double minSup, double minConf) {
            this.fileName = fileName;
            this.minSup = minSup;
            this.minConf = minConf;
        }
    }
}
