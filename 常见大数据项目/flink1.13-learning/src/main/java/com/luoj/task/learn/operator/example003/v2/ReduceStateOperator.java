package com.luoj.task.learn.operator.example003.v2;


import com.luoj.task.learn.operator.example003.Constants;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.state.ReducingState;
import org.apache.flink.api.common.state.ReducingStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.VoidNamespace;
import org.apache.flink.runtime.state.VoidNamespaceSerializer;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.BoundedOneInput;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.util.OutputTag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author lj.michale
 * @description
 * @date 2021-05-29
 */
public class ReduceStateOperator extends AbstractStreamOperator<Tuple2<MyKey, MyValue>>
        implements OneInputStreamOperator<Tuple2<MyKey, MyValue>, Tuple2<MyKey, MyValue>>,
        BoundedOneInput {

    public static OutputTag<String> STATE_RESULT_TAG =
            new OutputTag<String>(Constants.OUTPUT_TAG_NAME) {};

    private final int totalRecords;

    private ReducingStateDescriptor<MyValue> reducingStateDescriptor;
    private ReducingState<MyValue> reducingState;

    public ReduceStateOperator(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    @Override
    public void open() throws Exception {
        super.open();

        this.reducingStateDescriptor =
                new ReducingStateDescriptor<>(
                        "state",
                        (ReduceFunction<MyValue>)
                                (myValue, t1) -> new MyValue(myValue.getValue() + t1.getValue()),
                        MyValue.class);
        this.reducingState = getRuntimeContext().getReducingState(reducingStateDescriptor);
    }

    @Override
    public void processElement(StreamRecord<Tuple2<MyKey, MyValue>> streamRecord) throws Exception {
        reducingState.add(streamRecord.getValue().f1);
        output.collect(streamRecord);
    }

    @Override
    public void endInput() throws Exception {
        getKeyedStateBackend()
                .applyToAllKeys(
                        VoidNamespace.INSTANCE,
                        VoidNamespaceSerializer.INSTANCE,
                        reducingStateDescriptor,
                        (key, value) ->
                                output.collect(
                                        STATE_RESULT_TAG,
                                        new StreamRecord<>(key + " " + value.get().getValue())));
    }

    public static void checkResult(String fileName, int totalRecords, int numberOfKeys)
            throws IOException {
        Set<Integer> processedKeys = new HashSet<>();

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(
                    line -> {
                        String[] parts = line.split(" ");
                        int key = Integer.parseInt(parts[0]);
                        int value = Integer.parseInt(parts[1]);

                        if (processedKeys.contains(key)) {
                            throw new RuntimeException("Repeat keys: " + key);
                        }

                        processedKeys.add(key);

                        int maxValue = totalRecords / numberOfKeys;
                        if (key < totalRecords % numberOfKeys) {
                            maxValue += 1;
                        }

                        int expectedValue = maxValue * (maxValue - 1) / 2;
                        if (value != expectedValue) {
                            throw new RuntimeException(
                                    "Value not match: key = "
                                            + key
                                            + " value = "
                                            + value
                                            + ", expected = "
                                            + expectedValue);
                        }
                    });
        }

        for (int i = 0; i < numberOfKeys; ++i) {
            if (!processedKeys.contains(i)) {
                throw new RuntimeException("Key not found: " + i);
            }
        }
    }
}