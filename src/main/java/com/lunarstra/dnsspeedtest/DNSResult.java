package com.lunarstra.dnsspeedtest;

/**
 * DNS测试结果封装类
 * 用于存储DNS服务器地址、响应时间和解析结果
 */
public class DNSResult implements Comparable<DNSResult> {
    private final String dnsServer;
    private final long responseTime; // 以毫秒为单位
    private final boolean success;
    private final String errorMessage;
    private final String resolvedIP; // 解析出的IP地址

    public DNSResult(String dnsServer, long responseTime, String resolvedIP) {
        this.dnsServer = dnsServer;
        this.responseTime = responseTime;
        this.success = true;
        this.errorMessage = null;
        this.resolvedIP = resolvedIP;
    }

    public DNSResult(String dnsServer, String errorMessage) {
        this.dnsServer = dnsServer;
        this.responseTime = Long.MAX_VALUE; // 失败的DNS设置为最大值，排序时会排在最后
        this.success = false;
        this.errorMessage = errorMessage;
        this.resolvedIP = null;
    }

    public String getDnsServer() {
        return dnsServer;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getResolvedIP() {
        return resolvedIP;
    }

    @Override
    public int compareTo(DNSResult other) {
        // 按响应时间升序排序
        return Long.compare(this.responseTime, other.responseTime);
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("%s (%dms) -> %s", dnsServer, responseTime, resolvedIP);
        } else {
            return String.format("%s (失败: %s)", dnsServer, errorMessage);
        }
    }

    /**
     * 获取用于输出到文件的格式
     * @return DNS服务器地址
     */
    public String toFileFormat() {
        return dnsServer;
    }
}