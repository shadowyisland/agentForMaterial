package com.ruoyi.common.ocr;

import com.baidu.aip.ocr.AipOcr;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;

/**
 * 百度OCR服务
 */
@Service
public class BaiduOcrService {

    private static final Logger log = LoggerFactory.getLogger(BaiduOcrService.class);

    // 1. AppID 直接设为 null
    private static final String APP_ID = null;

    // 2. 修改配置读取路径，匹配你的 YAML: baidu.api.key
    @Value("${baidu.api.key:#{null}}")
    private String apiKey;

    @Value("${baidu.api.secret:#{null}}")
    private String secretKey;

    private AipOcr client;

    @PostConstruct
    public void init() {
        // 3. 移除 appId 的非空检查，只检查 Key 和 Secret
        if (apiKey == null || secretKey == null) {
            log.warn("========== 警告：百度OCR配置缺失 (Key/Secret)，功能不可用 ==========");
            return;
        }

        try {
            // 4. 初始化时，第一个参数传 null
            client = new AipOcr(APP_ID, apiKey, secretKey);

            // 设置超时参数
            client.setConnectionTimeoutInMillis(2000);
            client.setSocketTimeoutInMillis(60000);

            log.info("百度OCR服务初始化成功 (无AppID模式)");
        } catch (Exception e) {
            log.error("百度OCR SDK初始化失败", e);
        }
    }

    /**
     * 通用文字识别（本地文件）
     */
    public String recognizeGeneral(String localFilePath) {
        if (client == null) {
            return "识别失败: OCR服务未配置 (Key/Secret为空)";
        }
        try {
            HashMap<String, String> options = new HashMap<String, String>();
            options.put("language_type", "CHN_ENG");
            options.put("detect_direction", "true");
            options.put("detect_language", "true");
            options.put("probability", "true");

            JSONObject res = client.basicGeneral(localFilePath, options);

            StringBuilder sb = new StringBuilder();
            if (res.has("words_result")) {
                JSONArray wordsResult = res.getJSONArray("words_result");
                for (int i = 0; i < wordsResult.length(); i++) {
                    JSONObject word = wordsResult.getJSONObject(i);
                    sb.append(word.getString("words")).append("\n");
                }
            } else if (res.has("error_msg")) {
                // 处理百度返回的错误信息
                return "识别失败: " + res.getString("error_msg");
            } else {
                return "识别失败: 未知响应 " + res.toString();
            }
            return sb.toString();

        } catch (Exception e) {
            log.error("百度OCR识别异常", e);
            return "识别异常: " + e.getMessage();
        }
    }
}
