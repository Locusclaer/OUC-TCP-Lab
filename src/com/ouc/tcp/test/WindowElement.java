package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

public interface WindowElement {
    TCP_PACKET getPacket(); // 获取存储的TCP数据包
    void setElem(TCP_PACKET packet, int flag); // 设置元素的数据包和状态
    void resetElem(); // 重置元素为空状态
}
