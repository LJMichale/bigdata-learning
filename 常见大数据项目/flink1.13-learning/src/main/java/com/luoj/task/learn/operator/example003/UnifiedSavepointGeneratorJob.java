package com.luoj.task.learn.operator.example003;

import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author lj.michale
 * @description
 * @date 2021-05-29
 */
public class UnifiedSavepointGeneratorJob {

    public static void main(String[] args) throws Exception {

        ParameterTool tool = ParameterTool.fromArgs(args);
//        int totalRecords = tool.getInt("total_records");
//        int numberOfKeys = tool.getInt("num_keys");
//        int parallelism = tool.getInt("parallelism");
        int totalRecords = 100000;
        int numberOfKeys = 100;
        int parallelism = 1;

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(parallelism);
        env.enableCheckpointing(20, CheckpointingMode.EXACTLY_ONCE);

//        String stateBackendName = tool.get("state_backend");
        String stateBackendName = "hashmap";

        switch (stateBackendName) {
            case "hashmap":
                env.setStateBackend(new HashMapStateBackend());
                break;
            case "rocksdb":
                env.setStateBackend(new EmbeddedRocksDBStateBackend());
                break;
            case "rocksdb_incre":
                env.setStateBackend(new EmbeddedRocksDBStateBackend(true));
                break;
            default:
                throw new RuntimeException("Not supported statebackend " + stateBackendName);
        }

//        String stateBackendPath = tool.get("state_backend_path");
        String stateBackendPath = "file:///E:\\OpenSource\\GitHub\\bigdata-learning\\常见大数据项目\\flink1.13-learning\\checkpoints";
        env.getCheckpointConfig().setCheckpointStorage(stateBackendPath);

        DataStream<Tuple2<Integer, Integer>> source = env.addSource(
                new Source(totalRecords, numberOfKeys)).uid("source");

        KeySelector<Tuple2<Integer, Integer>, Integer> keySelector = tuple -> tuple.f0;
        source.keyBy(keySelector)
                .transform("value state", source.getType(), new ValueStateOperator(totalRecords))
                .uid("value_state")
                .keyBy(keySelector)
                .transform(
                        "reducing state", source.getType(), new ReduceStateOperator(totalRecords))
                .uid("reducing_state")
                .keyBy(keySelector)
                .transform(
                        "aggregation state",
                        source.getType(),
                        new AggregationStateOperator(totalRecords))
                .uid("aggregating_state")
                .keyBy(keySelector)
                .transform("list state", source.getType(), new ListStateOperator(totalRecords))
                .uid("list_state")
                .keyBy(keySelector)
                .transform("map state", source.getType(), new MapStateOperator(totalRecords))
                .uid("map_state");

        env.execute();
    }
}