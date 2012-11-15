/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.giraph.comm;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collection;
import org.apache.giraph.GiraphConfiguration;
import org.apache.giraph.ImmutableClassesGiraphConfiguration;
import org.apache.giraph.comm.netty.NettyClient;
import org.apache.giraph.comm.netty.NettyServer;
import org.apache.giraph.comm.netty.handler.WorkerRequestServerHandler;
import org.apache.giraph.comm.requests.SendWorkerMessagesRequest;
import org.apache.giraph.comm.requests.WritableRequest;
import org.apache.giraph.graph.EdgeListVertex;
import org.apache.giraph.graph.WorkerInfo;
import org.apache.giraph.utils.ByteArrayVertexIdMessageCollection;
import org.apache.giraph.utils.MockUtils;
import org.apache.giraph.utils.PairList;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test all the netty failure scenarios
 */
public class RequestFailureTest {
  /** Configuration */
  private ImmutableClassesGiraphConfiguration conf;
  /** Server data */
  private ServerData<IntWritable, IntWritable, IntWritable, IntWritable>
  serverData;
  /** Server */
  private NettyServer server;
  /** Client */
  private NettyClient client;
  /** Mock context */
  private Context context;

  /**
   * Only for testing.
   */
  public static class TestVertex extends EdgeListVertex<IntWritable,
      IntWritable, IntWritable, IntWritable> {
    @Override
    public void compute(Iterable<IntWritable> messages) throws IOException {
    }
  }

  @Before
  public void setUp() throws IOException {
    // Setup the conf
    GiraphConfiguration tmpConf = new GiraphConfiguration();
    tmpConf.setVertexClass(TestVertex.class);
    conf = new ImmutableClassesGiraphConfiguration(tmpConf);

    context = mock(Context.class);
    when(context.getConfiguration()).thenReturn(conf);
  }

  private WritableRequest getRequest() {
    // Data to send
    final int partitionId = 0;
    PairList<Integer, ByteArrayVertexIdMessageCollection<IntWritable,
            IntWritable>>
        dataToSend = new PairList<Integer,
        ByteArrayVertexIdMessageCollection<IntWritable, IntWritable>>();
    dataToSend.initialize();
    ByteArrayVertexIdMessageCollection<IntWritable,
        IntWritable> vertexIdMessages =
        new ByteArrayVertexIdMessageCollection<IntWritable, IntWritable>();
    vertexIdMessages.setConf(conf);
    vertexIdMessages.initialize();
    dataToSend.add(partitionId, vertexIdMessages);
    for (int i = 1; i < 7; ++i) {
      IntWritable vertexId = new IntWritable(i);
      for (int j = 0; j < i; ++j) {
        vertexIdMessages.add(vertexId, new IntWritable(j));
      }
    }

    // Send the request
    SendWorkerMessagesRequest<IntWritable, IntWritable, IntWritable,
            IntWritable> request =
        new SendWorkerMessagesRequest<IntWritable, IntWritable,
                    IntWritable, IntWritable>(dataToSend);
    return request;
  }

  private void checkResult(int numRequests) throws IOException {
    // Check the output
    Iterable<IntWritable> vertices =
        serverData.getIncomingMessageStore().getDestinationVertices();
    int keySum = 0;
    int messageSum = 0;
    for (IntWritable vertexId : vertices) {
      keySum += vertexId.get();
      Collection<IntWritable> messages =
          serverData.getIncomingMessageStore().getVertexMessages(vertexId);
      synchronized (messages) {
        for (IntWritable message : messages) {
          messageSum += message.get();
        }
      }
    }
    assertEquals(21, keySum);
    assertEquals(35 * numRequests, messageSum);
  }

  @Test
  public void send2Requests() throws IOException {
    checkSendingTwoRequests();
  }

  @Test
  public void alreadyProcessedRequest() throws IOException {
    // Force a drop of the first request
    conf.setBoolean(GiraphConfiguration.NETTY_SIMULATE_FIRST_RESPONSE_FAILED, true);
    // One second to finish a request
    conf.setInt(GiraphConfiguration.MAX_REQUEST_MILLISECONDS, 1000);
    // Loop every 2 seconds
    conf.setInt(GiraphConfiguration.WAITING_REQUEST_MSECS, 2000);

    checkSendingTwoRequests();
  }

  @Test
  public void resendRequest() throws IOException {
    // Force a drop of the first request
    conf.setBoolean(GiraphConfiguration.NETTY_SIMULATE_FIRST_REQUEST_CLOSED, true);
    // One second to finish a request
    conf.setInt(GiraphConfiguration.MAX_REQUEST_MILLISECONDS, 1000);
    // Loop every 2 seconds
    conf.setInt(GiraphConfiguration.WAITING_REQUEST_MSECS, 2000);

    checkSendingTwoRequests();
  }

  private void checkSendingTwoRequests() throws IOException {
    // Start the service
    serverData = MockUtils.createNewServerData(conf, context);
    WorkerInfo workerInfo = new WorkerInfo(-1);
    server = new NettyServer(conf,
        new WorkerRequestServerHandler.Factory(serverData), workerInfo);
    server.start();
    workerInfo.setInetSocketAddress(server.getMyAddress());
    client = new NettyClient(context, conf, new WorkerInfo());
    client.connectAllAddresses(
        Lists.<WorkerInfo>newArrayList(workerInfo));

    // Send the request 2x, but should only be processed once
    WritableRequest request1 = getRequest();
    WritableRequest request2 = getRequest();
    client.sendWritableRequest(workerInfo.getTaskId(), request1);
    client.sendWritableRequest(workerInfo.getTaskId(), request2);
    client.waitAllRequests();

    // Stop the service
    client.stop();
    server.stop();

    // Check the output (should have been only processed once)
    checkResult(2);
  }
}
