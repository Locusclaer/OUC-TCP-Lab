package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;

public class SenderWindow {
    private final LinkedBlockingDeque<SenderElem> window;

    private UDT_Timer timer; // 为发送窗口设置计时器
    private final TCP_Sender sender;
    private final int delay = 3000;
    // private final int period = 3000;

    private int cwnd = 1;
    private double dcwnd = 1.0;
    private int ssthresh = 16;

    private int latestseq = -1; // 最新收到的包的序号
    private int latestseqnum = 0; // 最新收到的包收到的次数
    private int maxlatestseq = 3; // 最多能够收到三个冗余重复包

    // 构造函数
    public SenderWindow(TCP_Sender sender) {
        this.sender = sender;
        this.timer = new UDT_Timer();
        this.window = new LinkedBlockingDeque<>();
    }

    // 超时重传N个分组
    public static class GBN_RetransTask extends TimerTask {
        private final SenderWindow window;

        public GBN_RetransTask(SenderWindow window) {
            this.window = window;
        }

        @Override
        public void run() {
            window.SendtheWindow();
        }
    }

    private void SendtheWindow() {
//        ssthresh = Math.max(cwnd / 2, 2);
//        cwnd = 1;
//        dcwnd = 1.0;
//        SenderElem windowelem = window.peekFirst();
//        if (windowelem != null) {
//            sender.udt_send(windowelem.getPacket());
//        }
        // 发送窗口中的数据
        for (SenderElem elem : window) {
            if (!elem.isAcked()) {
                sender.udt_send(elem.getPacket());
            }
        }
    }

    // 检查窗口是否已满，流量控制，防止发送超过拥塞窗口允许的数据。
    public boolean iscwndFull() {
        return window.size() >= cwnd;
    }

    // 将数据包放入窗口
    public void PushPacket(TCP_PACKET packet) {
        // 如果窗口是空，那么则说明是第一个包
        if (window.isEmpty()) {
            timer = new UDT_Timer();
            timer.schedule(new GBN_RetransTask(this), delay);
        }
        window.addLast(new SenderElem(packet, SenderFlag.NOT_ACKED.ordinal()));
        sender.udt_send(packet);
    }

    // 重置计时器
    public void resetTimer() {
        timer.cancel();
        timer = new UDT_Timer();
        if (!window.isEmpty()) {
            timer.schedule(new GBN_RetransTask(this), delay);
        }
    }

    // 快重传重发期望的数据包
    public void QuickResend (int seq) {
        int expectedseq = seq + 100;
        for (SenderElem elem : window) {
            if (elem.getPacket().getTcpH().getTh_seq() == expectedseq) {
                sender.udt_send(elem.getPacket());
                break;
            }
        }
    }

    // 找到并标记对应数据包为已确认，滑动窗口
    public void AckPacket(int seq) {
        // 移除已经确认的报文、慢开始、拥塞避免
        for (SenderElem elem : window) {
            if (elem.getPacket().getTcpH().getTh_seq() <= seq) {
                elem.ackPacket();
                window.remove(elem);
                if (cwnd < ssthresh) {
                    cwnd++;
                    dcwnd = cwnd;
                }
                // 拥塞避免阶段，加法增大，每次增大窗口分之一，一个RTT后拥塞窗口大小增大MSS
                if (cwnd >= ssthresh) {
                    dcwnd += (double) 1 / cwnd;
                    cwnd = (int) dcwnd;
                }
            } else {
                break;
            }
        }
        // 滑动完窗口之后需要重新对窗口左沿设置计时器
        resetTimer();

        // 收到重复确认进行记录
        if (seq == latestseq) {
            latestseqnum++;
        } else {
            latestseq = seq;
            latestseqnum = 0;
        }

        // 连续收到三个重复包则重传窗口内第一个包，将ssthresh设置为cwnd的一半，cwnd设置为1
        if (latestseqnum == maxlatestseq) {
            ssthresh = cwnd / 2;
            cwnd = ssthresh;
            dcwnd = cwnd;
            QuickResend(seq);
        }
    }
}