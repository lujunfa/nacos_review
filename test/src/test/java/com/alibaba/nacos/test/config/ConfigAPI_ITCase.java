/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacos.test.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.http.MetricsHttpAgent;
import com.alibaba.nacos.client.config.http.ServerHttpAgent;
import com.alibaba.nacos.client.config.impl.HttpSimpleClient.HttpResult;
import com.alibaba.nacos.client.config.http.HttpAgent;
import com.alibaba.nacos.config.server.Config;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author xiaochun.xxc
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConfigAPI_ITCase {
    public static final long TIME_OUT = 2000;
    public ConfigService iconfig = null;
    HttpAgent agent = null;

    static final String CONFIG_CONTROLLER_PATH = "/v1/cs/configs";
    String SPECIAL_CHARACTERS = "!@#$%^&*()_+-=_|/'?.";
    String dataId = "yanlin";
    String group = "yanlin";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Before
    public void setUp() throws Exception {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, "127.0.0.1"+":"+port);
        iconfig = NacosFactory.createConfigService(properties);

        agent = new MetricsHttpAgent(new ServerHttpAgent(properties));
        agent.start();
    }

    @After
    public void cleanup() throws Exception {
        HttpResult result = null;
        try {
            List<String> params = Arrays.asList("dataId", dataId, "group", group, "beta", "true");
            result = agent.httpDelete(CONFIG_CONTROLLER_PATH + "/", null, params, agent.getEncode(), TIME_OUT);
            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);
            Assert.assertEquals(true, JSON.parseObject(result.content).getBoolean("data"));
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    /**
     * @TCDescription : nacos_??????????????????
     * @TestStep :
     * @ExpectResult :
     */
    @Test(timeout = 3*TIME_OUT)
    public void nacos_getconfig_1() throws Exception {
        final String content = "test";
        boolean result = iconfig.publishConfig(dataId, group, content);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);
        String value = iconfig.getConfig(dataId, group, TIME_OUT);
        Assert.assertEquals(content, value);
        result = iconfig.removeConfig(dataId, group);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);
        value = iconfig.getConfig(dataId, group, TIME_OUT);
        System.out.println(value);
        Assert.assertEquals(null, value);
    }

    /**
     * @TCDescription : nacos_????????????????????????????????????
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_getconfig_2() throws Exception {
        String content = iconfig.getConfig(dataId, "nacos", TIME_OUT);
        Assert.assertNull(content);
    }

    /**
     * @TCDescription : nacos_???????????????dataId???null
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_getconfig_3() throws Exception {
        try {
            String content = iconfig.getConfig(null, group, TIME_OUT);
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }
        Assert.assertTrue(false);
    }

    /**
     * @TCDescription : nacos_???????????????group???null
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_getconfig_4() throws Exception {
        final String content = "test";

        boolean result = iconfig.publishConfig(dataId, null, content);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);

        String value = iconfig.getConfig(dataId, null, TIME_OUT);
        Assert.assertEquals(content, value);

        result = iconfig.removeConfig(dataId, null);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);
    }

    /**
     * @TCDescription : nacos_????????????????????????????????????????????????
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_publishConfig_1() throws Exception {
        final String content = "publishConfigTest";
        boolean result = iconfig.publishConfig(dataId, group, content);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);
        result = iconfig.removeConfig(dataId, group);
        Assert.assertTrue(result);
    }

    /**
     * @TCDescription : nacos_????????????????????????????????????????????????
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_publishConfig_2() throws Exception {
        final String content = "publishConfigTest";
        boolean result = iconfig.publishConfig(dataId, group, content);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);

        final String content1 = "test.abc";
        result = iconfig.publishConfig(dataId, group, content1);
        Thread.sleep(TIME_OUT);
        String value = iconfig.getConfig(dataId, group, TIME_OUT);
        Assert.assertEquals(content1, value);
    }

    /**
     * @TCDescription : nacos_?????????????????????????????????
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_publishConfig_3() throws Exception {
        String content = "test" + SPECIAL_CHARACTERS;
        boolean result = iconfig.publishConfig(dataId, group, content);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);

        String value = iconfig.getConfig(dataId, group, TIME_OUT);
        Assert.assertEquals(content, value);
    }

    /**
     * @TCDescription : nacos_???????????????dataId???null
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_publishConfig_4() throws Exception {
        try {
            String content = "test";
            boolean result = iconfig.publishConfig(null, group, content);
            Thread.sleep(TIME_OUT);
            Assert.assertTrue(result);
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }
        Assert.assertTrue(false);
    }

    /**
     * @TCDescription : nacos_???????????????group???null
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_publishConfig_5() throws Exception {
        String content = "test";
        boolean result = iconfig.publishConfig(dataId, null, content);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);

        String value = iconfig.getConfig(dataId, null, TIME_OUT);
        Assert.assertEquals(content, value);
    }


    /**
     * @TCDescription : nacos_??????????????????????????????null
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_publishConfig_6() throws Exception {
        String content = null;
        try {
            boolean result = iconfig.publishConfig(dataId, group, content);
            Thread.sleep(TIME_OUT);
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }
        Assert.assertTrue(false);
    }

    /**
     * @TCDescription : nacos_?????????????????????????????????????????????
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_publishConfig_7() throws Exception {
        String content = "??????abc";
        boolean result = iconfig.publishConfig(dataId, group, content);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);
        String value  = iconfig.getConfig(dataId, group, TIME_OUT);
        Assert.assertEquals(content, value);
    }

    /**
     * @TCDescription : nacos_????????????????????????????????????????????????
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_removeConfig_1() throws Exception {
        String content = "test";
        boolean result = iconfig.publishConfig(dataId, group, content);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);

        result = iconfig.removeConfig(dataId, group);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);
        String value = iconfig.getConfig(dataId, group, TIME_OUT);
        Assert.assertEquals(null, value);
    }

    /**
     * @TCDescription : nacos_????????????????????????????????????????????????
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_removeConfig_2() throws Exception {
        group += "removeConfig2";
        boolean result = iconfig.removeConfig(dataId, group);
        Assert.assertTrue(result);
    }

    /**
     * @TCDescription : nacos_???????????????dataId???null
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_removeConfig_3() throws Exception {
        try {
            boolean result = iconfig.removeConfig(null, group);
            Assert.assertTrue(result);
        } catch (Exception e) {
            Assert.assertTrue(true);
            return;
        }
        Assert.assertTrue(false);
    }

    /**
     * @TCDescription : nacos_???????????????group???null
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_removeConfig_4() throws Exception {
        boolean result = iconfig.removeConfig(dataId, null);
        Assert.assertTrue(result);
    }

    /**
     * @TCDescription : nacos_?????????dataId???????????????????????????????????????????????????????????????????????????
     * @throws Exception
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_addListener_1() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final String content = "test-abc";
        boolean result = iconfig.publishConfig(dataId, group, content);
        Assert.assertTrue(result);

        Listener ml = new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                System.out.println("recieve23:" + configInfo);
                count.incrementAndGet();
                Assert.assertEquals(content, configInfo);
            }

            @Override
            public Executor getExecutor() {
                return null;
            }
        };
        iconfig.addListener(dataId, group, ml);
        while (count.get() == 0) {
            Thread.sleep(2000);
        }
        Assert.assertTrue(count.get() >= 1);
        iconfig.removeListener(dataId, group, ml);
    }

    /**
     * @TCDescription : nacos_??????????????????null?????????????????????
     * @TestStep :
     * @ExpectResult :
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = TIME_OUT)
    public void nacos_addListener_2() {
        try {
            iconfig.addListener(dataId, group, null);
            Assert.assertFalse(true);
        } catch (Exception e) {
            Assert.assertFalse(false);
        }
    }


    /**
     * @TCDescription : nacos_?????????dataId??????????????????????????????????????????????????????????????????
     * @TestStep : TODO Test steps
     * @ExpectResult : TODO expect results
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_addListener_3() throws InterruptedException, NacosException {
        final AtomicInteger count = new AtomicInteger(0);
        final String content = "test-abc";
        boolean result = iconfig.publishConfig(dataId, group, content);
        Assert.assertTrue(result);

        Listener ml = new AbstractListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                count.incrementAndGet();
                Assert.assertEquals(content, configInfo);
            }
        };
        iconfig.addListener(dataId, group, ml);
        while (count.get() == 0) {
            Thread.sleep(2000);
        }
        Assert.assertEquals(1, count.get());
        iconfig.removeListener(dataId, group, ml);
    }

    /**
     * @TCDescription : nacos_?????????????????????????????????dataId?????????
     * @TestStep : TODO Test steps
     * @ExpectResult : TODO expect results
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_addListener_4() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);

        iconfig.removeConfig(dataId, group);
        Thread.sleep(TIME_OUT);

        Listener ml = new AbstractListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                count.incrementAndGet();
            }
        };
        iconfig.addListener(dataId, group, ml);
        Thread.sleep(TIME_OUT);
        String content = "test-abc";
        boolean result = iconfig.publishConfig(dataId, group, content);
        Assert.assertTrue(result);

        while (count.get() == 0) {
            Thread.sleep(3000);
        }
        Assert.assertEquals(1, count.get());
        iconfig.removeListener(dataId, group, ml);
    }

    /**
     * @TCDescription : nacos_?????????????????????
     * @TestStep : TODO Test steps
     * @ExpectResult : TODO expect results
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_removeListener_1() throws Exception {
        iconfig.addListener(dataId, group, new AbstractListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                Assert.assertTrue(false);
            }
        });
        Thread.sleep(TIME_OUT);
        try {
            iconfig.removeListener(dataId, group, new AbstractListener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    System.out.println("remove recieve:" + configInfo);
                }
            });
        } catch (Exception e) {

        }
    }

    /**
     * @TCDescription : nacos_???????????????dataId????????????
     * @TestStep : TODO Test steps
     * @ExpectResult : TODO expect results
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = TIME_OUT)
    public void nacos_removeListener_2() {
        group += "test.nacos";
        try {
            iconfig.removeListener(dataId, group, new AbstractListener() {
                @Override
                public void receiveConfigInfo(String configInfo) {

                }
            });
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    /**
     * @TCDescription : nacos_??????????????????????????????????????????????????????
     * @TestStep : TODO Test steps
     * @ExpectResult : TODO expect results
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 5*TIME_OUT)
    public void nacos_removeListener_3() throws Exception {
        final String contentRemove = "test-abc-two";
        final AtomicInteger count = new AtomicInteger(0);

        Listener ml = new AbstractListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                count.incrementAndGet();
            }
        };
        Listener ml1 = new AbstractListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                //System.out.println("ml1 remove listener recieve:" + configInfo);
                count.incrementAndGet();
                Assert.assertEquals(contentRemove, configInfo);
            }
        };
        iconfig.addListener(dataId, group, ml);
        iconfig.addListener(dataId, group, ml1);

        iconfig.removeListener(dataId, group, ml);
        Thread.sleep(TIME_OUT);

        boolean result = iconfig.publishConfig(dataId, group, contentRemove);
        Thread.sleep(TIME_OUT);
        Assert.assertTrue(result);

        while (count.get() == 0) {
            Thread.sleep(3000);
        }
        Assert.assertNotEquals(0, count.get());
    }

    /**
     * @TCDescription : nacos_????????????null???
     * @TestStep : TODO Test steps
     * @ExpectResult : TODO expect results
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = TIME_OUT)
    public void nacos_removeListener_4() {
        iconfig.removeListener(dataId, group, (Listener) null);
        Assert.assertTrue(true);
    }

    /**
     * @TCDescription : nacos_openAPI_??????????????????
     * @TestStep :
     * @ExpectResult :
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 3*TIME_OUT)
    public void nacos_openAPI_detailConfig_1() {
        HttpResult result = null;

        try {
            final String content = "test";
            boolean ret = iconfig.publishConfig(dataId, group, content);
            Thread.sleep(TIME_OUT);
            Assert.assertTrue(ret);

            List<String> params = Arrays.asList("dataId", dataId, "group", group, "show", "all");
            result = agent.httpGet(CONFIG_CONTROLLER_PATH, null, params, agent.getEncode(), TIME_OUT);
            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);

            Assert.assertEquals(content, JSON.parseObject(result.content).getString("content"));
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    /**
     * @TCDescription : nacos_openAPI_catalog??????
     * @TestStep :
     * @ExpectResult :
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 3*TIME_OUT)
    public void nacos_openAPI_catalog() {
        HttpResult result = null;

        try {
            final String content = "test";
            boolean ret = iconfig.publishConfig(dataId, group, content);
            Thread.sleep(TIME_OUT);
            Assert.assertTrue(ret);

            List<String> params = Arrays.asList("dataId", dataId, "group", group);
            result = agent.httpGet(CONFIG_CONTROLLER_PATH+"/catalog", null, params, agent.getEncode(), TIME_OUT);
            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);

            System.out.println(result.content);
            Assert.assertNotNull(JSON.parseObject(result.content).getString("data"));

        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    /**
     * @TCDescription : nacos_openAPI_queryBeta??????
     * @TestStep :
     * @ExpectResult :
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 3*TIME_OUT)
    public void nacos_openAPI_queryBeta_1() {
        HttpResult result = null;

        try {
            final String content = "test-beta";
            List<String> headers = Arrays.asList("betaIps", "127.0.0.1");
            List<String> params1 = Arrays.asList("dataId", dataId, "group", group, "content", content);
            result = agent.httpPost(CONFIG_CONTROLLER_PATH + "/", headers, params1, agent.getEncode(), TIME_OUT);
            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);
            Assert.assertEquals("true", result.content);

            List<String> params = Arrays.asList("dataId", dataId, "group", group, "beta", "true");
            result = agent.httpGet(CONFIG_CONTROLLER_PATH + "/", null, params, agent.getEncode(), TIME_OUT);
            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);
            Assert.assertEquals(content, JSON.parseObject(result.content).getJSONObject("data").getString("content"));
            // delete data
            result = agent.httpDelete(CONFIG_CONTROLLER_PATH + "/", null, params, agent.getEncode(), TIME_OUT);
            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    /**
     * @TCDescription : nacos_openAPI_queryBeta????????????
     * @TestStep : 1. ????????????
     *             2. ??????Beta????????????
     * @ExpectResult :
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 3*TIME_OUT)
    public void nacos_openAPI_queryBeta_delete() {
        HttpResult result = null;

        try {
            final String content = "test-beta";
            List<String> headers = Arrays.asList("betaIps", "127.0.0.1");
            List<String> params1 = Arrays.asList("dataId", dataId, "group", group, "content", content);
            result = agent.httpPost(CONFIG_CONTROLLER_PATH + "/", headers, params1, agent.getEncode(), TIME_OUT);
            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);
            Assert.assertEquals("true", result.content);


            List<String> params = Arrays.asList("dataId", dataId, "group", group, "beta", "true");
            result = agent.httpDelete(CONFIG_CONTROLLER_PATH + "/", null, params, agent.getEncode(), TIME_OUT);

            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);
            Assert.assertEquals(true, JSON.parseObject(result.content).getBoolean("data"));
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    /**
     * @TCDescription : nacos_openAPI_????????????????????????
     * @TestStep : 1. ????????????
     *             2. ????????????
     * @ExpectResult : ?????????????????????
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 3*TIME_OUT)
    public void nacos_openAPI_fuzzySearchConfig() {
        HttpResult result = null;

        try {
            final String content = "test123";
            boolean ret = iconfig.publishConfig(dataId, group, content);
            Thread.sleep(TIME_OUT);
            Assert.assertTrue(ret);

            List<String> params = Arrays.asList("dataId", dataId, "group", group, "pageNo","1", "pageSize","10", "search", "blur");
            result = agent.httpGet(CONFIG_CONTROLLER_PATH + "/", null, params, agent.getEncode(), TIME_OUT);
            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);

            Assert.assertTrue(JSON.parseObject(result.content).getIntValue("totalCount") >= 1);
            Assert.assertTrue(JSON.parseObject(result.content).getJSONArray("pageItems").getJSONObject(0).getString("content").startsWith(content));
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    /**
     * @TCDescription : nacos_openAPI_????????????????????????
     * @TestStep : 1. ????????????
     *             2. ??????????????????
     * @ExpectResult : ?????????????????????
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 3*TIME_OUT)
    public void nacos_openAPI_fuzzySearchConfig_1() {
        HttpResult result = null;

        try {
            final String content = "test123";
            boolean ret = iconfig.publishConfig(dataId, group, content);
            Thread.sleep(TIME_OUT);
            Assert.assertTrue(ret);

            List<String> params = Arrays.asList("dataId", dataId+"*", "group", group+"*", "pageNo","1", "pageSize","10", "search", "blur");
            result = agent.httpGet(CONFIG_CONTROLLER_PATH + "/", null, params, agent.getEncode(), TIME_OUT);

            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);
            Assert.assertTrue(JSON.parseObject(result.content).getIntValue("totalCount") >= 1);
            Assert.assertEquals(content, JSON.parseObject(result.content).getJSONArray("pageItems").getJSONObject(0).getString("content"));

        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    /**
     * @TCDescription : nacos_openAPI_??????????????????
     * @TestStep : 1. ????????????
     *             2. ??????????????????
     * @ExpectResult : ?????????????????????
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 3*TIME_OUT)
    public void nacos_openAPI_searchConfig() {
        HttpResult result = null;

        try {
            final String content = "test123";
            boolean ret = iconfig.publishConfig(dataId, group, content);
            Thread.sleep(TIME_OUT);
            Assert.assertTrue(ret);

            List<String> params = Arrays.asList("dataId", dataId, "group", group, "pageNo","1", "pageSize","10", "search", "accurate");
            result = agent.httpGet(CONFIG_CONTROLLER_PATH + "/", null, params, agent.getEncode(), TIME_OUT);

            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);
            Assert.assertTrue(JSON.parseObject(result.content).getIntValue("totalCount") == 1);
            Assert.assertEquals(content, JSON.parseObject(result.content).getJSONArray("pageItems").getJSONObject(0).getString("content"));

        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    /**
     * @TCDescription : nacos_openAPI_????????????????????????????????????utf-8
     * @TestStep : 1. ????????????
     *             2. ??????????????????
     * @ExpectResult : ?????????????????????
     * @author xiaochun.xxc
     * @since 3.6.8
     */
    @Test(timeout = 3*TIME_OUT)
    public void nacos_openAPI_searchConfig_2() {
        HttpResult result = null;

        try {
            final String content = "test??????";
            boolean ret = iconfig.publishConfig(dataId, group, content);
            Thread.sleep(TIME_OUT);
            Assert.assertTrue(ret);

            List<String> params = Arrays.asList("dataId", dataId, "group", group, "pageNo","1", "pageSize","10", "search", "accurate");
            result = agent.httpGet(CONFIG_CONTROLLER_PATH + "/", null, params, "utf-8", TIME_OUT);
            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.code);
            Assert.assertTrue(JSON.parseObject(result.content).getIntValue("totalCount") == 1);
            Assert.assertEquals(content, JSON.parseObject(result.content).getJSONArray("pageItems").getJSONObject(0).getString("content"));
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

}
