package com.ouc.tcp.test;

public enum AckFlag {
    WITHIN, DUPLICATE, OUTSIDE, IS_BASE
    // WITHIN: 接收到的包在接收窗口内，但是不是base包
    // DUPLICATE: 接收到的包是重复的
    // OUTSIDE: 接收到的包不在接收窗口内
    // IS_BASE: 接收到的包是基序号的包，开始交付数据
}
