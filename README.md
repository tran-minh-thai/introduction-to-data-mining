# Frequent Itemset Mining & Rule Generation

Dự án này cung cấp mã nguồn Java cơ bản cho các thuật toán khai thác tập phổ biến và sinh luật kết hợp, bao gồm **Apriori** và **FP-Growth**. Hệ thống được thiết kế đặc biệt để phục vụ phân tích học thuật, tích hợp sẵn bộ đo lường (profiling) chi tiết để đánh giá mức tiêu thụ bộ nhớ (Peak Memory) và thời gian thực thi.

## Yêu cầu môi trường
* **Ngôn ngữ:** Java 8 hoặc cao hơn.
* **IDE:** IntelliJ IDEA (khuyên dùng).
* **Tài nguyên:** Khuyến nghị thiết lập Heap Size tối thiểu `-Xmx4G` khi chạy với các tập dữ liệu thưa (sparse datasets) để tránh tràn bộ nhớ.

## Cấu trúc dữ liệu đầu vào (Datasets)
Hệ thống nhận đầu vào là các file text chứa dữ liệu giao dịch (ví dụ: `chess.txt`, `retail.txt`). Mỗi dòng biểu diễn một giao dịch, các item (số nguyên) được phân tách bằng khoảng trắng:
1 3 4
2 3 5
1 2 3 5

## Hướng dẫn sử dụng
Để chạy kiểm thử tự động với nhiều cấu hình ngưỡng khác nhau, vui lòng thực thi các lớp điều phối (Tester):
1. Cấu hình danh sách file và `minSupport` bên trong `AprioriTester.java` hoặc `FPGrowthTester.java`, sau đó chạy chương trình.
2. Để sinh luật kết hợp, cấu hình `minConfidence` và chạy `RuleGeneratorTester.java` dựa trên kết quả đã sinh ra từ bước 1.

## Kết quả đầu ra (Output)
Toàn bộ kết quả sẽ được lưu tự động vào thư mục `results/` với cấu trúc tên file phân biệt rõ theo ngưỡng:
* **File `.txt`**: Danh sách chi tiết các mẫu phổ biến hoặc luật kết hợp.
* **File `.csv`**: Báo cáo Profiling bao gồm các số liệu quan trọng: thời gian I/O, thời gian thuật toán, số lượng nhánh bị tỉa (Pruned branches), nén cây FP-Tree, và Peak Memory.