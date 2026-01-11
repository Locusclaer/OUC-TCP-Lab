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

    // 处理接收到的数据包，更新窗口状态
    //处理接收到的数据包，将其缓存在窗口的相应位置，并返回该包的状态，不允许缓存乱序包
    public int bufferPacket(TCP_PACKET packet) {
        // 1. 计算当前收到的包的逻辑序列号
        int packetDataLength = packet.getTcpS().getData().length;
        int seq = (packet.getTcpH().getTh_seq() - 1) / packetDataLength;

        // 2. GBN 核心逻辑：只接受期望的那个序号 (baseSeq)
        if (seq == base) {
            // 是期望的包，存入（或直接交付上层），并准备接收下一个
            // 在 GBN 中，其实不需要 window 数组缓存乱序包，只需要存当前这一个
            window[getIndex(seq)].setElem(packet, ReceiverFlag.BUFFERED.ordinal());

            // 注意：GBN 的 baseSeq 增加通常在接收逻辑处理完后
            return AckFlag.IS_BASE.ordinal();
        }

        // 3. 如果收到的是已处理过的包 (seq < baseSeq)
        if (seq < base) {
            return AckFlag.DUPLICATE.ordinal();
        }

        // 4. 如果收到的是乱序的包 (seq > baseSeq)
        // GBN 直接丢弃乱序包，不进行缓存
        return AckFlag.OUTSIDE.ordinal();
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
