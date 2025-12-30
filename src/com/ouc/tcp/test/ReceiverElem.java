package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

import static com.ouc.tcp.test.ReceiverFlag.WAIT;
import static com.ouc.tcp.test.ReceiverFlag.BUFFERED;

// 表示接收方滑动窗口中的一个数据单元，用于管理接收到的数据包状态。
public class ReceiverElem implements WindowElement{
    private TCP_PACKET packet;
    private ReceiverFlag flag;

    // 构造函数
    public ReceiverElem() {
        this.packet = null;
        this.flag = WAIT;
    }

    @Override
    public TCP_PACKET getPacket() {
        return packet;
    }

    @Override
    public void setElem(TCP_PACKET packet, int flag) {
        this.packet = packet;
        this.flag = ReceiverFlag.values()[flag];
    }

    @Override
    public void resetElem() {
        packet = null;
        flag = WAIT;
    }

    // 判断该位置的数据包是否已到达但未确认
    public boolean isBuffered() {
        return flag == BUFFERED;
    }
}
