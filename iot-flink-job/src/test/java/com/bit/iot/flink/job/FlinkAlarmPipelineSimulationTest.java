package com.bit.iot.flink.job;

import com.bit.iot.common.flink.RuleJobConfig;
import com.bit.iot.common.flink.connector.model.AlgorithmOutputEvent;
import com.bit.iot.common.flink.connector.model.DeviceDataEvent;
import com.bit.iot.common.flink.connector.window.DynamicWindowAssigner;
import com.bit.iot.flink.job.process.AlgorithmWindowFunction;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.legacy.RichSinkFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class FlinkAlarmPipelineSimulationTest {

    @AfterEach
    void tearDown() {
        CollectSink.clear();
    }

    @Test
    void shouldGenerateAlgorithmOutputFromSimulatedDeviceData() throws Exception {
        Path algorithmJar = TestAlgorithmJarBuilder.buildThresholdAlgorithmJar();

        RuleJobConfig config = new RuleJobConfig();
        config.setRuleId("rule-1");
        config.setRuleName("温度阈值规则");
        config.setWindowType("tumbling");
        config.setWindowSizeMs(1_000L);
        config.setKeyStrategy("device_point");
        config.setParallelism(1);
        config.setAlgorithmType("jar");
        config.setAlgorithmPath(algorithmJar.toString());
        config.setAlgorithmClass("com.bit.iot.test.algorithm.MockThresholdAlgorithm");
        config.setRuleParams(Map.of(
                "threshold", "70",
                "alertLevel", "error",
                "alertMessage", "温度超限"
        ));

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        List<DeviceDataEvent> events = List.of(
                new DeviceDataEvent("device-1", "temp", 1_000L, 68.5, 0),
                new DeviceDataEvent("device-1", "temp", 1_400L, 71.2, 0),
                new DeviceDataEvent("device-1", "temp", 1_800L, 72.3, 0)
        );

        WatermarkStrategy<DeviceDataEvent> watermarkStrategy =
                WatermarkStrategy.<DeviceDataEvent>forBoundedOutOfOrderness(Duration.ZERO)
                        .withTimestampAssigner((event, recordTimestamp) -> event.getTimestamp());

        DataStream<DeviceDataEvent> source = env.fromCollection(events)
                .assignTimestampsAndWatermarks(watermarkStrategy);

        KeyedStream<DeviceDataEvent, String> keyed = source.keyBy(event -> event.getDeviceId() + "#" + event.getPointCode());
        DataStream<AlgorithmOutputEvent> result = DynamicWindowAssigner.applyWindow(keyed, config)
                .process(new AlgorithmWindowFunction(
                        config.getRuleId(),
                        config.getAlgorithmType(),
                        config.getAlgorithmPath(),
                        config.getAlgorithmClass(),
                        config.getRuleParams()
                ));

        result.addSink(new CollectSink());
        env.execute("flink-alarm-pipeline-simulation-test");

        assertThat(CollectSink.values()).hasSize(1);
        AlgorithmOutputEvent output = CollectSink.values().get(0);
        assertThat(output.getRuleId()).isEqualTo("rule-1");
        assertThat(output.getKey()).isEqualTo("device-1#temp");
        assertThat(output.isSuccess()).isTrue();
        assertThat(output.getResultData()).containsEntry("alert", true);
        assertThat(output.getResultData()).containsEntry("alertLevel", "error");
        assertThat(output.getResultData()).containsEntry("alertMessage", "温度超限");
        assertThat(output.getResultData()).containsKey("maxValue");
    }

    static class CollectSink extends RichSinkFunction<AlgorithmOutputEvent> {

        private static final List<AlgorithmOutputEvent> VALUES = new CopyOnWriteArrayList<>();

        @Override
        public void invoke(AlgorithmOutputEvent value, Context context) {
            VALUES.add(value);
        }

        static List<AlgorithmOutputEvent> values() {
            return VALUES;
        }

        static void clear() {
            VALUES.clear();
        }
    }

    static final class TestAlgorithmJarBuilder {

        private TestAlgorithmJarBuilder() {
        }

        static Path buildThresholdAlgorithmJar() throws Exception {
            Path workDir = Files.createTempDirectory("mock-algorithm");
            Path sourceDir = Files.createDirectories(workDir.resolve("src/com/bit/iot/test/algorithm"));
            Path classesDir = Files.createDirectories(workDir.resolve("classes"));
            Path sourceFile = sourceDir.resolve("MockThresholdAlgorithm.java");

            String source = "package com.bit.iot.test.algorithm;\n"
                    + "\n"
                    + "import com.bit.iot.common.flink.AlgorithmResult;\n"
                    + "import com.bit.iot.common.flink.DataPoint;\n"
                    + "import com.bit.iot.common.flink.IRuleAlgorithm;\n"
                    + "\n"
                    + "import java.util.HashMap;\n"
                    + "import java.util.List;\n"
                    + "import java.util.Map;\n"
                    + "\n"
                    + "public class MockThresholdAlgorithm implements IRuleAlgorithm {\n"
                    + "    @Override\n"
                    + "    public AlgorithmResult execute(List<DataPoint> dataPoints, Map<String, String> params) {\n"
                    + "        double max = dataPoints.stream()\n"
                    + "                .mapToDouble(DataPoint::getValue)\n"
                    + "                .max()\n"
                    + "                .orElse(0.0);\n"
                    + "        double threshold = Double.parseDouble(params.getOrDefault(\"threshold\", \"0\"));\n"
                    + "        boolean alert = max > threshold;\n"
                    + "        Map<String, Object> result = new HashMap<>();\n"
                    + "        result.put(\"alert\", alert);\n"
                    + "        result.put(\"alertLevel\", params.getOrDefault(\"alertLevel\", \"warning\"));\n"
                    + "        result.put(\"alertMessage\", params.getOrDefault(\"alertMessage\", \"模拟告警\"));\n"
                    + "        result.put(\"metricName\", \"temperature\");\n"
                    + "        result.put(\"metricValue\", String.valueOf(max));\n"
                    + "        result.put(\"maxValue\", max);\n"
                    + "        return AlgorithmResult.success(result);\n"
                    + "    }\n"
                    + "}\n";
            Files.writeString(sourceFile, source);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            assertThat(compiler).isNotNull();
            int result = compiler.run(
                    null,
                    null,
                    null,
                    "-classpath",
                    System.getProperty("java.class.path"),
                    "-d",
                    classesDir.toString(),
                    sourceFile.toString()
            );
            assertThat(result).isZero();

            Path jarPath = workDir.resolve("mock-algorithm.jar");
            try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
                List<Path> classFiles = Files.walk(classesDir)
                        .filter(Files::isRegularFile)
                        .sorted(Comparator.naturalOrder())
                        .toList();
                for (Path classFile : classFiles) {
                    String entryName = classesDir.relativize(classFile).toString().replace('\\', '/');
                    jarOutputStream.putNextEntry(new JarEntry(entryName));
                    jarOutputStream.write(Files.readAllBytes(classFile));
                    jarOutputStream.closeEntry();
                }
            }
            return jarPath;
        }
    }
}
