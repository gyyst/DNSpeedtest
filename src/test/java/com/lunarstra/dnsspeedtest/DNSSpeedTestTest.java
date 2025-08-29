package com.lunarstra.dnsspeedtest;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.List;

@QuarkusTest
public class DNSSpeedTestTest {

    @Inject
    DNSSpeedTest dnsSpeedTest;

    @Test
    public void testValidDNSServer() {
        // 测试有效的DNS服务器地址
        Assertions.assertTrue(dnsSpeedTest.isValidDNSServer("8.8.8.8"));
        Assertions.assertTrue(dnsSpeedTest.isValidDNSServer("1.1.1.1"));
        Assertions.assertTrue(dnsSpeedTest.isValidDNSServer("114.114.114.114"));
    }

    @Test
    public void testInvalidDNSServer() {
        // 测试无效的DNS服务器地址
        Assertions.assertFalse(dnsSpeedTest.isValidDNSServer(null));
        Assertions.assertFalse(dnsSpeedTest.isValidDNSServer(""));
        Assertions.assertFalse(dnsSpeedTest.isValidDNSServer("   "));
        Assertions.assertFalse(dnsSpeedTest.isValidDNSServer("invalid.dns"));
        Assertions.assertFalse(dnsSpeedTest.isValidDNSServer("999.999.999.999"));
    }

    @Test
    public void testSingleDNSSpeed() {
        // 测试单个DNS服务器（使用Google DNS）
        DNSResult result = dnsSpeedTest.testSingleDNS("8.8.8.8", "www.baidu.com");
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals("8.8.8.8", result.getDnsServer());
        
        if (result.isSuccess()) {
            Assertions.assertTrue(result.getResponseTime() > 0);
            Assertions.assertNotNull(result.getResolvedIP());
            System.out.println("DNS测试成功: " + result.toString());
        } else {
            System.out.println("DNS测试失败: " + result.toString());
        }
    }

    @Test
    public void testMultipleDNSSpeed() {
        // 测试多个DNS服务器
        List<String> dnsServers = Arrays.asList(
            "8.8.8.8",      // Google DNS
            "1.1.1.1",      // Cloudflare DNS
            "114.114.114.114" // 114 DNS
        );
        
        List<DNSResult> results = dnsSpeedTest.testMultipleDNS(dnsServers, "www.baidu.com");
        
        Assertions.assertNotNull(results);
        Assertions.assertEquals(3, results.size());
        
        System.out.println("多DNS测试结果:");
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, results.get(i).toString());
        }
        
        // 验证结果是否按响应时间排序（成功的结果）
        DNSResult prev = null;
        for (DNSResult result : results) {
            if (result.isSuccess() && prev != null && prev.isSuccess()) {
                Assertions.assertTrue(prev.getResponseTime() <= result.getResponseTime());
            }
            prev = result;
        }
    }

    @Test
    public void testDNSResultComparison() {
        // 测试DNSResult的比较功能
        DNSResult result1 = new DNSResult("8.8.8.8", 50, "142.251.43.4");
        DNSResult result2 = new DNSResult("1.1.1.1", 100, "142.251.43.4");
        DNSResult result3 = new DNSResult("114.114.114.114", "测试失败");
        
        Assertions.assertTrue(result1.compareTo(result2) < 0);
        Assertions.assertTrue(result2.compareTo(result3) < 0);
        Assertions.assertTrue(result1.compareTo(result3) < 0);
    }
}