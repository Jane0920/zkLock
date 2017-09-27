package com.xyr.zkLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xyr on 2017/9/27.
 */
public class OrderServiceImpl implements Runnable {

    private static Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final int NUM = 10;

    private static OrderCodeGenerator orderCodeGenerator = new OrderCodeGenerator();

    //同步工具类，线程并发发令枪
    private static CountDownLatch cdl = new CountDownLatch(NUM);

    //jdk自带的可重入锁 使用jdk自带的锁的方式 但是这种方式只适用于没有分布式的情况下 如果分布式则不适用
    //private static Lock lock = new ReentrantLock();

    //通过zookeeper的分布式锁
    private Lock lock = new ZookeeperLock();

    /**
     * 生成订单号
     */
    private void createCode() {

        //不进行锁 此时可能会出现多个编号相同的订单
        /*String code = orderCodeGenerator.getOrderCode();

        log.info(Thread.currentThread().getName() +
            "===================================" + code);*/

        lock.lock();
        try {
            String code = orderCodeGenerator.getOrderCode();
            log.info(Thread.currentThread().getName() +
                    "===================================" + code);
        } catch (Exception e) {

        } finally {
            lock.unlock();
        }

    }

    @Override
    public void run() {
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        createCode();
    }

    public static void main(String[] args) {
        for (int i = 1; i <= NUM; i ++) {
            new Thread(new OrderServiceImpl()).start();

            cdl.countDown();
        }
    }
}
