package gov.openoed.platform.sqs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

/** In-memory SQS. A receive holds a receipt until delete; visibility is explicit. */
@Component
public class SqsQueues {

	public record Message(String messageId, String receiptHandle, String body, Instant sentAt) {
	}

	private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Message>> queues = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Message> inflight = new ConcurrentHashMap<>();

	public Message send(String queue, String body) {
		Message message = new Message(UUID.randomUUID().toString(), null, body, Instant.now());
		queues.computeIfAbsent(queue, q -> new ConcurrentLinkedDeque<>()).add(message);
		return message;
	}

	public List<Message> receive(String queue, int max) {
		ConcurrentLinkedDeque<Message> deque = queues.get(queue);
		List<Message> out = new ArrayList<>();
		if (deque == null || max <= 0) return out;
		while (out.size() < max) {
			Message next = deque.poll();
			if (next == null) break;
			String receipt = UUID.randomUUID().toString();
			Message held = new Message(next.messageId(), receipt, next.body(), next.sentAt());
			inflight.put(receipt, held);
			out.add(held);
		}
		return out;
	}

	public boolean delete(String queue, String receipt) {
		return inflight.remove(receipt) != null;
	}

	public int depth(String queue) {
		ConcurrentLinkedDeque<Message> deque = queues.get(queue);
		return deque == null ? 0 : deque.size();
	}

	public int inflight() {
		return inflight.size();
	}
}
