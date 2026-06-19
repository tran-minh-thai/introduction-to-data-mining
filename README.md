# Introduction to Data Mining

This repository contains the source code, algorithmic implementations, and experimental setups designed for the **Data Mining** course. It features comprehensive modules for frequent itemset mining, association rule generation, as well as fundamental classification and clustering algorithms. 

The codebase is optimized for performance analysis, incorporating built-in profiling mechanisms to track memory consumption (Peak Memory) and execution time across various dataset sizes.

---

## Features & Modules

### 1. Frequent Itemset Mining
* **Apriori Algorithm:** A level-wise, breadth-first search approach utilizing join and prune steps with explicit logging of candidate sizes ($C_k$), frequent itemset sizes ($L_k$), and subset checks.
* **FP-Growth Algorithm:** A depth-first search approach utilizing a compact Frequent Pattern Tree (FP-Tree) structure. Features include tracking of global node counts, conditional trees built, and data compression ratios.

### 2. Association Rule Generation
* **Rule Generator:** An Apriori-based rule generation algorithm that extracts strong association rules from frequent itemsets using a minimum confidence threshold (`minConf`).
* **Advanced Metrics:** Automatically calculates evaluation metrics including **Support**, **Confidence**, **Lift**, and **Conviction** to filter and rank rules.

### 3. Classification & Clustering
* Includes fundamental algorithms and frameworks for dataset partitioning, pattern distribution mapping, and benchmarking against State-of-the-Art (SOTA) baselines.

---

## Environment & Prerequisites
* **Language:** Java 8 or higher.
* **IDE:** IntelliJ IDEA Ultimate (recommended) or any standard Java IDE.
* **OS:** macOS / Windows / Linux.
* **Resource Recommendation:** For experimental datasets, it is highly recommended to increase the JVM Heap Size (e.g., add `-Xmx4G` to VM options) to prevent `OutOfMemoryError` during recursive tree mining or intensive candidate generations.

---

## Benchmark Datasets (SPMF)

This project is fully compatible with the standard benchmark datasets provided by the [SPMF Open-Source Data Mining Library](https://www.philippe-fournier-viger.com/spmf/index.php?link=datasets.php#d2). 

For rigorous algorithm evaluation, users are encouraged to download and test the following dataset categories directly from SPMF:

* **Dense Datasets:** (e.g., `chess`, `mushroom`, `connect`) 
  * *Characteristics:* Produce a massive number of frequent itemsets even at high minimum support thresholds. Excellent for stress-testing algorithm execution time and search space pruning strategies.
* **Sparse Datasets:** (e.g., `retail`, `kosarak`, `BMS-WebView-1`)
  * *Characteristics:* Contain a large number of distinct items but short average transaction lengths. Ideal for testing memory optimization and tree compression ratios (e.g., in FP-Tree construction).

### Data Format
Input datasets must be configured as standard transaction text files (SPMF format). Each line represents a unique transaction, with individual items (represented as integers) separated by whitespace:
```text
1 3 4
2 3 5
1 2 3 5
