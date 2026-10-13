package gov.openoed.platform.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SqsQueuesTest {

	@Test
	void sendReceiveDelete() {
		SqsQueues queues = new SqsQueues();
		queues.send("office-actions", "{\"a\":1}");
		queues.send("office-actions", "{\"a\":2}");
		assertThat(queues.depth("office-actions")).isEqualTo(2);
		var batch = queues.receive("office-actions", 1);
		assertThat(batch).hasSize(1);
		assertThat(queues.depth("office-actions")).isEqualTo(1);
		assertThat(queues.delete("office-actions", batch.get(0).receiptHandle())).isTrue();
		assertThat(queues.delete("office-actions", "missing")).isFalse();
		assertThat(queues.receive("empty", 5)).isEmpty();
	}
}
