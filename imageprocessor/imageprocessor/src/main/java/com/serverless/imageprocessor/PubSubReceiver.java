package com.serverless.imageprocessor;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;


@Component
public class PubSubReceiver implements MessageReceiver {

    public String gcsPath;

    public String fileName;

    public String getGcsPath() {
        return gcsPath;
    }

    public void setGcsPath(String gcsPath) {
        this.gcsPath = gcsPath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private Log log = LogFactory.getLog(PubSubReceiver.class);
    @Override
    public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer ackReplyConsumer) {
        log.info(pubsubMessage.getData());
        log.info("ID :" + pubsubMessage.getMessageId());
        log.info("Data :" + pubsubMessage.getData());

        String pubSubMessage = pubsubMessage.getData().toStringUtf8();

        pubsubMessage
                .getAttributesMap()
                .forEach((key, value) -> System.out.println(key + " = " + value));

        String getBucketName = pubsubMessage.getAttributesMap().get("bucketId");
        String getObjectName = pubsubMessage.getAttributesMap().get("objectId");

            gcsPath = String.format("gs://%s/%s", getBucketName, getObjectName);
            System.out.println(String.format("Analyzing %s", getObjectName));
            System.out.println(String.format("Analyzing %s", gcsPath));
            setGcsPath(gcsPath);
            setFileName(getObjectName);
            ackReplyConsumer.ack();
        }
}
