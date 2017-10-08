package com.example.helio.arduino.core;

import android.util.Log;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueManager {

    private static final String TAG = "QueueManager";

    private Queue<QueueItem> queue = new ConcurrentLinkedQueue<>();
    private ConnectionManager manager;
    private static QueueManager instance;

    private QueueManager() {
    }

    public static QueueManager getInstance() {
        if (instance == null) {
            instance = new QueueManager();
        }
        return instance;
    }

    public String insert(QueueItem item) {
        queue.offer(item);
        int size = queue.size();
        Log.d(TAG, "insert: " + size + ", manager " + manager);
        if (size == 1 && manager != null) {
            manager.writeMessage(item.getData());
        }
        return item.getUuId();
    }

    public void setManager(ConnectionManager manager) {
        if (this.manager == null) {
            this.manager = manager;
        }
    }

    public QueueItem getCurrent() {
        return queue.peek();
    }

    public QueueItem deQueue() {
        QueueItem item = queue.poll();
        QueueItem next = queue.peek();
        Log.d(TAG, "deQueue: " + queue.size());
        if (next != null) {
            manager.writeMessage(next.getData());
        }
        return item;
    }

    public void clearQueue() {
        queue.clear();
    }

    public void notifyQueue() {
        QueueItem item = getCurrent();
        Log.d(TAG, "notifyQueue: " + item + ", " + manager);
        if (item != null && manager != null) manager.writeMessage(item.getData());
    }
}