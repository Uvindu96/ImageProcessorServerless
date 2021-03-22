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


}
