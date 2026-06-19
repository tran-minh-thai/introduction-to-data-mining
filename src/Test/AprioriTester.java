package Test;

import ItemsetMining.Apriori;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * APRIORI TESTER - Kiểm thử thuật toán Apriori
 * 
 * Chạy Apriori trên nhiều dataset và minSupport khác nhau
 * Ghi lại kết quả (itemset phổ biến) và metrics hiệu năng
 * 
 * @author Data Mining Course
 * @version 1.0
 */
public class AprioriTester {
    /**
     * Phương thức main
     * 
     * Cấu hình kiểm thử:
     * - Chess dataset với minSupport = 0.9 (90%) - dataset dày đặc
     * - Retail dataset với minSupport = 0.007 (0.7%) - dataset thưa
     */
    public static void main(String[] args) {
        // ===== ĐỊNH NGHĨA CÁC TEST CASE =====
        List<DatasetConfig> testCases = new ArrayList<>();
        // Dense dataset - minSupport cao
        testCases.add(new DatasetConfig("chess.txt", 0.9));
        // Sparse dataset - minSupport thấp
        testCases.add(new DatasetConfig("retail.txt", 0.007));

        System.out.println("==================================================");
        System.out.println(" BẮT ĐẦU CHẠY KIỂM THỬ APRIORI - CHESS & RETAIL");
        System.out.println("==================================================\n");

        // Tạo thư mục results nếu chưa có
        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdir();
        }

        // ===== THỰC THI TỪNG TEST CASE =====
        for (DatasetConfig testCase : testCases) {
            String inputPath = "datasets/" + testCase.fileName;

            // Trích tên dataset từ tên file (ví dụ: "chess.txt" -> "chess")
            String baseName = testCase.fileName.replaceFirst("[.][^.]+$", "");

            // Tên file xuất: apriori_FP_{dataset}_{minSupport}
            String baseOutputPath = "results/apriori_FP_" + baseName + "_" + testCase.minSupport;

            System.out.println("-> Đang xử lý: [" + testCase.fileName + "] với ngưỡng minSupport = " + testCase.minSupport);
            long startTime = System.currentTimeMillis();

            try {
                // Gọi Apriori để khai phá itemset
                Apriori.executeAndExport(inputPath, baseOutputPath, testCase.minSupport);

                long duration = System.currentTimeMillis() - startTime;

                System.out.println("   + Thời gian chạy tổng cộng: " + duration + " ms");
                System.out.println("   + Hoàn thành! Kết quả tập phổ biến (.txt) và log hiệu năng (.csv) đã được sinh ra.");
                System.out.println("--------------------------------------------------");

            } catch (IOException e) {
                System.err.println("   [LỖI] Không thể xử lý tập dữ liệu " + testCase.fileName + ": " + e.getMessage());
            } catch (OutOfMemoryError e) {
                System.err.println("   [LỖI] Tràn bộ nhớ (Out Of Memory) khi xử lý " + testCase.fileName);
                System.err.println("   -> Gợi ý: Hãy tăng giới hạn heap size (VD: -Xmx4G) hoặc điều chỉnh tăng minSupport.");
                System.out.println("--------------------------------------------------");
            }
        }

        System.out.println("Hoàn thành toàn bộ quá trình kiểm thử!");
    }

    /**
     * Inner class cấu hình dataset
     * Lưu tên file dataset và ngưỡng minSupport tương ứng
     */
    static class DatasetConfig {
        String fileName;      // Tên file (ví dụ: "chess.txt")
        double minSupport;    // Ngưỡng support (ví dụ: 0.9 cho 90%)

        DatasetConfig(String fileName, double minSupport) {
            this.fileName = fileName;
            this.minSupport = minSupport;
        }
    }
}
