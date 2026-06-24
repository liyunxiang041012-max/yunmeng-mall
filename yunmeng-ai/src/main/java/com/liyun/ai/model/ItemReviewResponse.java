package com.liyun.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemReviewResponse {

    /** approve=建议通过, reject=建议驳回, review=需人工判断 */
    private String suggestion;

    /** AI 审核意见 */
    private String reason;
}
