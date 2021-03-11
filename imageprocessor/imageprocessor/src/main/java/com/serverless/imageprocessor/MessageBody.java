package com.serverless.imageprocessor;

public class MessageBody {

    private Message message;

    public MessageBody() {}

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public class Message {

        private String messageId;
        private String publishTime;
        private String data;

        public Message() {}

        public Message(String messageId, String publishTime, String data) {
            this.messageId = messageId;
            this.publishTime = publishTime;
            this.data = data;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getPublishTime() {
            return publishTime;
        }

        public void setPublishTime(String publishTime) {
            this.publishTime = publishTime;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
