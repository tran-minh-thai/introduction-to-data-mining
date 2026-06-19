package Test;

import ItemsetMining.FPGrowth;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * FP-GROWTH TESTER - Kiểm thử thuật toán FP-Growth
 * 
 * Chạy FP-Growth trên nhiều dataset và minSupport khác nhau
 * So sánh hiệu năng với Apriori thông qua metrics
 * 
 * @author Data Mining Course
 * @version 1.0
 */
public class FPGrowthTester {
    /**
     * Phương thức main
     * 
     * Cấu hình kiểm thử:
     * - Chess dataset với minSupport = 0.9 (dense)
     * - Retail dataset với minSupport = 0.007 (sparse)
     * 
     * Ghi chú:
     * - FP-Growth thường nhanh hơn Apriori trên sparse dataset
     * - FP-Tree nén dữ liệu tốt khi có pattern lặp lại
     */
    public static void main(String[] args) {
        // ===== ĐỊNH NGHĨA CÁC TEST CASE =====
        List<DatasetConfig> testCases = new ArrayList<>();
        // Dense dataset
        testCases.add(new DatasetConfig("chess.txt", 0.9));
        // Sparse dataset
        testCases.add(new DatasetConfig("retail.txt", 0.007));

        System.out.println("==================================================");
        System.out.println(" BẮT ĐẦU CHẠY KIỂM THỬ FP-GROWTH");
        System.out.println("==================================================\n");

        // Tạo thư mục results nếu chưa có
        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdir();
        }

        // ===== THỰC THI TỪNG TEST CASE =====
        for (DatasetConfig testCase : testCases) {
            String inputPath = "datasets/" + testCase.fileName;

            // Trích tên dataset (ví dụ: "chess.txt" -> "chess")
            String baseName = testCase.fileName.replaceFirst("[.][^.]+$", "");

            // Tên file xuất: fpgrowth_FP_{dataset}_{minSupport}
            String baseOutputPath = "results/fpgrowth_FP_" + baseName + "_" + testCase.minSupport;

            System.out.println("-> Đang xử lý: [" + testCase.fileName + "] với ngưỡng minSupport = " + testCase.minSupport);
            long startTime = System.currentTimeMillis();

            try {
                // Gọi FP-Growth để khai phá itemset
                FPGrowth.executeAndExport(inputPath, baseOutputPath, testCase.minSupport);

                long duration = System.currentTimeMillis() - startTime;

                System.out.println("   + Thời gian chạy tổng cộng: " + duration + " ms");
                System.out.println("   + Hoàn thành! Kết quả tập phổ biến (.txt) và log (.csv) đã được lưu vào thư mục results.");
                System.out.println("--------------------------------------------------");

            } catch (IOException e) {
                System.err.println("   [LỖI] Không thể xử lý tập dữ liệu " + testCase.fileName + ": " + e.getMessage());
                System.out.println("--------------------------------------------------");
            } catch (OutOfMemoryError e) {
                System.err.println("   [LỖI] Tràn bộ nhớ (Out Of Memory) khi xây dựng cây FP-Tree cho " + testCase.fileName);
                System.err.println("   -> Gợi ý: Dữ liệu quá thưa (sparse) làm cây phình to. Hãy tăng ngưỡng minSupport.");
                System.out.println("--------------------------------------------------");
            }
        }

        System.out.println("Hoàn thành toàn bộ quá trình kiểm thử FP-Growth!");
    }

    /**
     * Inner class cấu hình dataset
     */
    static class DatasetConfig {
        String fileName;      // Tên file dataset
        double minSupport;    // Ngưỡng support

        DatasetConfig(String fileName, double minSupport) {
            this.fileName = fileName;
            this.minSupport = minSupport;
        }
    }
}
