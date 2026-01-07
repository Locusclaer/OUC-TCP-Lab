package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

import static com.ouc.tcp.test.SenderFlag.ACKED;
import static com.ouc.tcp.test.SenderFlag.NOT_ACKED;


public class SenderElem implements WindowElement{
    private TCP_PACKET packet;
    private SenderFlag flag;


    // 构造函数
    public SenderElem() {
        this.packet = null;
        this.flag = NOT_ACKED;
    }

    public SenderElem(TCP_PACKET packet, int flag) {
        this.packet = packet;
        this.flag = SenderFlag.values()[flag];
    }

    @Override
    public TCP_PACKET getPacket() {
        return packet;
    }

    @Override
    public void setElem(TCP_PACKET packet, int flag) {
        this.packet = packet;
        this.flag = SenderFlag.values()[flag];
    }

    @Override
    public void resetElem() {
        packet = null;
        flag = NOT_ACKED;
    }

    // 检查数据包是否已收到确认
    public boolean isAcked() {
        return flag == ACKED;
    }

    // 确认数据包已收到
    public void ackPacket() {
        this.flag = ACKED;
    }
}
