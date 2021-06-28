package com.remmel.recorder3d.recorder.video;

import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class VideoUtils {
    private static final String TAG = VideoUtils.class.getSimpleName();

    public static boolean mux(File videoFile, File audioFile, File outputFile) {
        Movie video;
        try {
            video = new MovieCreator().build(videoFile.getAbsolutePath());
        } catch (RuntimeException e) {
            e.printStackTrace();

            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Movie audio;
        try {
            audio = new MovieCreator().build(audioFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        Track audioTrack = audio.getTracks().get(0);
        video.addTrack(audioTrack);

        Container out = new DefaultMp4Builder().build(video);

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        BufferedWritableFileByteChannel byteBufferByteChannel = new BufferedWritableFileByteChannel(fos);
        try {
            out.writeContainer(byteBufferByteChannel);
            byteBufferByteChannel.close();
            Log.e(TAG, "byteBufferByteChannel written and closed");
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static class BufferedWritableFileByteChannel implements WritableByteChannel {
        //    private static final int BUFFER_CAPACITY = 1000000;
        private static final int BUFFER_CAPACITY = 10000000;

        private boolean isOpen = true;
        private final OutputStream outputStream;
        private final ByteBuffer byteBuffer;
        private final byte[] rawBuffer = new byte[BUFFER_CAPACITY];

        private BufferedWritableFileByteChannel(OutputStream outputStream) {
            this.outputStream = outputStream;
            this.byteBuffer = ByteBuffer.wrap(rawBuffer);
            Log.e(TAG, "construct");
        }

        @Override
        public int write(ByteBuffer inputBuffer) throws IOException {
            int inputBytes = inputBuffer.remaining();

            if (inputBytes > byteBuffer.remaining()) {
                Log.e(TAG, "song size is ok");
                dumpToFile();
                byteBuffer.clear();

                if (inputBytes > byteBuffer.remaining()) {
                    Log.e(TAG, "song size is not ok");
                    throw new BufferOverflowException();
                }
            }

            byteBuffer.put(inputBuffer);

            return inputBytes;
        }

        @Override
        public boolean isOpen() {
            return isOpen;
        }

        @Override
        public void close() throws IOException {
            dumpToFile();
            isOpen = false;
        }

        private void dumpToFile() {
            try {
                outputStream.write(rawBuffer, 0, byteBuffer.position());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
