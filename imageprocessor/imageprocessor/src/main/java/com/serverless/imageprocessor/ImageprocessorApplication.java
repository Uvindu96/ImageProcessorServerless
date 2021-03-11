package com.serverless.imageprocessor;

import com.sun.org.apache.bcel.internal.generic.ANEWARRAY;
import org.apache.commons.logging.Log;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ImageprocessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImageprocessorApplication.class, args);
	}

	public static PubSubReceiver newPub = new PubSubReceiver();


	@Bean
	public CommandLineRunner getCommandLiner() {
		return (args) -> {
			String PROJECT_ID = ServiceOptions.getDefaultProjectId();
			String SUBSCRIPTION_ID = "test-topic-sub";
			ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(PROJECT_ID, SUBSCRIPTION_ID);
			Subscriber subscriber = null ;
			Log log = LogFactory.getLog(ImageprocessorApplication.class);
			log.info(String.format("Project, %s", PROJECT_ID) );
			try {
				subscriber = Subscriber.newBuilder(subscriptionName, newPub).build() ;
				subscriber.startAsync().awaitRunning();
				subscriber.awaitTerminated();
				System.out.println(newPub.getGcsPath());
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		} ;
	}

}
