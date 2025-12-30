package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import static com.ouc.tcp.test.SenderFlag.NOT_ACKED;
import static com.ouc.tcp.test.SenderFlag.ACKED;


public class SenderElem implements WindowElement{
    private TCP_PACKET packet;
    private SenderFlag flag;
    private UDT_Timer timer;

    // 构造函数
    public SenderElem() {
        this.packet = null;
        this.flag = NOT_ACKED;
        this.timer = null;
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
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    // 检查数据包是否已收到确认
    public boolean isAcked() {
        return flag == ACKED;
    }

    // 为数据包启动重传定时器
    public void scheduleTask(UDT_RetransTask retransTask, int delay, int period) {
        this.timer = new UDT_Timer();
        this.timer.schedule(retransTask, delay, period);
    }

    // 确认数据包已收到
    public void ackPacket() {
        this.flag = ACKED;
        this.timer.cancel();
    }
}
