package com.lunarstra.dnsspeedtest;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * DNS测速工具命令行主类
 */
@QuarkusMain
@Command(name = "dnsspeedtest", mixinStandardHelpOptions = true, 
         version = "DNS Speed Test 1.0",
         description = "DNS服务器响应速度测试工具")
public class DNSSpeedTestRunner implements Callable<Integer>, QuarkusApplication {

    @Inject
    DNSSpeedTest dnsSpeedTest;

    @Parameters(index = "0", description = "包含DNS服务器地址的文本文件路径（每行一个DNS地址），默认：dns.txt", defaultValue = "dns.txt")
    private String dnsFile = "dns.txt";

    @Option(names = {"-d", "--domain"}, description = "要解析的域名（默认：www.baidu.com）")
    private String domain = "www.baidu.com";

    @Option(names = {"-o", "--output"}, description = "输出文件路径（默认：dns_speed_result_时间戳.txt）")
    private String outputFile;

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this).execute(args);
    }

    @Override
    public Integer call() throws Exception {
        try {
            System.out.println("=== DNS测速工具 ===");
            System.out.println("DNS文件: " + dnsFile);
            System.out.println("目标域名: " + domain);
            
            // 检查DNS文件是否存在
            Path dnsFilePath = Paths.get(dnsFile);
            if (!Files.exists(dnsFilePath)) {
                System.err.println("错误：DNS文件不存在: " + dnsFile);
                return 1;
            }

            // 读取DNS服务器列表
            List<String> dnsServers = readDNSServers(dnsFilePath);
            if (dnsServers.isEmpty()) {
                System.err.println("错误：DNS文件为空或没有有效的DNS服务器地址");
                return 1;
            }

            System.out.println("找到 " + dnsServers.size() + " 个DNS服务器");
            System.out.println("开始测试...\n");

            // 执行DNS测速
            List<DNSResult> results = dnsSpeedTest.testMultipleDNS(dnsServers, domain);

            // 显示结果
            System.out.println("=== 测试结果 ===");
            for (int i = 0; i < results.size(); i++) {
                DNSResult result = results.get(i);
                System.out.printf("%2d. %s%n", i + 1, result.toString());
            }

            // 生成输出文件名
            if (outputFile == null) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                outputFile = "dns_speed_result_" + timestamp + ".txt";
            }

            // 写入结果文件
            writeResults(results, outputFile);
            System.out.println("\n结果已保存到文件: " + outputFile);

            return 0;
            
        } catch (Exception e) {
            System.err.println("发生错误: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * 从文件中读取DNS服务器列表（使用UTF-8编码）
     */
    private List<String> readDNSServers(Path filePath) throws IOException {
        List<String> dnsServers = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // 跳过空行和注释行
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                
                // 验证DNS服务器地址
                if (dnsSpeedTest.isValidDNSServer(line)) {
                    dnsServers.add(line);
                } else {
                    System.out.printf("警告：第%d行包含无效的DNS地址: %s%n", lineNumber, line);
                }
            }
        }
        
        return dnsServers;
    }

    /**
     * 将结果写入文件（使用UTF-8编码）
     */
    private void writeResults(List<DNSResult> results, String fileName) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8))) {
            // 写入文件头信息
            writer.println("# DNS测速结果 - 生成时间: " + 
                          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("# 目标域名: " + domain);
            writer.println("# 按响应时间升序排序");
            writer.println();
            
            // 写入DNS服务器列表（按响应时间排序）
            for (DNSResult result : results) {
                if (result.isSuccess()) {
                    writer.println(result.toFileFormat());
                }
            }
            
            // 如果有失败的DNS，追加到文件末尾
            boolean hasFailures = false;
            for (DNSResult result : results) {
                if (!result.isSuccess()) {
                    if (!hasFailures) {
                        writer.println();
                        writer.println("# 以下DNS服务器测试失败:");
                        hasFailures = true;
                    }
                    writer.println("# " + result.toFileFormat() + " - " + result.getErrorMessage());
                }
            }
        }
    }
}