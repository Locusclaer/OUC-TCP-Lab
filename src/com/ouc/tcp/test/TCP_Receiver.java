/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TimerTask;

public class TCP_Receiver extends TCP_Receiver_ADT {

    private TCP_PACKET ackPack;    //回复的ACK报文段
    private ReceiverWindow window = new ReceiverWindow(16);

    private UDT_Timer cumulativeTimer = new UDT_Timer(); // 设置计时器用于进行累计确认

    /*构造函数*/
    public TCP_Receiver() {
        super();    //调用超类构造函数
        super.initTCP_Receiver(this);    //初始化TCP接收端
    }

    @Override
    //接收到数据报：检查校验和，设置回复的ACK报文段
    public void rdt_recv(TCP_PACKET recvPack) {
        //检查校验码，生成ACK
        if (CheckSum.computeChkSum(recvPack) != recvPack.getTcpH().getTh_sum()) {
            System.out.println();
            return;
        }
        // 生成ACK报文段（设置确认号）
        // 对收到的包进行处理，看看属于那种情况
        int bufferResult = window.bufferPacket(recvPack);
        // 接收到期望数据包可以进行存储并累积回复
        if (bufferResult == AckFlag.IS_BASE.ordinal()) {
            TCP_PACKET packet = window.PacketDeliver();
            while (packet != null) {
                // 1. 将数据存入队列
                dataQueue.add(packet.getTcpS().getData());

                // 2. 更新最后交付的序列号
                tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
                ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
                tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
                ackPack.setTcpH(tcpH);

                // 3. 获取下一个包
                packet = window.PacketDeliver();
            }

            // 重新设置计时器，因为此时已经收到了base数据包，需要将计时器重置，如果在500ms内未收到base，则回复
            cumulativeTimer.cancel();
            cumulativeTimer = new UDT_Timer();
            cumulativeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    reply(ackPack);
                }
            }, 500);
        } else if (bufferResult != AckFlag.WITHIN.ordinal()) {
            reply(ackPack);
        }

        // 如果是错误的包，则不再回复 ACK
        System.out.println();
        deliver_data();
    }

    @Override
    //交付数据（将数据写入文件）；不需要修改
    public void deliver_data() {
        //检查dataQueue，将数据写入文件
        File fw = new File("recvData.txt");
        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(fw, true));

            //循环检查data队列中是否有新交付数据
            while (!dataQueue.isEmpty()) {
                int[] data = dataQueue.poll();

                //将数据写入文件
                for (int datum : data) {
                    writer.write(datum + "\n");
                }

                writer.flush();        //清空输出缓存
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    //回复ACK报文段
    public void reply(TCP_PACKET replyPack) {
        //设置错误控制标志
        tcpH.setTh_eflag((byte) 7);    //eFlag=0，信道无错误

        //发送数据报
        client.send(replyPack);
    }

}
