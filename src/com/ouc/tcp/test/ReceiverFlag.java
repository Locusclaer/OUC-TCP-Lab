package com.ouc.tcp.test;

public enum ReceiverFlag {
    WAIT, BUFFERED
    // WAIT: 该位置的数据包尚未到达或已被上层应用读取
    // BUFFERED: 数据包已经接收但还未交付
}
