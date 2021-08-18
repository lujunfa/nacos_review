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
package com.alibaba.nacos.naming.consistency.ephemeral.distro;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.naming.cluster.servers.Server;
import com.alibaba.nacos.naming.misc.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Data sync task dispatcher
 *
 * @author nkorange
 * @since 1.0.0
 */

//任务分发器
@Component
public class TaskDispatcher {

    @Autowired
    private GlobalConfig partitionConfig;

    @Autowired
    private DataSyncer dataSyncer;

    private List<TaskScheduler> taskSchedulerList = new ArrayList<>();

    @PostConstruct
    public void init() {
        for (int i = 0; i < partitionConfig.getTaskDispatchThreadCount(); i++) {
            TaskScheduler taskScheduler = new TaskScheduler(i);

            //任务调度执行器加入到链表
            taskSchedulerList.add(taskScheduler);

            //每个任务调度器由一个对应的线程支持
            GlobalExecutor.submitTaskDispatch(taskScheduler);
        }
    }

    public void addTask(String key) {
        //根据key计算
        taskSchedulerList.get(UtilsAndCommons.shakeUp(key, partitionConfig.getTaskDispatchThreadCount())).addTask(key);
    }

    //任务调度器
    public class TaskScheduler implements Runnable {

        private int index;

        private int dataSize = 0;

        private long lastDispatchTime = 0L;

        private BlockingQueue<String> queue = new LinkedBlockingQueue<>(128 * 1024);

        public TaskScheduler(int index) {
            this.index = index;
        }

        public void addTask(String key) {
            queue.offer(key);
        }

        public int getIndex() {
            return index;
        }

        @Override
        public void run() {

            List<String> keys = new ArrayList<>();
            while (true) {

                try {

                    String key = queue.poll(partitionConfig.getTaskDispatchPeriod(),
                        TimeUnit.MILLISECONDS);

                    if (Loggers.EPHEMERAL.isDebugEnabled() && StringUtils.isNotBlank(key)) {
                        Loggers.EPHEMERAL.debug("got key: {}", key);
                    }

                    if (dataSyncer.getServers() == null || dataSyncer.getServers().isEmpty()) {
                        continue;
                    }

                    if (StringUtils.isBlank(key)) {
                        continue;
                    }

                    if (dataSize == 0) {
                        keys = new ArrayList<>();
                    }

                    keys.add(key);
                    dataSize++;

                    if (dataSize == partitionConfig.getBatchSyncKeyCount() ||
                        (System.currentTimeMillis() - lastDispatchTime) > partitionConfig.getTaskDispatchPeriod()) {

                        for (Server member : dataSyncer.getServers()) {
                            if (NetUtils.localServer().equals(member.getKey())) {
                                continue;
                            }
                            SyncTask syncTask = new SyncTask();
                            syncTask.setKeys(keys);
                            syncTask.setTargetServer(member.getKey());

                            if (Loggers.EPHEMERAL.isDebugEnabled() && StringUtils.isNotBlank(key)) {
                                Loggers.EPHEMERAL.debug("add sync task: {}", JSON.toJSONString(syncTask));
                            }

                            //向其他节点发起同步key数据，让其他节点也写入key数据
                            dataSyncer.submit(syncTask, 0);
                        }
                        lastDispatchTime = System.currentTimeMillis();
                        dataSize = 0;
                    }

                } catch (Exception e) {
                    Loggers.EPHEMERAL.error("dispatch sync task failed.", e);
                }
            }
        }
    }
}
