package editor;

import javafx.scene.text.Text;
import java.util.HashMap;
import java.util.Iterator;

/** The data structure to store the text content. */
public class TextBuffer {
    private Node sentinel;
    private Node currentNode;
    private int currentPos;
    private int size;

    // Record the current line height for the first node in current line.
    private HashMap<Integer, Node> linesNode;
    private HashMap<Integer, Integer> linesPos;

    public class Node {
        public Node prev, next;
        public Text text;

        public Node(Node prev, Text text, Node next) {
            this.prev = prev;
            this.text = text;
            this.next = next;
        }
    }

    public TextBuffer() {
        sentinel = new Node(null, new Text(), null);
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
        currentNode = sentinel;
        size = 0;
        currentPos = 0;
        linesNode = new HashMap<>();
        linesPos = new HashMap<>();
    }

    public class TextBuffIterator implements Iterator<Text> {
        private Node front;
        public TextBuffIterator() {
            front = sentinel;
        }
        public boolean hasNext() {
            return front.next != null;
        }
        public Text next() {
            Node curText = front.next;
            front = front.next;
            return curText.text;
        }
    }

    public Iterator<Text> iterator() {
        return new TextBuffIterator();
    }

    public Iterator<Text> iterator(int pos) {
        Iterator<Text> tmp = iterator();
        int i = 0;
        while (i < pos) {
            tmp.next();
            i += 1;
        }
        return tmp;
    }

    public class TextBuffIteratorDescend implements Iterator<Text> {
        private Node tail;
        public TextBuffIteratorDescend() {
            tail = sentinel;
        }
        public boolean hasNext() {
            return tail.prev != null;
        }
        public Text next() {
            Node curText = tail.prev;
            tail = tail.prev;
            return curText.text;
        }
    }

    public Iterator<Text> descendingIterator() {
        return new TextBuffIteratorDescend();
    }

    public Iterator<Text> descendingIterator(int pos) {
        Iterator<Text> tmp = descendingIterator();
        int i = size - pos;
        while (i > 0) {
            tmp.next();
            i -= 1;
        }
        return tmp;
    }

    public void add(Text t) {
        if (size == currentPos) {
            addLast(t);
        } else {
            Node newNode = new Node(currentNode.prev, t, currentNode);
            currentNode.prev.next = newNode;
            currentNode.prev = newNode;
            currentNode = currentNode.prev;
            size += 1;
        }
    }

    public void addLast(Text t) {
        Node oldEnd = sentinel.prev;
        Node newEnd = new Node(oldEnd, t, sentinel);
        sentinel.prev= newEnd;
        sentinel.prev.prev.next = newEnd;
        currentNode = newEnd;
        size += 1;
    }

    public Text remove() {
        if (size > 0 && currentPos > 0) {
            Node removeNode = currentNode.prev;
            removeNode.prev.next = currentNode;
            currentNode.prev = removeNode.prev;
            size -= 1;
            currentPos -= 1;
            return removeNode.text;
        }
        return null;
    }

    public void moveLeft() {
        if (currentPos > 0) {
            currentNode = currentNode.prev;
            currentPos -= 1;
        }
    }

    public void moveRight() {
        if (currentPos < size) {
            currentNode = currentNode.next;
            currentPos += 1;
        }
    }

    public void recordLine(int y, int pos) {
        if (pos < size &&  pos >= 0) {
            if (linesNode.containsKey(y)) {
                linesNode.remove(y);
            }
            Node n = getNode(pos);
            linesNode.put(y, n);
            linesPos.put(y, pos);
        }
    }

    public Node getLineNode(int y) {
        return linesNode.get(y);
    }

    public int getLinePos(int y) {
        return linesPos.get(y);
    }

    public Text getFirst() {
        return sentinel.next.text;
    }

    public Node getLastNode() {
        return sentinel.prev;
    }

    public Text getLast() {
        return sentinel.prev.text;
    }

    public void setCurrentPos(int pos) {
        currentPos = pos;
    }

    public void setCurrentNode (Node node) {
        currentNode = node;
    }

    public Node getCurrentNode() {
        return currentNode;
    }

    public int getCurrentPos() {
        return currentPos;
    }

    public Text get(int pos) {
        Text item = new Text();
        if (pos >= 0 && pos < size) {
            item = getNode(pos).text;
        } else if (pos < 0) {
            item = getFirst();
        } else if (pos > size) {
            item = getLast();
        }
        return item;
    }

    private Node getNode(int pos) {
        if (pos < 0 || pos > size - 1) {
            return null;
        }
        Node p = sentinel.next;
        while (pos > 0) {
            p = p.next;
            pos -= 1;
        }
        return p;
    }

    public int size() {
        return size;
    }
}
