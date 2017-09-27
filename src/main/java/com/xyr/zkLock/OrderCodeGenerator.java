package com.xyr.zkLock;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 订单编码生成器
 * Created by xyr on 2017/9/27.
 */
public class OrderCodeGenerator {

    /**
     * 自增长系列
     */
    private static int i = 0;

    public String getOrderCode() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-SS");
        return sdf.format(date) + "-" + ++i;
    }

    public static void main(String[] args) {
        OrderCodeGenerator orderCodeGenerator = new OrderCodeGenerator();
        for (int j = 0; j < 10; j ++) {
            System.out.println(orderCodeGenerator.getOrderCode());
        }
    }

}
