package es.upv.pros.microservices.mapek.events;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@Component
public class EventManager {
	
	private EventCallback eventCallback;
	
	public void registerChangeListener(EventCallback eventCallback) throws IOException, TimeoutException{
		this.eventCallback=eventCallback;
		switch(BrokerConfig.getBrokerType()){
			case RabbitMQConfig.ID: rabbitmqRegisterChangeListener(); break;
		}
	}
	
	private void rabbitmqRegisterChangeListener() throws IOException, TimeoutException{	
		Connection connection = RabbitMQConfig.getconnection();	
		Channel channel = connection.createChannel();
		channel.exchangeDeclare(RabbitMQConfig.LOCALCHANGE_EXCHANGE, BuiltinExchangeType.DIRECT);
		String COLA_CONSUMER = channel.queueDeclare().getQueue();
		channel.queueBind(COLA_CONSUMER, RabbitMQConfig.LOCALCHANGE_EXCHANGE,"");

		Consumer consumer = new DefaultConsumer(channel) {
			 @Override
			 public void handleDelivery(String consumerTag, Envelope envelope, 
					 					AMQP.BasicProperties properties, byte[] body) throws IOException {
				 EventManager.this.eventCallback.execute(new String(body));
			 }
		 };
		channel.basicConsume(COLA_CONSUMER, true, consumer);
	}
	
}
