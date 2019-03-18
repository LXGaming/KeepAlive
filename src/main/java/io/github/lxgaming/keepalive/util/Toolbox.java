/*
 * Copyright 2019 Alex Thomson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lxgaming.keepalive.util;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class Toolbox {
    
    public static ThreadFactory buildThreadFactory(String namingPattern) {
        return new BasicThreadFactory.Builder().namingPattern(namingPattern).daemon(true).priority(Thread.NORM_PRIORITY).build();
    }
    
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(corePoolSize, buildThreadFactory("Service Thread #%d"));
        scheduledThreadPoolExecutor.setMaximumPoolSize(maximumPoolSize);
        scheduledThreadPoolExecutor.setKeepAliveTime(keepAliveTime, unit);
        return scheduledThreadPoolExecutor;
    }
    
    public static <T> T getField(Object instance, String name, Class<T> typeOfT) {
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return typeOfT.cast(field.get(instance));
        } catch (Exception ex) {
            return null;
        }
    }
}