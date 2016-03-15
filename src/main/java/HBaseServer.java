import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hbase.async.GetRequest;
import org.hbase.async.HBaseClient;
import org.hbase.async.KeyValue;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HBaseServer extends AbstractVerticle  {
	private final static String TEAMID = "DimlyImpugn";
	private static final String AWSID = "103032155548";
	
	private static HBaseClient client;
	private static HConnection conn;
	private static Configuration conf;
//	private static HTableInterface tweetInterface;
//	private final static Logger logger = Logger.getRootLogger();
	private final static Logger logger = LoggerFactory.getLogger(HBaseServer.class);
	private static String zkAddr = "172.31.22.224"; //DNS
	private static String tableName = "tweet";
//	private static TableName tableTweet = TableName.valueOf(tableName);
//	private static HTableInterface tweetTable;

	@Override
	public void start(Future<Void> fut) {
		// Create a router object.
		initializeConnection();
		Router router = Router.router(vertx);
		router.route("/q2").handler(this::q2);

		// Create the HTTP server and pass the "accept" method to the request
		// handler.
		vertx.createHttpServer().requestHandler(router::accept).listen(
				// Retrieve the port from the configuration,
				config().getInteger("0.0.0.0", 80), result -> {
					if (result.succeeded()) {
						fut.complete();
					} else {
						fut.fail(result.cause());
					}
				});
	}
	
	private void initializeConnection()  {
		logger.setLevel(Level.ERROR);
		conf = HBaseConfiguration.create();
		conf.set("hbase.master", zkAddr + ":60000");
        conf.set("hbase.zookeeper.quorum", zkAddr);
        conf.set("hbase.zookeeper.property.clientport", "2181");
	    if (!zkAddr.matches("\\d+.\\d+.\\d+.\\d+")) {
		    System.out.print("HBase not configured!");
		    return;
	    }
	    
        try {
			conn = HConnectionManager.createConnection(conf);
			System.out.print("Successfully initiate hbClient");
		} catch (IOException e) {
			System.out.println("Exception occurs in conn or hbClient");
			e.printStackTrace();
		}
       
	}

	private void q2(RoutingContext routingContext) {
		try {
			HttpServerRequest req = routingContext.request();
			String userid = req.params().get("userid").trim();
			String hashtag = req.params().get("hashtag").trim();
			String rowKey = userid + hashtag;
			String response = processRequest(rowKey);
			req.response().headers().set("Content-Type", "text/plain");
			req.response().end(response);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private String processRequest(String rowKey) {
		String output = null;
		try {
			GetRequest getRequest = new GetRequest("tweet", rowKey);
			ArrayList<KeyValue> resultSets = client.get(getRequest).joinUninterruptibly();
			StringBuffer sb = new StringBuffer();
			for (KeyValue pair : resultSets) {
				output = Bytes.toString(pair.value()).replaceAll("\\\\n", "\n");
				sb.append(output);
				sb.append("\n");
			}
			sb.append("\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return output;

        // Get a specific value
	}
	
	/*private void processRequest(HttpServerRequest req, asyncHBaseClient hbClient) {
		System.out.print("Begin to process request");

		hbClient.getConnection(); // ?? necessary?
		
		System.out.print("finish hbClient.getConnection()");

		String userid = req.params().get("userid").trim();
		String hashtag = req.params().get("hashtag").trim();
		String rowKey = userid + hashtag;

		byte[] bCol_text = Bytes.toBytes("text");
		byte[] row = Bytes.toBytes(rowKey); 
		
		//GET: based on hbClient
		Get getResult = new Get(row);
		Result result = new Result(); // result is byte[]
		try {
			*//**
			 * Create a connection to the cluster. HConnection connection =
			 * HConnectionManager.createConnection(Configuration);
			 * HTableInterface table = connection.getTable("myTable"); // use
			 * table as needed, the table returned is lightweight table.close();
			 * use the connection for other access to the cluster
			 * connection.close();
			 *//*
			//still use one connection? 
			result = hbClient.get(tableTweet, getResult, hbClient.<Result>newPromise()).get();
			
		} catch (InterruptedException e) {
			System.out.println("InterrupedException occur in hbClient.get()");
			e.printStackTrace();
		} catch (ExecutionException e) {
			System.out.println("ExecutionException occur in hbClient.get()");
			e.printStackTrace();
		}

		String text = Arrays.toString(result.getValue(bColFamily, bCol_text));
		
		System.out.println("text: " + text);
				
		StringBuffer sb = new StringBuffer();
		sb.append(TEAMID).append(",").append(AWSID).append("\n");
		sb.append(text).append("\n");
		
		req.response().end(text);
		//how to close?
	}*/

}
