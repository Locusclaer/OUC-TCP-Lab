package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.message.TCP_SEGMENT;

import java.util.zip.CRC32;

public class CheckSum {
	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
        CRC32 crc32 = new CRC32();
		TCP_HEADER header = tcpPack.getTcpH();
		TCP_SEGMENT segment = tcpPack.getTcpS();

		// seq (4 B)
		int seq = header.getTh_seq();
		crc32.update(seq);

		// ack (4 B)
		int ack = header.getTh_ack();
		crc32.update(ack);

		// data
		int[] data = segment.getData();
		for (int i : data) {
			crc32.update(i);
		}

        return (short) crc32.getValue();
	}
}
