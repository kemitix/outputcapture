package net.kemitix.outputcapture;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TeeOutputStream}.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class TeeOutputStreamTest {

    @Test
    public void canWriteByteToSoloOutput() {
        //given
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(byteArray);
        final TeeOutputStream teeOutputStream = new TeeOutputStream(out, null);
        //when
        teeOutputStream.write('x');
        //then
        assertThat(byteArray.toString()).isEqualTo("x");
    }

    @Test
    public void canWriteByteArrayToSoloOutput() {
        //given
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(byteArray);
        final TeeOutputStream teeOutputStream = new TeeOutputStream(out, null);
        //when
        teeOutputStream.write("test".getBytes(), 0, 4);
        //then
        assertThat(byteArray.toString()).isEqualTo("test");
    }

    @Test
    public void canWriteByteToSecondaryOutput() {
        //given
        final ByteArrayOutputStream byteArray1 = new ByteArrayOutputStream();
        final ByteArrayOutputStream byteArray2 = new ByteArrayOutputStream();
        final PrintStream out1 = new PrintStream(byteArray1);
        final PrintStream out2 = new PrintStream(byteArray2);
        final TeeOutputStream teeOutputStream = new TeeOutputStream(out1, null, out2);
        //when
        teeOutputStream.write('x');
        //then
        assertThat(byteArray1.toString()).isEqualTo("x");
        assertThat(byteArray2.toString()).isEqualTo("x");
    }

    @Test
    public void canWriteByteArrayToSecondaryOutput() {
        //given
        final ByteArrayOutputStream byteArray1 = new ByteArrayOutputStream();
        final ByteArrayOutputStream byteArray2 = new ByteArrayOutputStream();
        final PrintStream out1 = new PrintStream(byteArray1);
        final PrintStream out2 = new PrintStream(byteArray2);
        final TeeOutputStream teeOutputStream = new TeeOutputStream(out1, null, out2);
        //when
        teeOutputStream.write("test".getBytes(), 0, 4);
        //then
        assertThat(byteArray1.toString()).isEqualTo("test");
        assertThat(byteArray2.toString()).isEqualTo("test");
    }

    @Test
    public void canWriteByteToSecondaryThreadFilteredOutput() {
        //given
        final ByteArrayOutputStream byteArray1 = new ByteArrayOutputStream();
        final ByteArrayOutputStream byteArray2 = new ByteArrayOutputStream();
        final PrintStream out1 = new PrintStream(byteArray1);
        final Thread filteredThread = new Thread();
        final PrintStream out2 = new ThreadFilteredPrintStreamWrapper(new PrintStream(byteArray2), filteredThread);
        final TeeOutputStream teeOutputStream = new TeeOutputStream(out1, filteredThread, out2);
        //when
        teeOutputStream.write('x');
        //then
        assertThat(byteArray1.toString()).isEqualTo("x");
        assertThat(byteArray2.toString()).isEqualTo("x");
    }

    @Test
    public void canWriteByteArrayToSecondaryThreadFilteredOutput() {
        //given
        final ByteArrayOutputStream byteArray1 = new ByteArrayOutputStream();
        final ByteArrayOutputStream byteArray2 = new ByteArrayOutputStream();
        final PrintStream out1 = new PrintStream(byteArray1);
        final Thread filteredThread = new Thread();
        final PrintStream out2 = new ThreadFilteredPrintStreamWrapper(new PrintStream(byteArray2), filteredThread);
        final TeeOutputStream teeOutputStream = new TeeOutputStream(out1, filteredThread, out2);
        //when
        teeOutputStream.write("test".getBytes(), 0, 4);
        //then
        assertThat(byteArray1.toString()).isEqualTo("test");
        assertThat(byteArray2.toString()).isEqualTo("test");
    }


    @Test
    public void willNotWriteByteToSecondaryThreadFilteredOutputOnWrongThread() {
        //given
        final ByteArrayOutputStream byteArray1 = new ByteArrayOutputStream();
        final ByteArrayOutputStream byteArray2 = new ByteArrayOutputStream();
        final PrintStream out1 = new PrintStream(byteArray1);
        final Thread filteredThread = new Thread();
        final Thread nonFilteredThread = new Thread();
        final PrintStream out2 = new ThreadFilteredPrintStreamWrapper(new PrintStream(byteArray2), filteredThread);
        final TeeOutputStream teeOutputStream = new TeeOutputStream(out1, nonFilteredThread, out2);
        //when
        teeOutputStream.write('x');
        //then
        assertThat(byteArray1.toString()).isEqualTo("x");
        assertThat(byteArray2.toString()).isEqualTo("");
    }

    @Test
    public void willNotWriteByteArrayToSecondaryThreadFilteredOutputOnWrongThread() {
        //given
        final ByteArrayOutputStream byteArray1 = new ByteArrayOutputStream();
        final ByteArrayOutputStream byteArray2 = new ByteArrayOutputStream();
        final PrintStream out1 = new PrintStream(byteArray1);
        final Thread filteredThread = new Thread();
        final Thread nonFilteredThread = new Thread();
        final PrintStream out2 = new ThreadFilteredPrintStreamWrapper(new PrintStream(byteArray2), filteredThread);
        final TeeOutputStream teeOutputStream = new TeeOutputStream(out1, nonFilteredThread, out2);
        //when
        teeOutputStream.write("test".getBytes(), 0, 4);
        //then
        assertThat(byteArray1.toString()).isEqualTo("test");
        assertThat(byteArray2.toString()).isEqualTo("");
    }
}
