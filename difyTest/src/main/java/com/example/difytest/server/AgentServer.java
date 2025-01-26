package com.example.difytest.server;

import com.example.difytest.dao.ResponseData;
import com.example.difytest.util.HttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/*
通过接口调用agent
 */
@Service
@Slf4j
public class AgentServer {

//    HttpClient httpClient;
    public ResponseData difyAgent() {
        ResponseData responseData = new ResponseData();
        // 创建一个连接管理器
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(100); // 设置最大连接数
        connManager.setDefaultMaxPerRoute(20); // 每个路由的默认最大连接数

        // 创建一个请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(5000) // 套接字超时
                .setConnectTimeout(5000) // 连接超时
                .setConnectionRequestTimeout(5000) // 请求超时
                .build();

        // 创建一个可关闭的 HttpClient 实例，并应用连接管理器和请求配置
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            // 创建一个POST请求
            HttpPost httpPost = new HttpPost("https://api.dify.ai/v1/workflows/run");
            // 添加请求头
            httpPost.setHeader("Content-Type", "application/json");
            // bearer后续可以配置化作为选择不同agent的key
            httpPost.setHeader("Authorization", "Bearer app-iKEdRpaGoxhtALjgEMNDfhof");

            // 创建请求体的 JSON 数据
            // 测试阶段使用blocking方式即可，线上版本再考虑stream（stream调试不方便）
            String jsonBody = "{"
                    + "\"inputs\": {},"
                    + "\"response_mode\": \"blocking\","
                    + "\"user\": \"abc-123\""
                    + "}";

            // 设置请求体
            StringEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            // 执行请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                // 获取响应状态
                log.info("Response Status: {}", response.getStatusLine());

                // 获取响应实体
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    // 将响应实体转换为字符串
                    String responseBody = EntityUtils.toString(responseEntity);
                    log.info("Response Body: {}", responseBody);

                    // 使用 Jackson 的 ObjectMapper 解析 JSON 响应为 Java 对象
                    ObjectMapper objectMapper = new ObjectMapper();
                    responseData = objectMapper.readValue(responseBody, ResponseData.class);

                    // 访问解析后的对象
                    log.info("Task ID: {}", responseData.getTask_id());
                    log.info("Workflow Run ID: {}", responseData.getWorkflow_run_id());
                    log.info("Status: {}", responseData.getData().getStatus());
                    log.info("Final Output: {}", responseData.getData().getOutputs().getFinalOutput());
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while handling the response", e);
        }
        return responseData;
    }
}
