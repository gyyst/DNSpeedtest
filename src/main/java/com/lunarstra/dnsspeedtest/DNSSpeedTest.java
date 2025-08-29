package com.lunarstra.dnsspeedtest;

import jakarta.inject.Singleton;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * DNS测速核心类
 * 提供DNS解析时间测量功能
 */
@Singleton
public class DNSSpeedTest {

    private static final int TIMEOUT_MS = 5000; // 5秒超时
    private static final int MAX_THREADS = 50; // 最大并发线程数

    /**
     * 测试单个DNS服务器的响应时间
     * @param dnsServer DNS服务器地址
     * @param domain 要解析的域名
     * @return DNS测试结果
     */
    public DNSResult testSingleDNS(String dnsServer, String domain) {
        try {
            // 创建解析器
            SimpleResolver resolver = new SimpleResolver(dnsServer);
            resolver.setTimeout(Duration.ofMillis(TIMEOUT_MS));

            // 创建查询
            Name name = Name.fromString(domain + ".");
            Record record = Record.newRecord(name, Type.A, DClass.IN);
            Message query = Message.newQuery(record);

            // 记录开始时间
            Instant startTime = Instant.now();

            // 发送查询
            Message response = resolver.send(query);

            // 记录结束时间
            Instant endTime = Instant.now();

            // 检查响应
            if (response.getRcode() == Rcode.NOERROR && response.getSectionArray(Section.ANSWER).length > 0) {
                long responseTime = Duration.between(startTime, endTime).toMillis();
                return new DNSResult(dnsServer, responseTime);
            } else {
                return new DNSResult(dnsServer, "DNS查询无结果");
            }

        } catch (Exception e) {
            return new DNSResult(dnsServer, e.getMessage());
        }
    }

    /**
     * 并行测试多个DNS服务器
     * @param dnsServers DNS服务器列表
     * @param domain 要解析的域名
     * @return DNS测试结果列表，按响应时间升序排序
     */
    public List<DNSResult> testMultipleDNS(List<String> dnsServers, String domain) {
        ExecutorService executor = Executors.newFixedThreadPool(
            Math.min(MAX_THREADS, dnsServers.size())
        );

        List<Future<DNSResult>> futures = new ArrayList<>();

        try {
            // 提交所有DNS测试任务
            for (String dnsServer : dnsServers) {
                Future<DNSResult> future = executor.submit(() -> testSingleDNS(dnsServer, domain));
                futures.add(future);
            }

            // 收集结果
            List<DNSResult> results = new ArrayList<>();
            for (Future<DNSResult> future : futures) {
                try {
                    DNSResult result = future.get(TIMEOUT_MS + 1000, TimeUnit.MILLISECONDS);
                    results.add(result);
                } catch (TimeoutException e) {
                    // 如果超时，创建一个超时结果
                    results.add(new DNSResult("超时DNS", "请求超时"));
                } catch (Exception e) {
                    results.add(new DNSResult("错误DNS", e.getMessage()));
                }
            }

            // 按响应时间排序
            results.sort(DNSResult::compareTo);

            return results;

        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 验证DNS服务器地址格式
     * @param dnsServer DNS服务器地址
     * @return 是否为有效的IP地址
     */
    public boolean isValidDNSServer(String dnsServer) {
        if (dnsServer == null || dnsServer.trim().isEmpty()) {
            return false;
        }

        try {
            InetAddress.getByName(dnsServer.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}