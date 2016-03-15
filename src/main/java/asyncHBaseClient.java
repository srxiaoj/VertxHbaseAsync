//
//import com.google.protobuf.RpcCallback;
//import com.google.protobuf.RpcChannel;
//
//import mousio.hbase.async.HBaseResponsePromise;
//
//import org.apache.hadoop.hbase.HConstants;
//import org.apache.hadoop.hbase.HRegionLocation;
//import org.apache.hadoop.hbase.TableName;
//import org.apache.hadoop.hbase.client.*;
//import org.apache.hadoop.hbase.ipc.AsyncPayloadCarryingRpcController;
//import org.apache.hadoop.hbase.ipc.AsyncRpcClient;
//import org.apache.hadoop.hbase.protobuf.generated.ClientProtos;
//import org.apache.hadoop.hbase.security.User;
//
//import java.io.Closeable;
//import java.io.IOException;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.apache.hadoop.hbase.protobuf.ProtobufUtil.toResult;
//import static org.apache.hadoop.hbase.protobuf.RequestConverter.buildGetRequest;
//
///**
// * Hbase client.
// */
//public class asyncHBaseClient extends AbstractHBaseClient implements Closeable {
//  private final AsyncRpcClient client;
//  private final int rpcTimeout;
//
//  /**
//   * Constructor
//   *
//   * @param connection to Hbase
//   * @throws java.io.IOException if HConnection could not be set up
//   */
//  public asyncHBaseClient(HConnection connection) throws IOException {
//    super(connection);
//
//    this.rpcTimeout = connection.getConfiguration().getInt(HConstants.HBASE_RPC_TIMEOUT_KEY,
//        HConstants.DEFAULT_HBASE_RPC_TIMEOUT);
//
//    this.client = new AsyncRpcClient(connection.getConfiguration(), this.clusterId, null);
//    
//    //just for one request or for all request?
//    
//  }
//  
//  /**
//   * From cc
//   * How to connect hbaseClient with connectionFactory
//   */
//
//  /**
//   * Send a Get
//   *
//   * @param table   to run get on
//   * @param get     to fetch
//   * @param handler on response
//   * @return the handler with the result
//   */
//  public <H extends ResponseHandler<Result>> H get(TableName table, Get get, final H handler) {
//    try {
//      HRegionLocation location = getRegionLocation(table, get.getRow(), false);
//
//      this.getClientService(location).get(
//          getNewRpcController(handler),
//          buildGetRequest(
//              location.getRegionInfo().getRegionName(),
//              get
//          ),
//          new RpcCallback<ClientProtos.GetResponse>() {
//            @Override public void run(ClientProtos.GetResponse response) {
//              handler.onSuccess(toResult(response.getResult()));
//            }
//          }
//      );
//    } catch (IOException e) {
//      handler.onFailure(e);
//    }
//    return handler;
//  }
//
//  /**
//   * Send a Get
//   *
//   * @param table   to run get on
//   * @param gets    to fetch
//   * @param handler on response
//   * @return the handler with the result
//   */
//  public <H extends ResponseHandler<Result[]>> H get(TableName table, List<Get> gets, final H handler) {
//    final Result[] results = new Result[gets.size()];
//    final AtomicInteger counter = new AtomicInteger(0);
//    final AtomicBoolean complete = new AtomicBoolean(false);
//
//    for (int i = 0; i < gets.size(); i++) {
//      this.get(table, gets.get(i), new ResultListener<Result>(i) {
//        @Override public void onSuccess(Result response) {
//          if (!complete.get()) {
//            synchronized (results) {
//              results[this.index] = response;
//              if (counter.incrementAndGet() == results.length) {
//                handler.onSuccess(results);
//              }
//            }
//          }
//        }
//
//        @Override public void onFailure(IOException e) {
//          if (!complete.get()) {
//            handler.onFailure(e);
//            complete.set(true);
//          }
//        }
//      });
//    }
//
//    return handler;
//  }
//  
//
//  /**
//   * Get a new promise chained to event loop of internal netty client
//   *
//   * @param <T> Type of response to return
//   * @return Hbase Response Promise
//   */
//  public <T> HBaseResponsePromise<T> newPromise() {
//    return new HBaseResponsePromise<>(client.getEventLoop());
//  }
//
//
//  public ClientProtos.ClientService.Interface getClientService(HRegionLocation location) throws
//      IOException {
//    return ClientProtos.ClientService.newStub(
//        client.createRpcChannel(
//            location.getServerName(),
//            User.getCurrent(),
//            rpcTimeout)
//    );
//
//  }
//
//  /**
//   * Creates and returns a {@link com.google.protobuf.RpcChannel} instance connected to the
//   * table region containing the specified row.  The row given does not actually have
//   * to exist.  Whichever region would contain the row based on start and end keys will
//   * be used.  Note that the {@code row} parameter is also not passed to the
//   * coprocessor handler registered for this protocol, unless the {@code row}
//   * is separately passed as an argument in the service request.  The parameter
//   * here is only used to locate the region used to handle the call.
//   * <p/>
//   * <p>
//   * The obtained {@link com.google.protobuf.RpcChannel} instance can be used to access a published
//   * coprocessor {@link com.google.protobuf.Service} using standard protobuf service invocations:
//   * </p>
//   * <p/>
//   * <div style="background-color: #cccccc; padding: 2px">
//   * <blockquote><pre>
//   * CoprocessorRpcChannel channel = myTable.coprocessorService(rowkey);
//   * MyService.BlockingInterface service = MyService.newBlockingStub(channel);
//   * MyCallRequest request = MyCallRequest.newBuilder()
//   *     ...
//   *     .build();
//   * MyCallResponse response = service.myCall(null, request);
//   * </pre></blockquote></div>
//   *
//   * @param table to get service from
//   * @param row   The row key used to identify the remote region location
//   * @return A CoprocessorRpcChannel instance
//   * @throws java.io.IOException when there was an error creating connection or getting location
//   */
//  //where to use? 
//  public RpcChannel coprocessorService(TableName table, byte[] row) throws IOException {
//    HRegionLocation location = getRegionLocation(table, row, false);
//
//    return client.createRpcChannel(location.getServerName(), User.getCurrent(), rpcTimeout);
//  }
//
//
//  @Override public void close() throws IOException {
//    client.close();
//  }
//
//  /**
//   * Get a new Rpc controller
//   *
//   * @param handler to handle result
//   * @return new RpcController
//   */
//  public <H extends ResponseHandler<?>> AsyncPayloadCarryingRpcController getNewRpcController
//      (final H handler) {
//    AsyncPayloadCarryingRpcController controller = new AsyncPayloadCarryingRpcController();
//    controller.notifyOnFail(new RpcCallback<IOException>() {
//      @Override
//      public void run(IOException e) {
//        handler.onFailure(e);
//      }
//    });
//    return controller;
//  }
//
//  /**
//   * Listens for results with an index
//   *
//   * @param <R> Type of result listened to
//   */
//  private abstract class ResultListener<R> implements ResponseHandler<R> {
//
//    protected int index;
//    /**
//     * Constructor
//     *
//     * @param i index for result
//     */
//    public ResultListener(int i) {
//      this.index = i;
//    }
//
//  }
//  
//  /**
//   * Get region location
//   * @param table to get location of
//   * @param row to get location of
//   * @param reload true to not use cached location
//   * @return HRegionLocation
//   * @throws IOException if location fetch fails
//   */
//  public HRegionLocation getRegionLocation(TableName table, byte[] row, boolean reload)
//      throws IOException {
//    return getConnection().getRegionLocation(table,row,reload);
//  }
//}