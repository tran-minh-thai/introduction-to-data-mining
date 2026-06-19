package Test;

import ItemsetMining.Apriori;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AprioriTester {
    public static void main(String[] args) {
        List<DatasetConfig> testCases = new ArrayList<>();
        testCases.add(new DatasetConfig("chess.txt", 0.9));
        testCases.add(new DatasetConfig("retail.txt", 0.007));

        System.out.println("==================================================");
        System.out.println(" BẮT ĐẦU CHẠY KIỂM THỬ APRIORI - CHESS & RETAIL");
        System.out.println("==================================================\n");

        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdir();
        }

        for (DatasetConfig testCase : testCases) {
            String inputPath = "datasets/" + testCase.fileName;

            String baseName = testCase.fileName.replaceFirst("[.][^.]+$", "");

            // THAY ĐỔI: Chèn testCase.minSupport vào tên file baseOutputPath
            String baseOutputPath = "results/apriori_FP_" + baseName + "_" + testCase.minSupport;

            System.out.println("-> Đang xử lý: [" + testCase.fileName + "] với ngưỡng minSupport = " + testCase.minSupport);
            long startTime = System.currentTimeMillis();

            try {
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

    static class DatasetConfig {
        String fileName;
        double minSupport;

        DatasetConfig(String fileName, double minSupport) {
            this.fileName = fileName;
            this.minSupport = minSupport;
        }
    }
}