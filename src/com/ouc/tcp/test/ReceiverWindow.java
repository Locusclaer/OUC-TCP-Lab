package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

public class ReceiverWindow {
    private final int size; // 窗口大小一旦设置就不能改变
    private final ReceiverElem[] window; // 数组引用不可变但是内容可变
    private int base; // 窗口的基序号，即下一个期望接收的包的序号

    // 构造函数
    public ReceiverWindow(int size) {
        this.size = size;
        this.window = new ReceiverElem[size];
        for (int i = 0; i < size; i++) {
            this.window[i] = new ReceiverElem();
        }
        this.base = 0;
    }

    // 将绝对序列号映射到窗口环形缓冲区的索引
    private int getIndex(int seq) {
        return seq % size;
    }

    public int bufferPacket(TCP_PACKET packet) {
        // 计算数据包的相对序列号
        int seq = (packet.getTcpH().getTh_seq() - 1) / packet.getTcpS().getData().length;

        // Condition1: 序列号超出窗口范围
        if (seq >= (base + size)) {
            return AckFlag.OUTSIDE.ordinal();
        }

        // Condition2: 序列号小于base
        if (seq < base) {
            return AckFlag.DUPLICATE.ordinal();
        }

        // Condition3 and 4: 序列号在窗口内
        window[getIndex(seq)].setElem(packet, ReceiverFlag.BUFFERED.ordinal());

        // Condition3: 序列号正好就是期望的下一个包的序号
        if (seq == base) {
            return AckFlag.IS_BASE.ordinal();
        }

        // Condition4: 序列号在窗口内但是不是期望的下一个包的序号
        return AckFlag.WITHIN.ordinal();
    }

    // 按序交付数据包给上层应用
    public TCP_PACKET PacketDeliver() {
        // 首先需要检查base位置的包是否已经接收到，若没有收到则不能交付
        if (!window[getIndex(base)].isBuffered()) {
            return null;
        }

        // 获取base位置的数据包
        TCP_PACKET packet = window[getIndex(base)].getPacket();

        // 交付后需要重置状态为WAIT，然后窗口向右滑动
        window[getIndex(base)].resetElem();
        base++;

        return packet;
    }
}
