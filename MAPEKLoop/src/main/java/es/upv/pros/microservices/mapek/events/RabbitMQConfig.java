package es.upv.pros.microservices.mapek.events;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQConfig {
	
		public static final String ID="rabbitmq";

		public static final String LOCALCHANGE_EXCHANGE="local_changes";
		
		public static Connection getconnection() throws IOException, TimeoutException{
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(BrokerConfig.getHost());
			factory.setPort(Integer.parseInt(BrokerConfig.getPort()));
			if(BrokerConfig.getVirtualHost()!=null) factory.setVirtualHost(BrokerConfig.getVirtualHost());
			if(BrokerConfig.getUser()!=null) factory.setUsername(BrokerConfig.getUser());
			if(BrokerConfig.getPassword()!=null) factory.setPassword(BrokerConfig.getPassword());
			Connection connection = factory.newConnection();
			return connection;
		}

}
