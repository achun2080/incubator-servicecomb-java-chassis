/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.qps;

import static org.junit.Assert.fail;

import org.apache.servicecomb.core.Const;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.core.definition.OperationMeta;
import org.apache.servicecomb.foundation.test.scaffolding.config.ArchaiusUtils;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;

public class TestProviderQpsFlowControlHandler {
  ProviderQpsFlowControlHandler handler = new ProviderQpsFlowControlHandler();

  Invocation invocation = Mockito.mock(Invocation.class);

  AsyncResponse asyncResp = Mockito.mock(AsyncResponse.class);

  OperationMeta operationMeta = Mockito.mock(OperationMeta.class);

  @Before
  public void setUP() {
    ArchaiusUtils.resetConfig();
    QpsControllerManagerTest.clearState(ProviderQpsFlowControlHandler.qpsControllerMgr);
    Utils.updateProperty(Config.PROVIDER_LIMIT_KEY_PREFIX + "test", 1);
  }


  @After
  public void afterTest() {
    ArchaiusUtils.resetConfig();
    QpsControllerManagerTest.clearState(ProviderQpsFlowControlHandler.qpsControllerMgr);
  }

  @Test
  public void testGlobalQpsControl(final @Injectable Invocation invocation,
      final @Injectable AsyncResponse asyncResp) throws Exception {
    new Expectations() {
      {
        invocation.getContext(Const.SRC_MICROSERVICE);
        result = "test";
        invocation.getOperationMeta();
        result = QpsControllerManagerTest.getMockOperationMeta("pojo", "server", "opr");
        asyncResp.producerFail((Throwable) any);
        result = new RuntimeException("test error");
      }
    };

    ProviderQpsFlowControlHandler gHandler = new ProviderQpsFlowControlHandler();
    gHandler.handle(invocation, asyncResp);

    Utils.updateProperty(Config.PROVIDER_LIMIT_KEY_GLOBAL, 3);

    int count = 1;
    try {
      gHandler.handle(invocation, asyncResp);
      count++;
      gHandler.handle(invocation, asyncResp);
      fail("An exception is expected!");
    } catch (Exception e) {
      Assert.assertEquals(2, count);
      Assert.assertEquals("test error", e.getMessage());
    }
  }

  @Test
  public void testQpsController() {
    QpsController qpsController = new QpsController("abc", 100);
    Assert.assertEquals(false, qpsController.isLimitNewRequest());

    qpsController.setQpsLimit(1);
    Assert.assertEquals(true, qpsController.isLimitNewRequest());
  }

  @Test
  public void testHandleWithException() {
    boolean validAssert;
    try {
      Mockito.when(invocation.getContext(Const.SRC_MICROSERVICE)).thenReturn(null);

      validAssert = true;
      handler.handle(invocation, asyncResp);
      handler.handle(invocation, asyncResp);
    } catch (Exception e) {
      e.printStackTrace();
      validAssert = false;
    }
    Assert.assertTrue(validAssert);
  }

  @Test
  public void testHandle() {
    boolean validAssert;
    try {
      validAssert = true;
      Mockito.when(invocation.getContext(Const.SRC_MICROSERVICE)).thenReturn("test");
      OperationMeta mockOperationMeta = QpsControllerManagerTest.getMockOperationMeta("pojo", "server", "opr");
      Mockito.when(invocation.getOperationMeta()).thenReturn(mockOperationMeta);

      new MockUp<QpsController>() {
        @Mock
        public boolean isLimitNewRequest() {
          return true;
        }
      };

      new MockUp<QpsControllerManager>() {

        @Mock
        protected QpsController create(String qualifiedNameKey) {
          return new QpsController(qualifiedNameKey, 12);
        }
      };
      handler.handle(invocation, asyncResp);
    } catch (Exception e) {
      e.printStackTrace();
      validAssert = false;
    }
    Assert.assertTrue(validAssert);
  }

  @Test
  public void testHandleIsLimitNewRequestAsFalse() {
    boolean validAssert;
    try {
      validAssert = true;
      Mockito.when(invocation.getContext(Const.SRC_MICROSERVICE)).thenReturn("test");
      OperationMeta mockOperationMeta = QpsControllerManagerTest
          .getMockOperationMeta("pojo", "server", "opr");
      Mockito.when(invocation.getOperationMeta()).thenReturn(mockOperationMeta);

      new MockUp<QpsController>() {
        @Mock
        public boolean isLimitNewRequest() {
          return false;
        }
      };

      new MockUp<QpsControllerManager>() {

        @Mock
        protected QpsController create(String qualifiedNameKey) {
          return new QpsController(qualifiedNameKey, 12);
        }
      };
      handler.handle(invocation, asyncResp);
    } catch (Exception e) {
      e.printStackTrace();
      validAssert = false;
    }
    Assert.assertTrue(validAssert);
  }
}
