package rs.ac.uns.ftn.isa.isa_project.proto;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class UploadEventProto {

    public static class UploadEvent {
        private long videoId;
        private String title;
        private long fileSize;
        private String authorUsername;
        private String videoPath;
        private String uploadedAt;

        private UploadEvent() {}

        public long getVideoId() { return videoId; }
        public String getTitle() { return title; }
        public long getFileSize() { return fileSize; }
        public String getAuthorUsername() { return authorUsername; }
        public String getVideoPath() { return videoPath; }
        public String getUploadedAt() { return uploadedAt; }

        public static Builder newBuilder() {
            return new Builder();
        }

        public byte[] toByteArray() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeLong(videoId);
            writeString(dos, title);
            dos.writeLong(fileSize);
            writeString(dos, authorUsername);
            writeString(dos, videoPath);
            writeString(dos, uploadedAt);

            dos.flush();
            return baos.toByteArray();
        }

        public static UploadEvent parseFrom(byte[] data) throws IOException {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);

            Builder builder = newBuilder();
            builder.setVideoId(dis.readLong());
            builder.setTitle(readString(dis));
            builder.setFileSize(dis.readLong());
            builder.setAuthorUsername(readString(dis));
            builder.setVideoPath(readString(dis));
            builder.setUploadedAt(readString(dis));

            return builder.build();
        }

        private static void writeString(DataOutputStream dos, String str) throws IOException {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }

        private static String readString(DataInputStream dis) throws IOException {
            int length = dis.readInt();
            byte[] bytes = new byte[length];
            dis.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        public static class Builder {
            private UploadEvent event = new UploadEvent();

            public Builder setVideoId(long videoId) {
                event.videoId = videoId;
                return this;
            }

            public Builder setTitle(String title) {
                event.title = title;
                return this;
            }

            public Builder setFileSize(long fileSize) {
                event.fileSize = fileSize;
                return this;
            }

            public Builder setAuthorUsername(String username) {
                event.authorUsername = username;
                return this;
            }

            public Builder setVideoPath(String path) {
                event.videoPath = path;
                return this;
            }

            public Builder setUploadedAt(String uploadedAt) {
                event.uploadedAt = uploadedAt;
                return this;
            }

            public UploadEvent build() {
                return event;
            }
        }
    }
}