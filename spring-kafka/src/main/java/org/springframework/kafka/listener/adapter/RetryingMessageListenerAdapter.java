/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.kafka.listener.adapter;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.kafka.KafkaException;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * A retrying message listener adapter for {@link MessageListener}s.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 *
 * @author Gary Russell
 *
 */
public class RetryingMessageListenerAdapter<K, V> extends AbstractRetryingMessageListenerAdapter<K, V>
		implements MessageListener<K, V> {

	private final MessageListener<K, V> delegate;

	/**
	 * Construct an instance with the provided template and delegate. The exception will
	 * be thrown to the container after retries are exhausted.
	 * @param messageListener the delegate listener.
	 * @param retryTemplate the template.
	 */
	public RetryingMessageListenerAdapter(MessageListener<K, V> messageListener, RetryTemplate retryTemplate) {
		this(messageListener, retryTemplate, null);
	}

	/**
	 * Construct an instance with the provided template, callback and delegate.
	 * @param messageListener the delegate listener.
	 * @param retryTemplate the template.
	 * @param recoveryCallback the recovery callback; if null, the exception will be
	 * thrown to the container after retries are exhausted.
	 */
	public RetryingMessageListenerAdapter(MessageListener<K, V> messageListener, RetryTemplate retryTemplate,
			RecoveryCallback<Void> recoveryCallback) {
		super(retryTemplate, recoveryCallback);
		Assert.notNull(messageListener, "'messageListener' cannot be null");
		this.delegate = messageListener;
	}

	@Override
	public void onMessage(final ConsumerRecord<K, V> record) {
		getRetryTemplate().execute(new RetryCallback<Void, KafkaException>() {

			@Override
			public Void doWithRetry(RetryContext context) throws KafkaException {
				context.setAttribute(CONTEXT_RECORD, record);
				RetryingMessageListenerAdapter.this.delegate.onMessage(record);
				return null;
			}

		}, getRecoveryCallback());
	}

}
