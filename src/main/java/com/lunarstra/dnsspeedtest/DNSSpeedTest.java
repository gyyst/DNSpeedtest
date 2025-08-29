package com.lunarstra.dnsspeedtest;

import jakarta.inject.Singleton;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 * DNS测速核心类
 * 提供DNS解析时间测量功能，使用Java标准库实现
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
            // 配置DNS服务器
            Properties env = new Properties();
            env.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            env.setProperty(Context.PROVIDER_URL, "dns://" + dnsServer + "/");
            env.setProperty("com.sun.jndi.dns.timeout.initial", String.valueOf(TIMEOUT_MS));
            env.setProperty("com.sun.jndi.dns.timeout.retries", "1");
            
            // 记录开始时间
            Instant startTime = Instant.now();
            
            // 创建DNS上下文并执行查询
            DirContext ctx = null;
            try {
                ctx = new InitialDirContext(env);
                Attributes attrs = ctx.getAttributes(domain, new String[]{"A"});
                
                // 记录结束时间
                Instant endTime = Instant.now();
                
                // 检查是否有A记录
                Attribute aRecord = attrs.get("A");
                if (aRecord != null && aRecord.size() > 0) {
                    long responseTime = Duration.between(startTime, endTime).toMillis();
                    // 获取第一个解析结果IP地址
                    String resolvedIP = aRecord.get(0).toString();
                    return new DNSResult(dnsServer, responseTime, resolvedIP);
                } else {
                    return new DNSResult(dnsServer, "没有找到A记录");
                }
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (NamingException e) {
                        // 忽略关闭异常
                    }
                }
            }
            
        } catch (NamingException e) {
            return new DNSResult(dnsServer, "DNS查询失败: " + e.getMessage());
        } catch (Exception e) {
            return new DNSResult(dnsServer, "测试异常: " + e.getMessage());
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
            for (int i = 0; i < futures.size(); i++) {
                try {
                    Future<DNSResult> future = futures.get(i);
                    DNSResult result = future.get(TIMEOUT_MS + 1000, TimeUnit.MILLISECONDS);
                    results.add(result);
                } catch (TimeoutException e) {
                    // 如果超时，创建一个超时结果
                    String dnsServer = dnsServers.get(i);
                    results.add(new DNSResult(dnsServer, "请求超时"));
                } catch (Exception e) {
                    String dnsServer = dnsServers.get(i);
                    results.add(new DNSResult(dnsServer, "执行异常: " + e.getMessage()));
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
        } catch (UnknownHostException e) {
            return false;
        }
    }
}