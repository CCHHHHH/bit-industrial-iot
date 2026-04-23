package com.bit.iot.integration.tdengine;

import com.bit.iot.integration.config.TDEngineProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TDEnginePointWriterTest {

    @Test
    void buildChildTableNameSanitizesUnsafeCharacters() {
        TDEngineProperties properties = new TDEngineProperties();
        properties.setSuperTable("point_data");
        TDEnginePointWriter writer = new TDEnginePointWriter(properties);

        String tableName = writer.buildChildTableName("device-001';drop", "TEMP.001");

        assertThat(tableName).isEqualTo("point_data_device_001_drop_TEMP_001");
    }

    @Test
    void writeEmptyPointsDoesNotRequireTdengineConfig() {
        TDEnginePointWriter writer = new TDEnginePointWriter(new TDEngineProperties());

        assertThat(writer.write(List.of())).isZero();
    }
}
