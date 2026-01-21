package com.ruoyi.framework.web.service;

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

    @Value("${baidu.ocr.app-id:#{null}}")
    private String appId;

    @Value("${baidu.ocr.api-key:#{null}}")
    private String apiKey;

    @Value("${baidu.ocr.secret-key:#{null}}")
    private String secretKey;

    private AipOcr client;

    @PostConstruct
    public void init() {
        // 增加判空逻辑。如果配置为空，打印警告但不阻断启动
        if (appId == null || apiKey == null || secretKey == null) {
            log.warn("========== 警告：百度OCR配置缺失，OCR功能将不可用，但系统已正常启动 ==========");
            // 如果你希望即使参数为空也强行初始化 client (像你发的那个项目一样)，可以解开下面这行：
            // client = new AipOcr(appId, apiKey, secretKey);
            return;
        }

        // 正常初始化
        try {
            // 初始化一个AipOcr
            client = new AipOcr(appId, apiKey, secretKey);
            // 可选：设置网络连接参数
            client.setConnectionTimeoutInMillis(2000);
            client.setSocketTimeoutInMillis(60000);
            log.info("百度OCR服务初始化成功");
        } catch (Exception e) {
            log.error("百度OCR SDK初始化失败", e);
        }
    }

    /**
     * 通用文字识别（本地文件）
     * @param localFilePath 本地文件绝对路径
     * @return 识别结果字符串
     */
    public String recognizeGeneral(String localFilePath) {
        // 使用前先检查 client 是否初始化
        if (client == null) {
            return "识别失败: OCR服务未配置 (AppID/Key/Secret为空)";
        }
        try {
            // 传入可选参数调用接口
            HashMap<String, String> options = new HashMap<String, String>();
            options.put("language_type", "CHN_ENG");
            options.put("detect_direction", "true");
            options.put("detect_language", "true");
            options.put("probability", "true");

            // 调用接口
            JSONObject res = client.basicGeneral(localFilePath, options);

            // 解析返回结果
            StringBuilder sb = new StringBuilder();
            if (res.has("words_result")) {
                JSONArray wordsResult = res.getJSONArray("words_result");
                for (int i = 0; i < wordsResult.length(); i++) {
                    JSONObject word = wordsResult.getJSONObject(i);
                    sb.append(word.getString("words")).append("\n");
                }
            } else {
                log.error("OCR识别失败或无结果: {}", res.toString());
                return "识别失败: " + res.toString();
            }
            return sb.toString();

        } catch (Exception e) {
            log.error("百度OCR识别异常", e);
            return "识别异常: " + e.getMessage();
        }
    }
}
