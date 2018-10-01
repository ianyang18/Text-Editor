package editor;

import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

/** An EventHandler to handle mouse that click. */
public class MouseEventHandler implements EventHandler<MouseEvent> {
    private Group root;
    private TextBuffer text;
    private Render render;

    public MouseEventHandler(final Group root, TextBuffer text, Render render) {
        this.root = root;
        this.text = text;
        this.render = render;
    }

    @Override
    public void handle(MouseEvent mouseEvent) {
        if (mouseEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
            // Use current word position as the reference coordinate.
            TextBuffer.Node curLineFirstNode;
            if (text.getCurrentPos() == text.size()) {
                curLineFirstNode = text.getLineNode((int) text.getCurrentNode().prev.text.getY());
            } else {
                curLineFirstNode = text.getLineNode((int) text.getCurrentNode().text.getY());
            }

            Text curLineFirstText = curLineFirstNode.text;

            // The current line bottom boundary
            int curLineBBoundary;
            if (curLineFirstText.getText().equals(System.getProperty("line.separator"))) {
                curLineBBoundary = (int) (curLineFirstText.getY() + Math.round(render.getAutoHeight().getLayoutBounds().getHeight()));
            } else {
                curLineBBoundary = (int) (curLineFirstText.getY() + Math.round(curLineFirstText.getLayoutBounds().getHeight()));
            }

            // Record current position
            int mousePressedX = (int) mouseEvent.getX();
            int mousePressedY = (int) mouseEvent.getY() + (-1) * (int) root.getLayoutY();

            // Verify the mouse click event happened in the above or below the cursor line.
            if (!(curLineFirstText.getY() < mousePressedY && mousePressedY < curLineBBoundary)) {
                if (curLineFirstText.getY() > mousePressedY) {
                    // mouse click event is above the current cursor line
                    // Up search to the mouse click position
                    while (curLineFirstText.getY() > mousePressedY) {
                        if (curLineFirstText.getText().equals(System.getProperty("line.separator"))) {
                            curLineBBoundary = (int) (curLineFirstText.getY() - Math.round(render.getAutoHeight().getLayoutBounds().getHeight()));
                        } else {
                            curLineBBoundary = (int) (curLineFirstText.getY() - Math.round(curLineFirstText.getLayoutBounds().getHeight()));
                        }
                        curLineFirstNode = text.getLineNode(curLineBBoundary);
                        curLineFirstText = curLineFirstNode.text;
                    }
                } else {
                    // mouse click event is below the current cursor line
                    // Down search to the mouse click position
                    while (curLineFirstText.getY() < mousePressedY && curLineBBoundary < mousePressedY && curLineFirstText.getY() < text.getLast().getY()) {
                        curLineFirstNode = text.getLineNode(curLineBBoundary);
                        curLineFirstText = curLineFirstNode.text;
                        if (curLineFirstText.getText().equals(System.getProperty("line.separator"))) {
                            curLineBBoundary = (int) (curLineFirstText.getY() + Math.round(render.getAutoHeight().getLayoutBounds().getHeight()));
                        } else {
                            curLineBBoundary = (int) (curLineFirstText.getY() + Math.round(curLineFirstText.getLayoutBounds().getHeight()));
                        }
                    }
                }
            }

            // Search from the beginning of each line.
            TextBuffer.Node curLineNode = curLineFirstNode;
            Text curLineText = curLineNode.text;
            int curLinePos = text.getLinePos((int) curLineText.getY());

            while (curLineText.getX() < mousePressedX  && curLineFirstText.getY() == curLineText.getY()) {
                curLineNode = curLineNode.next;
                curLineText = curLineNode.text;
                curLinePos += 1;
            }

            // Due to the above search might cause overhead in one turn, so need to check.
            // And here we have two situation: in the same line, and otherwise (encounter c/r or end of file character).
            if (curLineText.getY() == curLineFirstText.getY()) {
                // If we are in the same line, then we need to check which text is close to the mouse click.
                // Case: In the same line with two possible cases: normal case and edge case (e.g. at the bottom line)
                if (mousePressedY > text.getLast().getY() + render.getAutoHeight().getLayoutBounds().getHeight()
                        && text.getLast().getText().equals(System.getProperty("line.separator"))) {
                    // In the bottom line and the last character is c/r,
                    // the cursor should not move when the mousePressedX changed.
                    curLineNode = text.getLastNode().next;
                    curLinePos = text.size();
                } else {
                    if ((curLineText.getX() - mousePressedX) > (mousePressedX - curLineNode.prev.text.getX())) {
                        curLineNode = curLineNode.prev;
                        curLinePos -= 1;
                    }
                }
            } else if (curLineText.getText().equals("")) {
                // Case: '""' is the end of file character, its x, y position is (0, 0).
                // If the last character is c/r and mousePressedY click at that line, then we need to turn back.
                if (text.getLast().getText().equals(System.getProperty("line.separator"))) {
                    if (curLineFirstText.getY() < mousePressedY && mousePressedY < curLineBBoundary) {
                        curLineNode = curLineNode.prev;
                        curLinePos -= 1;
                    }
                }
            } else {
                // Case: c/r
                // If we traverse overhead (e.g. curLineText line is below curLineFirstText line), then we need to turn back.
                curLineNode = curLineNode.prev;
                curLinePos -= 1;
            }

            text.setCurrentNode(curLineNode);
            text.setCurrentPos(curLinePos);
            render.renderCursor();
            render.renderScrollBar();
        }
    }
}
