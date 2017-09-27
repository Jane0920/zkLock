package com.xyr.zkLock;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Created by xyr on 2017/9/27.
 */
public class ZookeeperLockImprove implements Lock {

    private static final String ZK_IP_PORT = "106.14.149.59:2181";

    private static final String LOCK_NODE = "/lock";

    private static final Logger log = LoggerFactory.getLogger(ZookeeperLockImprove.class);

    private CountDownLatch countDownLatch = null;

    private ZkClient client = new ZkClient(ZK_IP_PORT);

    private String beforePath; //当前创建的节点的前面那个节点
    private String currentPath;

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
                    countDownLatch.countDown();
                }
            }
        };
        client.subscribeDataChanges(beforePath, listener);
        if (client.exists(beforePath)) {
            countDownLatch = new CountDownLatch(1);
            try {
                //当前线程挂起
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        client.unsubscribeDataChanges(beforePath, listener);
    }

    /**
     * 通过新建节点的方式尝试加锁 非阻塞
     * @return
     */
    @Override
    public boolean tryLock() {
        try {
            //创建临时顺序节点
            if (currentPath == null || currentPath.length() <= 0) {
                currentPath = client.createEphemeralSequential(LOCK_NODE + "/", "lock");
                System.out.println("------------------------------" + currentPath);
            }
            //获取子节点列表
            List<String> childrens = client.getChildren(LOCK_NODE);
            Collections.sort(childrens);
            //如果当前节点为所有节点中最小的节点，说明该节点得到了锁
            if (currentPath.equals(LOCK_NODE + "/" + childrens.get(0))) {
                return true;
            } else { //否则说明没有获取到锁，那么就获取当前节点前面的那个节点，赋值给beforePath用于监听
                int index = Collections.binarySearch(childrens, currentPath.substring(6)); //获取当前节点的位置
                beforePath = LOCK_NODE + "/" + childrens.get(index - 1);
            }
            return false;
        } catch (ZkNodeExistsException e) {
            //节点已存在 加锁失败
            return false;
        }
    }

    @Override
    public void unlock() {
        client.delete(currentPath);
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
