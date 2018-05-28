package net.kemitix.outputcapture;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CapturedOutputLineTest {

    @Test
    public void asString() {
        //given
        final String xyz = "xyz";
        final List<Byte> byteList = new ArrayList<>();
        for (char aChar : xyz.toCharArray()) {
            byteList.add((byte) aChar);
        }
        //when
        final String result = CapturedOutputLine.byteListToString(byteList);
        //then
        assertThat(result).isEqualTo(xyz);
    }

}