import cluster.management.ServiceRegistry;
import networking.WebServer;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public class Application implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private ZooKeeper zooKeeper;
    private static ServiceRegistry coordinatorSR;
    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        int currentPort = args.length == 1 ? Integer.parseInt(args[0]) : 9000;
        Application application = new Application();

        ZooKeeper zooKeeper = application.connectToZookeeper();
        ServiceRegistry coordinatorSR = new ServiceRegistry(zooKeeper, ServiceRegistry.COORDINATOR_REGISTRY_ZNODE);
        SearchHandler searchHandler = new SearchHandler(coordinatorSR);
        WebServer server = new WebServer(searchHandler, currentPort);
        server.startServer();
        System.out.println("Server is running on port " + currentPort);

        application.run();
        application.close();
    }

    public ZooKeeper connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
        return zooKeeper;
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper){
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()){
            case None:
                if(event.getState() == Event.KeeperState.SyncConnected){
                    System.out.println("connected to ZooKeeper successfully !");
                }
                else{
                    System.out.println("Disconnected from Zookeeper");
                    zooKeeper.notifyAll();
                }
                break;
        }
    }
}
