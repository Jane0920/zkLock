package com.xyr.zkLock;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Created by xyr on 2017/9/27.
 */
public class ZookeeperLock implements Lock {

    private static final String ZK_IP_PORT = "106.14.149.59:2181";

    private static final String LOCK_NODE = "/LOCK";

    private static final Logger log = LoggerFactory.getLogger(ZookeeperLock.class);

    private CountDownLatch countDownLatch = null;

    private ZkClient client = new ZkClient(ZK_IP_PORT);

    /**
     * 阻塞的方式去获取锁
     */
    @Override
    public void lock() {
        if (tryLock()) {
            log.info("=================get lock success=================");
        } else {
            waitForLock();
            lock();
        }
    }

    /**
     * 挂起 等待锁
     */
    private void waitForLock() {
        //1.加监听器
        IZkDataListener listener = new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {

            }

            @Override
            public void handleDataDeleted(String s) throws Exception {
                //2.当有lock被删除时，停止等待 重新去竞争锁
                log.info("====================catch data delete event====================");
                if (countDownLatch != null) {
                    log.info("====================catch data delete event countDownLatch.countDown()====================");
                    countDownLatch.countDown();
                }
            }
        };
        client.subscribeDataChanges(LOCK_NODE, listener);
        if (client.exists(LOCK_NODE)) {
            countDownLatch = new CountDownLatch(1);
            try {
                //当前线程挂起
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        client.unsubscribeDataChanges(LOCK_NODE, listener);
    }

    /**
     * 通过新建节点的方式尝试加锁 非阻塞
     * @return
     */
    @Override
    public boolean tryLock() {
        try {
            client.createPersistent(LOCK_NODE);
            //加锁成功
            return true;
        } catch (ZkNodeExistsException e) {
            //节点已存在 加锁失败
            return false;
        }
    }

    @Override
    public void unlock() {
        client.delete(LOCK_NODE);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
