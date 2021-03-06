package com.luoj.task.learn.datastream;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.util.Arrays;

/**
 * @author lj.michale
 * @description DataStream Api实现WordCount
 *              Flink1.12中DataStream支持流处理也支持批处理
 * @date 2021-04-19
 */
public class WordCountByLambda {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setRuntimeMode(RuntimeExecutionMode.BATCH);  // 注意使用DataStream实现批处理
//        env.setRuntimeMode(RuntimeExecutionMode.STREAMING); // 注意使用DataStream实现流处理
//        env.setRuntimeMode(RuntimeExecutionMode.AUTOMATIC); // 注意使用DataStream自动选择使用批处理还是流处理

        DataStreamSource<String> lines = env.fromElements("itcast hadoop spark","itcast hadoop spark","itcast hadoop","itcast");

        ////////////////////////// Transformation Start ///////////////////////////////////////
       SingleOutputStreamOperator<String> words = lines.flatMap(
               (String value, Collector<String> out) -> Arrays.stream(value.split(" ")).forEach(out :: collect)
       );

       DataStream<Tuple2<String, Integer>>  wordAndOne = words.map((String value) -> Tuple2.of(value, 1));
       KeyedStream<Tuple2<String, Integer>, String> grouped = wordAndOne.keyBy(t -> t.f0);
       SingleOutputStreamOperator<Tuple2<String, Integer>> result = grouped.sum(1);

        ////////////////////////// Transformation End ///////////////////////////////////////

        result.print();

    }

}
