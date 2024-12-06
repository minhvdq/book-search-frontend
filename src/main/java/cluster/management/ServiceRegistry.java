package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ServiceRegistry implements Watcher {
    private ZooKeeper zooKeeper;
    private final String REGISTRY_NAMESPACE;

    public static final String WORKERS_REGISTRY_ZNODE = "/registry_znode";
    public static final String COORDINATOR_REGISTRY_ZNODE = "/coordinator_registry_znode";
    List<String> allAddresses;

    private String currentZnode = null;
    public ServiceRegistry(ZooKeeper zooKeeper, String str) throws InterruptedException, KeeperException {
        this.zooKeeper = zooKeeper;
        this.REGISTRY_NAMESPACE = str;
        createRegistyZnode();
    }
    private void createRegistyZnode() throws InterruptedException, KeeperException {
        Stat registryznode = zooKeeper.exists(REGISTRY_NAMESPACE, false);
        if(registryznode == null){
            zooKeeper.create(REGISTRY_NAMESPACE, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    public void registerToCluster(String metadata) throws InterruptedException, KeeperException {
        if( currentZnode != null ){
            return;
        }
        byte[] dataInByte = metadata.getBytes();
        String znodeModel = REGISTRY_NAMESPACE + "/n_";
        this.currentZnode = zooKeeper.create(znodeModel, dataInByte, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Registered to cluster");
    }

    public void unregisterFromCluster() {
        try {
            if (currentZnode != null && zooKeeper.exists(currentZnode, false) != null) {
                zooKeeper.delete(currentZnode, -1);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        }
    }
    public void registerToUpdate(){
        try{
            updateAddresses();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateAddresses() throws InterruptedException, KeeperException {
        List<String> addresses = new ArrayList<>();
        List<String> children = zooKeeper.getChildren(REGISTRY_NAMESPACE, this);
        for(String regChild : children){
            String znodeFullPath = REGISTRY_NAMESPACE + "/" + regChild;
            Stat stat = zooKeeper.exists(znodeFullPath, false);
            if(stat == null){
                continue;
            }
            byte[] data = zooKeeper.getData(znodeFullPath,false, stat);
            String address = new String(data);
            addresses.add(address);
        }
        this.allAddresses = Collections.unmodifiableList(addresses);
        System.out.println("All the addresses: " + allAddresses);
    }

    public List<String> getAllAddresses() throws InterruptedException, KeeperException {
        if(allAddresses == null){
            updateAddresses();
        }
        return allAddresses;
    }

    public String getRandomAddress(){
        if(allAddresses == null){
            try{
                updateAddresses();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (KeeperException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("having " + allAddresses.size() + " addresses in cluster");
        Random random = new Random();
        int rand = random.nextInt(allAddresses.size() );
        return allAddresses.get(rand);
    }

    @Override
    public void process(WatchedEvent event) {
        try{
            updateAddresses();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        }
    }
}
