package com.luoj.task.example.jcache;

import cn.jpush.jcache.client.JcacheTemplate;
import com.luoj.common.ExampleConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

import static com.luoj.common.GenerateRandomDataUtils.*;

/**
 * @author lj.michale
 * @description
 * @date 2021-07-05
 */
@Slf4j
public class JCacheSinkExample002 {

    public static void main(String[] args) throws Exception {

        // 1. 初始化流式运行环境
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.enableCheckpointing(60000);
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30000);
        env.getCheckpointConfig().setCheckpointTimeout(10000);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        env.getCheckpointConfig().setCheckpointStorage("file:///E:\\OpenSource\\GitHub\\bigdata-learning\\常见大数据项目\\flink1.13-learning\\checkpoint");

        // Get Data And Do Some ETL
        SingleOutputStreamOperator<Tuple4<String, String, Long, Integer>> dStream =
                env.addSource(new RichSourceFunction<Tuple4<String, String, Long, Integer>>() {
                    private boolean isCanaled = false;
                    @Override
                    public void run(SourceContext<Tuple4<String, String, Long, Integer>> ctx) throws Exception {
                        while (!isCanaled) {
                            long  currentTimeStamp = System.currentTimeMillis();
                            ctx.collect(new Tuple4<>(getRandomUserID(), getRandomProductID(), currentTimeStamp, getRandomPrice().intValue()));
                        }
                    }
                    @Override
                    public void cancel() {
                        isCanaled = true;
                    }
                }).filter((FilterFunction<Tuple4<String, String, Long, Integer>>) value -> value != null);

        dStream.print();

        String iceLocator = ExampleConstant.JCACHE_ICE_LOCATOR;
        String proxyName = ExampleConstant.JCACHE_PROXY_NAME;
        String resourceName = ExampleConstant.JCACHE_CLUSTER_RESOURCE_NAME;
        // Sink To JCache
        dStream.addSink(new SinkToJCache(iceLocator, proxyName, resourceName));

        env.execute("JCacheSinkExample002");

    }
}
