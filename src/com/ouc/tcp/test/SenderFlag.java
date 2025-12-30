package com.ouc.tcp.test;

public enum SenderFlag {
    NOT_ACKED, ACKED
    // NOT_ACKED: 已发送，但是还未被确认
    // ACKED: 接收方已经成功接收并确认
}
