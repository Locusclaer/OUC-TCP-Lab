package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;

public class SenderWindow {
    private final int size;
    private final SenderElem[] window;
    // [base, next - 1] -> 已经发送但是还没有被确认
    // [next, rear - 1] -> 可以发送但是还没有发送
    private int base; // 最早发送但尚未确认的数据包的序列号
    private int nextPointer; // 下一个要发送的元素的下标
    private int rearPointer; // 窗口中最后一个已经装入的数据包的位置+1

    private UDT_Timer timer; // 为发送窗口设置计时器
    private TCP_Sender sender;
    private int delay;
    private int period;

    // 构造函数
    public SenderWindow(TCP_Sender sender, int size, int delay, int period) {
        this.size = size;
        this.window = new SenderElem[size];
        for (int i = 0; i < size; i++) {
            this.window[i] = new SenderElem(); // 初始化每个元素
        }
        this.base = 0;
        this.nextPointer = 0;
        this.rearPointer = 0;

        this.sender = sender;
        this.timer = new UDT_Timer();
        this.delay = delay;
        this.period = period;
    }

    // 超时重传N个分组
    public class GBN_RetransTask extends TimerTask {
        private TCP_Sender sender;
        private SenderWindow window;

        public GBN_RetransTask(TCP_Sender sender, SenderWindow window) {
            this.sender = sender;
            this.window = window;
        }

        public void run() {
            window.SendtheWindow();
        }
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

    // 检查下一个要发送的数据包是否为base包
    public boolean isBase() {
        return nextPointer == base;
    }

    // 将数据包放入窗口
    public void PushPacket(TCP_PACKET packet) {
        int index = getIndex(rearPointer);
        window[index].setElem(packet, SenderFlag.NOT_ACKED.ordinal());
        rearPointer++;
    }

    // 发送下一个数据包，启动重传定时器
    public void SendPacket() {
        // 如果窗口是空的或所有数据都已经被发送则直接退出
        if (isEmpty() || isFinish()) {
            return;
        }

        int index = getIndex(nextPointer);
        TCP_PACKET packet = window[index].getPacket();

        // 如果是base数据包则重传计时器启动
        if (isBase()) {
            timer.schedule(new GBN_RetransTask(sender, this), delay, period);
        }

        nextPointer++;
        sender.udt_send(packet); // 使用不可靠信道发送数据包
    }

    // 发送窗口中的所有数据包
    public void SendtheWindow() {
        nextPointer = base;
        while (nextPointer < rearPointer) {
            SendPacket();
        }
    }


    // 重置计时器
    public void resetTimer() {
        timer.cancel();
        timer = new UDT_Timer();
        if (!isEmpty()) {
            timer.schedule(new GBN_RetransTask(sender, this), delay, period);
        }
    }

    // 找到并标记对应数据包为已确认，滑动窗口
    public void AckPacket(int seq) {
        // 找到并标记数据包为已确认，重置计时器并滑动窗口
        for (int i = base; i != rearPointer; i++) {
            int index = getIndex(i);
            // 累计确认
            if (!window[index].isAcked() && window[index].getPacket().getTcpH().getTh_seq() <= seq) {
                window[index].ackPacket();
                window[index].resetElem();
                base++;
                resetTimer(); // 重启计时器
            }
        }
    }
}
