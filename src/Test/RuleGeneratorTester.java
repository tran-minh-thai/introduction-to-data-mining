package Test;

import ItemsetMining.RuleGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RuleGeneratorTester {
    public static void main(String[] args) {
        List<RuleConfig> testCases = new ArrayList<>();
        // THAY ĐỔI: Cần truyền vào cả ngưỡng minSup (để biết đọc file nào) và minConf (để sinh luật)
        // Ví dụ: file chess đã chạy Apriori ở ngưỡng 0.9, giờ sinh luật với ngưỡng 0.6
        testCases.add(new RuleConfig("chess.txt", 0.9, 0.6));
        testCases.add(new RuleConfig("retail.txt", 0.007, 0.5));

        System.out.println("==================================================");
        System.out.println(" BẮT ĐẦU CHẠY KIỂM THỬ RULE GENERATOR");
        System.out.println("==================================================\n");

        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdir();
        }

        for (RuleConfig testCase : testCases) {
            String baseName = testCase.fileName.replaceFirst("[.][^.]+$", "");

            // THAY ĐỔI: Đường dẫn input giờ phải có minSup kèm theo
            String fpInputPath = "results/apriori_FP_" + baseName + "_" + testCase.minSup + ".txt";

            // THAY ĐỔI: Tên file luật xuất ra sẽ chứa minConf
            String baseOutputPath = "results/apriori_AR_" + baseName + "_" + testCase.minConf;

            System.out.println("-> Đang xử lý: [" + testCase.fileName + "] với ngưỡng minConfidence = " + testCase.minConf);
            long startTime = System.currentTimeMillis();

            try {
                RuleGenerator rg = new RuleGenerator();
                rg.loadFrequentItemsets(fpInputPath);
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

    static class RuleConfig {
        String fileName;
        double minSup;
        double minConf;

        RuleConfig(String fileName, double minSup, double minConf) {
            this.fileName = fileName;
            this.minSup = minSup;
            this.minConf = minConf;
        }
    }
}