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
package org.apache.giraph.graph;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.apache.giraph.GiraphConfiguration;
import org.apache.giraph.ImmutableClassesGiraphConfiguration;
import org.apache.giraph.utils.DynamicChannelBufferInputStream;
import org.apache.giraph.utils.DynamicChannelBufferOutputStream;
import org.apache.giraph.utils.SystemTime;
import org.apache.giraph.utils.Time;
import org.apache.giraph.utils.Times;
import org.apache.giraph.utils.UnsafeByteArrayInputStream;
import org.apache.giraph.utils.UnsafeByteArrayOutputStream;
import org.apache.giraph.utils.WritableUtils;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test all the mutable vertices
 */
public class TestMutableVertex {
  /** Number of repetitions */
  public static final int REPS = 100;
  /** Vertex classes to be tested filled in from setup() */
  private Collection<
      Class<? extends Vertex<IntWritable, FloatWritable, DoubleWritable,
      LongWritable>>> vertexClasses = Lists.newArrayList();

  /**
   * Simple instantiable class that extends
   * {@link HashMapVertex}.
   */
  public static class IFDLHashMapVertex extends
      HashMapVertex<IntWritable, FloatWritable, DoubleWritable,
          LongWritable> {
    @Override
    public void compute(Iterable<LongWritable> messages) throws IOException { }
  }

  /**
   * Simple instantiable class that extends
   * {@link EdgeListVertex}.
   */
  public static class IFDLEdgeListVertex extends
      EdgeListVertex<IntWritable, FloatWritable, DoubleWritable,
      LongWritable> {
    @Override
    public void compute(Iterable<LongWritable> messages) throws IOException { }
  }

  /**
   * Simple instantiable class that extends
   * {@link RepresentativeVertex}.
   */
  public static class IFDLRepresentativeVertex extends
      RepresentativeVertex<IntWritable, FloatWritable, DoubleWritable,
                LongWritable> {
    @Override
    public void compute(Iterable<LongWritable> messages) throws IOException { }
  }

  @Before
  public void setUp() {
    vertexClasses.add(IFDLHashMapVertex.class);
    vertexClasses.add(IFDLEdgeListVertex.class);
    vertexClasses.add(IFDLRepresentativeVertex.class);
  }

  @Test
  public void testInstantiate() throws IOException {
    for (Class<? extends Vertex<IntWritable, FloatWritable, DoubleWritable,
        LongWritable>> vertexClass : vertexClasses) {
      testInstantiateVertexClass(vertexClass);
    }
  }

  /**
   * Test a vertex class for instantiation
   *
   * @param vertexClass Vertex class to check
   * @return Instantiated mutable vertex
   */
  private MutableVertex<IntWritable, FloatWritable, DoubleWritable,
      LongWritable> testInstantiateVertexClass(
      Class<? extends Vertex<IntWritable, FloatWritable, DoubleWritable,
      LongWritable>> vertexClass) {
    GiraphConfiguration giraphConfiguration = new GiraphConfiguration();
    giraphConfiguration.setVertexClass(vertexClass);
    ImmutableClassesGiraphConfiguration immutableClassesGiraphConfiguration =
        new ImmutableClassesGiraphConfiguration(giraphConfiguration);
    MutableVertex<IntWritable, FloatWritable, DoubleWritable,
        LongWritable> vertex =
        (MutableVertex<IntWritable, FloatWritable,
            DoubleWritable, LongWritable>)
            immutableClassesGiraphConfiguration.createVertex();
    assertNotNull(vertex);
    return vertex;
  }

  @Test
  public void testEdges() {
    for (Class<? extends Vertex<IntWritable, FloatWritable, DoubleWritable,
        LongWritable>> vertexClass : vertexClasses) {
      testEdgesVertexClass(vertexClass);
    }
  }

  /**
   * Test a vertex class for edges
   *
   * @param vertexClass Vertex class to check
   */
  private void testEdgesVertexClass(Class<? extends Vertex<IntWritable,
      FloatWritable, DoubleWritable, LongWritable>> vertexClass) {
    MutableVertex<IntWritable,
        FloatWritable, DoubleWritable, LongWritable> vertex =
        testInstantiateVertexClass(vertexClass);

    Map<IntWritable, DoubleWritable> edgeMap = Maps.newHashMap();
    for (int i = 1000; i > 0; --i) {
      edgeMap.put(new IntWritable(i), new DoubleWritable(i * 2.0));
    }
    vertex.initialize(null, null, edgeMap);
    assertEquals(vertex.getNumEdges(), 1000);
    for (Edge<IntWritable, DoubleWritable> edge : vertex.getEdges()) {
      assertEquals(edge.getValue().get(),
          edge.getTargetVertexId().get() * 2.0d, 0d);
    }
    assertEquals(vertex.removeEdge(new IntWritable(500)),
        new DoubleWritable(1000));
    assertEquals(vertex.getNumEdges(), 999);
  }

  @Test
  public void testGetEdges() {
    for (Class<? extends Vertex<IntWritable, FloatWritable, DoubleWritable,
        LongWritable>> vertexClass : vertexClasses) {
      testGetEdgesVertexClass(vertexClass);
    }
  }

  /**
   * Test a vertex class for getting edges
   *
   * @param vertexClass Vertex class to check
   */
  private void testGetEdgesVertexClass(Class<? extends Vertex<IntWritable,
      FloatWritable, DoubleWritable, LongWritable>> vertexClass) {
    MutableVertex<IntWritable,
        FloatWritable, DoubleWritable, LongWritable> vertex =
        testInstantiateVertexClass(vertexClass);

    Map<IntWritable, DoubleWritable> edgeMap = Maps.newHashMap();
    for (int i = 1000; i > 0; --i) {
      edgeMap.put(new IntWritable(i), new DoubleWritable(i * 3.0));
    }
    vertex.initialize(null, null, edgeMap);
    assertEquals(vertex.getNumEdges(), 1000);
    assertEquals(vertex.getEdgeValue(new IntWritable(600)),
        new DoubleWritable(600 * 3.0));
    assertEquals(vertex.removeEdge(new IntWritable(600)),
        new DoubleWritable(600 * 3.0));
    assertEquals(vertex.getNumEdges(), 999);
    assertEquals(vertex.getEdgeValue(new IntWritable(500)),
        new DoubleWritable(500 * 3.0));
    assertEquals(vertex.getEdgeValue(new IntWritable(700)),
        new DoubleWritable(700 * 3.0));
  }

  @Test
  public void testAddRemoveEdges() {
    for (Class<? extends Vertex<IntWritable, FloatWritable, DoubleWritable,
        LongWritable>> vertexClass : vertexClasses) {
      testAddRemoveEdgesVertexClass(vertexClass);
    }
  }

  /**
   * Test a vertex class for adding/removing edges
   *
   * @param vertexClass Vertex class to check
   */
  private void testAddRemoveEdgesVertexClass(Class<? extends
      Vertex<IntWritable, FloatWritable, DoubleWritable,
          LongWritable>> vertexClass) {
    MutableVertex<IntWritable,
        FloatWritable, DoubleWritable, LongWritable> vertex =
        testInstantiateVertexClass(vertexClass);

    Map<IntWritable, DoubleWritable> edgeMap = Maps.newHashMap();
    vertex.initialize(null, null, edgeMap);
    assertEquals(vertex.getNumEdges(), 0);
    assertTrue(vertex.addEdge(new IntWritable(2),
        new DoubleWritable(2.0)));
    assertEquals(vertex.getNumEdges(), 1);
    assertEquals(vertex.getEdgeValue(new IntWritable(2)),
        new DoubleWritable(2.0));
    assertTrue(vertex.addEdge(new IntWritable(4),
        new DoubleWritable(4.0)));
    assertTrue(vertex.addEdge(new IntWritable(3),
        new DoubleWritable(3.0)));
    assertTrue(vertex.addEdge(new IntWritable(1),
        new DoubleWritable(1.0)));
    assertEquals(vertex.getNumEdges(), 4);
    assertNull(vertex.getEdgeValue(new IntWritable(5)));
    assertNull(vertex.getEdgeValue(new IntWritable(0)));
    for (Edge<IntWritable, DoubleWritable> edge : vertex.getEdges()) {
      assertEquals(edge.getTargetVertexId().get() * 1.0d,
          edge.getValue().get(), 0d);
    }
    assertNotNull(vertex.removeEdge(new IntWritable(1)));
    assertEquals(vertex.getNumEdges(), 3);
    assertNotNull(vertex.removeEdge(new IntWritable(3)));
    assertEquals(vertex.getNumEdges(), 2);
    assertNotNull(vertex.removeEdge(new IntWritable(2)));
    assertEquals(vertex.getNumEdges(), 1);
    assertNotNull(vertex.removeEdge(new IntWritable(4)));
    assertEquals(vertex.getNumEdges(), 0);
  }

  @Test
  public void testSerialized() throws IOException {
    for (Class<? extends Vertex<IntWritable, FloatWritable, DoubleWritable,
        LongWritable>> vertexClass : vertexClasses) {
      testSerializeVertexClass(vertexClass);
      testDynamicChannelBufferSerializeVertexClass(vertexClass);
      testUnsafeSerializeVertexClass(vertexClass);
    }
  }

  /**
   * Build a vertex for testing
   *
   * @param vertexClass Vertex class to use for testing
   * @return Vertex that has some initial data
   */
  private MutableVertex<IntWritable,
      FloatWritable, DoubleWritable, LongWritable> buildVertex(Class<? extends
      Vertex<IntWritable, FloatWritable, DoubleWritable,
          LongWritable>> vertexClass) {
    MutableVertex<IntWritable,
        FloatWritable, DoubleWritable, LongWritable> vertex =
        testInstantiateVertexClass(vertexClass);

    final int edgesCount = 200;
    Map<IntWritable, DoubleWritable> edgeMap = Maps.newHashMap();
    for (int i = edgesCount; i > 0; --i) {
      edgeMap.put(new IntWritable(i), new DoubleWritable(i * 2.0));
    }
    vertex.initialize(new IntWritable(2), new FloatWritable(3.0f), edgeMap);
    return vertex;
  }

  /**
   * Test a vertex class for serializing
   *
   * @param vertexClass Vertex class to check
   */
  private void testSerializeVertexClass(Class<? extends
      Vertex<IntWritable, FloatWritable, DoubleWritable,
          LongWritable>> vertexClass) {
    MutableVertex<IntWritable,
        FloatWritable, DoubleWritable, LongWritable> vertex =
        buildVertex(vertexClass);

    long serializeNanosStart = 0;
    long serializeNanos = 0;
    byte[] byteArray = null;
    for (int i = 0; i < REPS; ++i) {
      serializeNanosStart = SystemTime.getInstance().getNanoseconds();
      byteArray = WritableUtils.writeToByteArray(vertex);
      serializeNanos += Times.getNanosecondsSince(SystemTime.getInstance(),
          serializeNanosStart);
    }
    serializeNanos /= REPS;
    System.out.println("testSerialize: Serializing took " +
        serializeNanos +
        " ns for " + byteArray.length + " bytes " +
        (byteArray.length * 1f * Time.NS_PER_SECOND / serializeNanos) +
        " bytes / sec for " + vertexClass.getName());

    MutableVertex<IntWritable,
        FloatWritable, DoubleWritable, LongWritable> readVertex =
        testInstantiateVertexClass(vertexClass);

    long deserializeNanosStart = 0;
    long deserializeNanos = 0;
    for (int i = 0; i < REPS; ++i) {
      deserializeNanosStart = SystemTime.getInstance().getNanoseconds();
      WritableUtils.readFieldsFromByteArray(byteArray, readVertex);
      deserializeNanos += Times.getNanosecondsSince(SystemTime.getInstance(),
          deserializeNanosStart);
    }
    deserializeNanos /= REPS;
    System.out.println("testSerialize: " +
        "Deserializing " +
        "took " +
        deserializeNanos +
        " ns for " + byteArray.length + " bytes " +
        (byteArray.length * 1f * Time.NS_PER_SECOND / deserializeNanos) +
        " bytes / sec for " + vertexClass.getName());

    assertEquals(vertex.getId(), readVertex.getId());
    assertEquals(vertex.getValue(), readVertex.getValue());
    assertEquals(Lists.newArrayList(vertex.getEdges()),
        Lists.newArrayList(readVertex.getEdges()));
  }

  /**
   * Test a vertex class for serializing with DynamicChannelBuffers
   *
   * @param vertexClass Vertex class to check
   */
  private void testDynamicChannelBufferSerializeVertexClass(Class<? extends
      Vertex<IntWritable, FloatWritable, DoubleWritable,
          LongWritable>> vertexClass) throws IOException {
    MutableVertex<IntWritable,
        FloatWritable, DoubleWritable, LongWritable> vertex =
        buildVertex(vertexClass);

    long serializeNanosStart = 0;
    long serializeNanos = 0;
    DynamicChannelBufferOutputStream outputStream = null;
    for (int i = 0; i <
        REPS; ++i) {
      serializeNanosStart = SystemTime.getInstance().getNanoseconds();
      outputStream =
          new DynamicChannelBufferOutputStream(32);
      vertex.write(outputStream);
      serializeNanos += Times.getNanosecondsSince(SystemTime.getInstance(),
          serializeNanosStart);
    }
    serializeNanos /= REPS;
    System.out.println("testDynamicChannelBufferSerializeVertexClass: " +
        "Serializing took " +
        serializeNanos +
        " ns for " + outputStream.getDynamicChannelBuffer().writerIndex()
        + " bytes " +
        (outputStream.getDynamicChannelBuffer().writerIndex() * 1f *
            Time.NS_PER_SECOND / serializeNanos) +
        " bytes / sec for " + vertexClass.getName());

    MutableVertex<IntWritable,
        FloatWritable, DoubleWritable, LongWritable> readVertex =
        testInstantiateVertexClass(vertexClass);

    long deserializeNanosStart = 0;
    long deserializeNanos = 0;
    for (int i = 0; i < REPS; ++i) {
      deserializeNanosStart = SystemTime.getInstance().getNanoseconds();
      DynamicChannelBufferInputStream inputStream = new
          DynamicChannelBufferInputStream(
          outputStream.getDynamicChannelBuffer());
      readVertex.readFields(inputStream);
      deserializeNanos += Times.getNanosecondsSince(SystemTime.getInstance(),
          deserializeNanosStart);
      outputStream.getDynamicChannelBuffer().readerIndex(0);
    }
    deserializeNanos /= REPS;
    System.out.println("testDynamicChannelBufferSerializeVertexClass: " +
        "Deserializing took " +
        deserializeNanos +
        " ns for " + outputStream.getDynamicChannelBuffer().writerIndex() +
        " bytes " +
        (outputStream.getDynamicChannelBuffer().writerIndex() * 1f *
            Time.NS_PER_SECOND / deserializeNanos) +
        " bytes / sec for " + vertexClass.getName());

    assertEquals(vertex.getId(), readVertex.getId());
    assertEquals(vertex.getValue(), readVertex.getValue());
    assertEquals(Lists.newArrayList(vertex.getEdges()),
        Lists.newArrayList(readVertex.getEdges()));
  }


  /**
   * Test a vertex class for serializing with UnsafeByteArray(Input/Output)
   * Stream
   *
   * @param vertexClass Vertex class to check
   */
  private void testUnsafeSerializeVertexClass(Class<? extends
      Vertex<IntWritable, FloatWritable, DoubleWritable,
          LongWritable>> vertexClass) throws IOException {
    MutableVertex<IntWritable,
        FloatWritable, DoubleWritable, LongWritable> vertex =
        buildVertex(vertexClass);

    long serializeNanosStart = 0;
    long serializeNanos = 0;
    UnsafeByteArrayOutputStream outputStream = null;
    for (int i = 0; i <
        REPS; ++i) {
      serializeNanosStart = SystemTime.getInstance().getNanoseconds();
      outputStream =
          new UnsafeByteArrayOutputStream(32);
      vertex.write(outputStream);
      serializeNanos += Times.getNanosecondsSince(SystemTime.getInstance(),
          serializeNanosStart);
    }
    serializeNanos /= REPS;
    System.out.println("testUnsafeSerializeVertexClass: " +
        "Serializing took " +
        serializeNanos +
        " ns for " + outputStream.getPos()
        + " bytes " +
        (outputStream.getPos() * 1f *
            Time.NS_PER_SECOND / serializeNanos) +
        " bytes / sec for " + vertexClass.getName());

    MutableVertex<IntWritable,
        FloatWritable, DoubleWritable, LongWritable> readVertex =
        testInstantiateVertexClass(vertexClass);

    long deserializeNanosStart = 0;
    long deserializeNanos = 0;
    for (int i = 0; i < REPS; ++i) {
      deserializeNanosStart = SystemTime.getInstance().getNanoseconds();
      UnsafeByteArrayInputStream inputStream = new
          UnsafeByteArrayInputStream(
          outputStream.getByteArray(), 0, outputStream.getPos());
      readVertex.readFields(inputStream);
      deserializeNanos += Times.getNanosecondsSince(SystemTime.getInstance(),
          deserializeNanosStart);
    }
    deserializeNanos /= REPS;
    System.out.println("testUnsafeSerializeVertexClass: " +
        "Deserializing took " +
        deserializeNanos +
        " ns for " + outputStream.getPos() +
        " bytes " +
        (outputStream.getPos() * 1f *
            Time.NS_PER_SECOND / deserializeNanos) +
        " bytes / sec for " + vertexClass.getName());

    assertEquals(vertex.getId(), readVertex.getId());
    assertEquals(vertex.getValue(), readVertex.getValue());
    assertEquals(Lists.newArrayList(vertex.getEdges()),
        Lists.newArrayList(readVertex.getEdges()));
  }
}
