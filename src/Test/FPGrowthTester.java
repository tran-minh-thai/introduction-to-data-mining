package Test;

import ItemsetMining.FPGrowth;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FPGrowthTester {
    public static void main(String[] args) {
        List<DatasetConfig> testCases = new ArrayList<>();
        testCases.add(new DatasetConfig("chess.txt", 0.9));
        testCases.add(new DatasetConfig("retail.txt", 0.007));

        System.out.println("==================================================");
        System.out.println(" BẮT ĐẦU CHẠY KIỂM THỬ FP-GROWTH");
        System.out.println("==================================================\n");

        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdir();
        }

        for (DatasetConfig testCase : testCases) {
            String inputPath = "datasets/" + testCase.fileName;

            String baseName = testCase.fileName.replaceFirst("[.][^.]+$", "");

            // THAY ĐỔI: Chèn testCase.minSupport vào tên file
            String baseOutputPath = "results/fpgrowth_FP_" + baseName + "_" + testCase.minSupport;

            System.out.println("-> Đang xử lý: [" + testCase.fileName + "] với ngưỡng minSupport = " + testCase.minSupport);
            long startTime = System.currentTimeMillis();

            try {
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

    static class DatasetConfig {
        String fileName;
        double minSupport;

        DatasetConfig(String fileName, double minSupport) {
            this.fileName = fileName;
            this.minSupport = minSupport;
        }
    }
}