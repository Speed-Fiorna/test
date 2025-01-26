package com.example.difytest.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author DUHAOLIN
 * @date 2024/12/2
 */
@Slf4j
@Service
public class HttpClient {

    //客户端从服务端读取数据的超时时间
    private static final int HTTP_TIMEOUT = 5000;
    //空闲的连接超时时间
    private static final int IDLE_TIMEOUT = 5000;
    //整个连接池连接的最大值
    private static final int HTTP_MAX_TOTAL = 10000;
    //客户端与服务器建立连接的超时时间
    private static final int HTTP_CON_TIMEOUT = 2000;
    //路由的默认最大连接
    private static final int HTTP_MAX_PERROUTE = 5000;
    //任务前一次执行结束到下一次执行开始的间隔时间（间隔执行延迟时间）
    private static final int TASK_DELAY = 5000;
    //任务初始化延时
    private static final int TASK_INITIAL_DELAY = 5000;
    //客户端从连接池中获取连接的超时时间
    private static final int HTTP_CON_REQ_TIMEOUT = 1000;
    private static RequestConfig defaultRequestConfig = null;
    private static HttpRequestRetryHandler retryHandler = null;
    private static CloseableHttpClient defaultHttpClient = null;
    private static ScheduledExecutorService monitorExecutor = null;
    private static PoolingHttpClientConnectionManager connManager = null;

    private static final HttpClient httpClient = new HttpClient();

    public static HttpClient getInstance() {
        return httpClient;
    }

    private HttpClient() {
        //创建SSLConnectionSocketFactory
        SSLConnectionSocketFactory factory = getSSLConnectionSocketFactory();

        //创建连接池管理器
        connManager = createPoolConnectManager(factory);

        //设置Socket配置
        setSocketConfig();

        //设置获取连接超时时间，建立连接超时时间，从服务端读取数据的超时时间
        defaultRequestConfig = getRequestConfig();

        //请求失败时,进行请求重试
        retryHandler = retryHandler();

        //创建HttpClient实例
        defaultHttpClient = createHttpClient(factory);

        //开启线程监控，对异常和空闲线程进行关闭
        monitorExecutor = startUpThreadMonitor();

    }

    public CloseableHttpClient getHttpClient()  {
        return defaultHttpClient;
    }

    /**
     * 关闭连接池
     */
    public static void closeConnPool(){
        try {
            defaultHttpClient.close();
            connManager.close();
            monitorExecutor.shutdown();
            log.info("Close the thread pool");
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Closing the thread pool failed", e);
        }
    }

    public RequestConfig getDefaultRequestConfig() {
        return defaultRequestConfig;
    }

    private SSLConnectionSocketFactory getSSLConnectionSocketFactory() {
        X509TrustManager manager = new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

        };

        SSLContext context = null;
        try {
            context = SSLContext.getInstance("TLS");
            //初始化上下文
            context.init(null, new TrustManager[] { manager }, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        assert context != null;
        return new SSLConnectionSocketFactory(context, NoopHostnameVerifier.INSTANCE);
    }

    private PoolingHttpClientConnectionManager createPoolConnectManager(SSLConnectionSocketFactory factory) {
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        Registry<ConnectionSocketFactory> registry = registryBuilder
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", factory)
                .build();
        return new PoolingHttpClientConnectionManager(registry);
    }

    private void setSocketConfig() {
        SocketConfig socketConfig = SocketConfig.custom()
                .setTcpNoDelay(true)
                .build();
        connManager.setDefaultSocketConfig(socketConfig);
        connManager.setMaxTotal(HTTP_MAX_TOTAL);
        connManager.setDefaultMaxPerRoute(HTTP_MAX_PERROUTE);
    }

    private RequestConfig getRequestConfig () {
        return RequestConfig.custom()
                .setSocketTimeout(HTTP_TIMEOUT)
                .setConnectTimeout(HTTP_CON_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_CON_REQ_TIMEOUT)
                .build();
    }

    private HttpRequestRetryHandler retryHandler() {
        return (e, executionCount, httpContext) -> {
            //重试超过3次，放弃请求
            if (executionCount > 3) {
                log.error("retry has more than 3 time, give up request");
                return false;
            }
            //服务器没有响应，可能是服务器断开了连接，应该重试
            if (e instanceof NoHttpResponseException) {
                log.error("receive no response from server, retry");
                return true;
            }
            // SSL握手异常
            if (e instanceof SSLHandshakeException){

                log.error("SSL hand shake exception");
                return false;
            }
            //超时
            if (e instanceof InterruptedIOException){
                log.error("InterruptedIOException");
                return false;
            }
            // 服务器不可达
            if (e instanceof UnknownHostException){
                log.error("server host unknown");
                return false;
            }
            if (e instanceof SSLException){
                log.error("SSLException");
                return false;
            }
            HttpClientContext context = HttpClientContext.adapt(httpContext);
            HttpRequest request = context.getRequest();
            //如果请求不是关闭连接的请求
            return !(request instanceof HttpEntityEnclosingRequest);
        };




    }

    private CloseableHttpClient createHttpClient(SSLConnectionSocketFactory factory) {
        CloseableHttpClient httpClient = HttpClients.custom()
//                .setRetryHandler(retryHandler)
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(defaultRequestConfig)
//                .setSSLSocketFactory(factory)
                .build();
        log.info("HttpClient Build");
        return httpClient;
    }

    private ScheduledExecutorService startUpThreadMonitor() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    //关闭异常连接
                    connManager.closeExpiredConnections();
                    //关闭5s空闲的连接
                    connManager.closeIdleConnections(IDLE_TIMEOUT, TimeUnit.MILLISECONDS);
                    log.debug("close expired and idle for over IDLE_TIMEOUT connection");
                } catch (Exception e) {
                    log.error("close expired or idle for over IDLE_TIMEOUT connection  fail", e);
                }
            }
        }, TASK_INITIAL_DELAY, TASK_DELAY, TimeUnit.MICROSECONDS);

        return executor;
    }

}

