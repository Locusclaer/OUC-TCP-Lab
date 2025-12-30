package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.message.TCP_PACKET;

public class SenderWindow {
    private final int size;
    private final SenderElem[] window;
    // [base, next - 1] -> 已经发送但是还没有被确认
    // [next, rear - 1] -> 可以发送但是还没有发送
    private int base; // 最早发送但尚未确认的数据包的序列号
    private int nextPointer; // 下一个要发送的元素的下标
    private int rearPointer; // 窗口中最后一个已经装入的数据包的位置+1

    // 构造函数
    public SenderWindow(int size) {
        this.size = size;
        this.window = new SenderElem[size];
        for (int i = 0; i < size; i++) {
            this.window[i] = new SenderElem(); // 初始化每个元素
        }
        this.base = 0;
        this.nextPointer = 0;
        this.rearPointer = 0;
    }

    // 将绝对序列号映射到窗口环形缓冲区的索引
    private int getIndex(int seq) {
        return seq % size;
    }

    // 检查窗口是否已满
    public boolean isFull() {
        return rearPointer - base == size;
    }

    // 检查窗口是否为空
    public boolean isEmpty() {
        return base == rearPointer;
    }

    // 检查窗口中的所有数据是否都已发送
    public boolean isFinish() {
        return nextPointer == rearPointer;
    }

    // 将数据包放入窗口
    public void PushPacket(TCP_PACKET packet) {
        int index = getIndex(rearPointer);
        window[index].setElem(packet, SenderFlag.NOT_ACKED.ordinal());
        rearPointer++;
    }

    // 发送下一个数据包，启动重传定时器
    public void SendPacket(TCP_Sender sender, Client client, int delay, int period) {
        // 如果窗口是空的或所有数据都已经被发送则直接退出
        if (isEmpty() || isFinish()) {
            return;
        }

        int index = getIndex(nextPointer);
        TCP_PACKET packet = window[index].getPacket();

        // 重传计时器启动
        window[index].scheduleTask(new UDT_RetransTask(client, packet), delay, period);

        nextPointer++;
        sender.udt_send(packet); // 使用不可靠信道发送数据包
    }

    // 找到并标记对应数据包为已确认，滑动窗口
    public void AckPacket(int seq) {
        // 找到并标记数据包为已确认
        for (int i = base; i != rearPointer; i++) {
            int index = getIndex(i);
            if (!window[index].isAcked() && window[index].getPacket().getTcpH().getTh_seq() == seq) {
                window[index].ackPacket();
                break;
            }
        }

        // 滑动窗口
        while (base != rearPointer && window[getIndex(base)].isAcked()) {
            int index = getIndex(base);
            window[index].resetElem();
            base++;
        }
    }
}
