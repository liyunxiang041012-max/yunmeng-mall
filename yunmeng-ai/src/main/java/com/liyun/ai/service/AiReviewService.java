package com.liyun.ai.service;

import com.liyun.ai.model.ItemReviewRequest;
import com.liyun.ai.model.ItemReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final DeepSeekService deepSeekService;

    /**
     * 本地快速规则检查（不调 AI，省 Token）
     * 返回 null 表示本地规则没有发现问题，需要 AI 进一步判断
     */
    private ItemReviewResponse localCheck(ItemReviewRequest item) {
        String name = item.getName();

        // 规则1：名称为空
        if (name == null || name.isBlank()) {
            return new ItemReviewResponse("reject", "【违规】商品名称为空");
        }
        // 规则2：名称过短（单字无意义）
        if (name.trim().length() < 2) {
            return new ItemReviewResponse("reject", "【违规】商品名称过短（少于2个字符），无法识别商品");
        }
        // 规则3：纯数字名称
        if (name.matches("^\\d+$")) {
            return new ItemReviewResponse("reject", "【违规】商品名称为纯数字'" + name + "'，不是有效商品名");
        }
        // 规则4：纯符号名称
        if (name.matches("^[^a-zA-Z\\u4e00-\\u9fa5\\d]+$")) {
            return new ItemReviewResponse("reject", "【违规】商品名称仅含符号，不是有效商品名");
        }
        // 规则5：名称超过100字符（太啰嗦）
        if (name.length() > 100) {
            return new ItemReviewResponse("reject", "【违规】商品名称过长（超过100字符），请精简到100字符以内");
        }
        // 规则6：价格异常
        if (item.getPrice() != null && item.getPrice() <= 0) {
            return new ItemReviewResponse("reject", "【违规】商品价格必须大于0");
        }
        // 规则7：库存为负
        if (item.getStock() != null && item.getStock() < 0) {
            return new ItemReviewResponse("reject", "【违规】库存不能为负数");
        }
        return null; // 本地规则通过，交给 AI 判断
    }

    private static final String REVIEW_PROMPT = """
        你是云梦商城的内容审核助手。只审核文字信息，图片由人工审核。

        严格按以下规则判断，返回 JSON（不要加 ``` 标记）：
        {"suggestion":"approve|reject","reason":"具体原因，一句话"}

        【必须拒绝 reject】
        - 含脏话、人身攻击、政治敏感、色情暗示
        - 含联系方式（手机号、微信号、QQ号、网址）
        - 含违法信息（枪支、毒品、赌博、假币、盗版）
        - 商品名与商城品类完全无关（如"今天天气真好"）
        - 明显刷单/测试数据（如"测试商品123"、"111"）

        【建议通过 approve】
        - 名称清晰描述商品，中英文均可，如"iPhone 15 Pro Max"
        - 名称包含品牌+品类+型号，如"华为Mate 60手机"
        - 名称虽简短但明确，如"男士T恤"、"有机大米"

        【不确定 review】
        - 名称模糊但有可能是真实商品，如"新款上市"、"特价清仓"
        - 无法判断是否存在违规但感觉不太对劲
        """;

    public ItemReviewResponse review(ItemReviewRequest item) {
        // 1. 本地规则快速拦截
        ItemReviewResponse local = localCheck(item);
        if (local != null) {
            return local;
        }
        // 2. AI 深度检查
        try {
            String result = deepSeekService.chat(REVIEW_PROMPT, buildPrompt(item), null);
            if (result != null) {
                return parseResult(result);
            }
        } catch (Exception e) {
            log.error("AI审核调用失败", e);
        }
        return new ItemReviewResponse("review", "AI审核服务暂不可用，请人工审核");
    }

    private String buildPrompt(ItemReviewRequest item) {
        return String.format("审核商品：名称='%s'，价格=%.2f元，库存=%d",
            item.getName() != null ? item.getName() : "",
            item.getPrice() != null ? item.getPrice() / 100.0 : 0,
            item.getStock() != null ? item.getStock() : 0);
    }

    private ItemReviewResponse parseResult(String result) {
        result = result.trim().replaceAll("```json|```", "").trim();
        String suggestion = "review";
        String reason = result;
        if (result.contains("\"suggestion\"")) {
            suggestion = extractJsonValue(result, "suggestion");
            reason = extractJsonValue(result, "reason");
            if (reason == null) reason = result;
        }
        if (!"approve".equals(suggestion) && !"reject".equals(suggestion)) {
            suggestion = "review";
        }
        return new ItemReviewResponse(suggestion, reason);
    }

    private String extractJsonValue(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"")
            .matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
