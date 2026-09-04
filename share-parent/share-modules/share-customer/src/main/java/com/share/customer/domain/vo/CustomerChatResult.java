package com.share.customer.domain.vo;

import com.share.customer.domain.CustomerMessage;
import com.share.customer.domain.CustomerSession;
import lombok.AllArgsConstructor;
import lombok.Data;

/** 发送消息后的会话快照。 */
@Data
@AllArgsConstructor
public class CustomerChatResult {
    private CustomerSession session;
    private CustomerMessage message;
}
