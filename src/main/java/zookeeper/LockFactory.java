package zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-8-25
 * Time: 下午3:18
 * To change this template use File | Settings | File Templates.
 */
public class LockFactory {

    public static final ZooKeeper DEFAULT_ZOOKEEPER = getDefaultZookeeper();
    //data格式:  ip:stat  如: 10.232.35.70:lock 10.232.35.70:unlock
    public static synchronized Lock getLock(String path,String ip) throws Exception{
        if(DEFAULT_ZOOKEEPER != null){
            Stat stat = null;
            try{
                stat = DEFAULT_ZOOKEEPER.exists(path, true);
            }catch (Exception e) {
                // TODO: use log system and throw new exception
            }
            if(stat!=null){
                byte[] data = DEFAULT_ZOOKEEPER.getData(path, null, stat);
                String dataStr = new String(data);
                String[] ipv = dataStr.split(":");
                if(ip.equals(ipv[0])){
                    Lock lock = new Lock(path);
                    lock.setZooKeeper(DEFAULT_ZOOKEEPER);
                    return lock;
                }
                //is not your lock, return null
                else{
                    return null;
                }
            }
            //no lock created yet, you can get it
            else{
                createZnode(path);
                Lock lock = new Lock(path);
                lock.setZooKeeper(DEFAULT_ZOOKEEPER);
                return lock;
            }
        }
        return null;
    }

    private static ZooKeeper getDefaultZookeeper() {
        try {
            ZooKeeper zooKeeper = new ZooKeeper("testserver", 10*1000, new Watcher(){
                public void process(WatchedEvent event) {
                    //节点的事件处理. you can do something when the node's data change
                  System.out.println("event " + event.getType() + " has happened!");
                }
            });
            return zooKeeper;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void createZnode(String path) throws Exception{

        if(DEFAULT_ZOOKEEPER!=null){
            InetAddress address = InetAddress.getLocalHost();
            String data = address.getHostAddress()+":unlock";
            DEFAULT_ZOOKEEPER.create(path, data.getBytes(), Collections.singletonList(
                    new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.ANYONE_ID_UNSAFE)) ,
                    CreateMode.EPHEMERAL);
        }
    }
    public static void main(String[] args) throws Exception {
        Lock lock1  = getLock("/test1","localhost");
        System.out.println(lock1);
        Lock lock2  = getLock("/test1","localhost");
        System.out.println(lock2);
        Lock lock3  = getLock("/test2","localhost");
        System.out.println(lock3);
    }

}