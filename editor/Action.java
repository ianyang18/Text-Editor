package editor;

import javafx.scene.text.Text;

/**
 * Record the current action state, such as add/delete character in certain position.
 */
public class Action {
    private boolean add;
    private int position;
    private TextBuffer.Node node;
    private Text text;

    public Action(boolean add, int curPos, TextBuffer.Node curNode, Text curText) {
        this.add = add;
        position = curPos;
        node = curNode;
        text = curText;
    }

    public boolean getAction() {
        return add;
    }

    public int getPosition() {
        return position;
    }

    public TextBuffer.Node getNode() {
        return node;
    }

    public Text getText() {
        return text;
    }
}
