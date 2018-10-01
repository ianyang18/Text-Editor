package editor;

import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;

import java.util.Iterator;
import java.util.Stack;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/** An EventHandler to handle keys that get pressed. */
public class KeyEventHandler implements EventHandler<KeyEvent> {
    private Group root;
    private TextBuffer text;
    private Render render;
    private String fileName;

    private Stack<Action> undo;
    private Stack<Action> redo;

    public KeyEventHandler(final Group root, TextBuffer text, Render render, String fileName) {
        this.root = root;
        this.text = text;
        this.render = render;
        this.fileName = fileName;
        undo = new Stack<>();
        redo = new Stack<>();
    }

    @Override
    public void handle(KeyEvent keyEvent) {
        boolean shortCut = keyEvent.isShortcutDown();
        if (keyEvent.getEventType() == KeyEvent.KEY_TYPED) {
            String characterTyped = keyEvent.getCharacter();
            if (characterTyped.length() > 0 && characterTyped.charAt(0) != 8 && characterTyped.charAt(0) != 13
                    && !(shortCut && characterTyped.equals("=")) && !(shortCut && characterTyped.equals("-"))
                    && !(shortCut && characterTyped.equals("z")) && !(shortCut && characterTyped.equals("y"))
                    && !(shortCut && characterTyped.equals("s")) && !(shortCut && characterTyped.equals("p"))) {
                text.add(new Text(characterTyped));
                // All new Nodes need to be added to the root in order to be displayed.
                root.getChildren().add(text.getCurrentPos(), text.getCurrentNode().text);
                text.moveRight();

                // The undo stack size is 100, so we can undo up to 100 actions.
                // If we reach the limit of size, then we need to remove the first one and leave one space for the new one.
                if (undo.size() == 100) {
                    undo.remove(0);
                }
                undo.push(new Action(true, text.getCurrentPos(), text.getCurrentNode(), text.getCurrentNode().prev.text));
                redo.clear();

                render.renderAll();
                keyEvent.consume();
            }
        } else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
            KeyCode code = keyEvent.getCode();
            if (code == KeyCode.LEFT) {
                text.moveLeft();
                render.renderCursor();
                render.renderScrollBar();
            } else if (code == KeyCode.RIGHT) {
                text.moveRight();
                render.renderCursor();
                render.renderScrollBar();
            } else if (code == KeyCode.UP) {
                Iterator<Text> curTextIterator = text.descendingIterator(text.getCurrentPos());
                if (curTextIterator.hasNext()) {
                    // Declaring and initialization
                    Text curText, prevText;
                    prevText = text.getCurrentNode().prev.text;
                    curText = text.getCurrentNode().text;
                    double xCur, yCur;
                    // Record current position
                    if (text.getCurrentPos() == text.size()) {
                        // If the current position is at the end of the line.
                        if (!prevText.getText().equals(System.getProperty("line.separator"))) {
                            xCur = prevText.getX() + prevText.getLayoutBounds().getWidth();
                            yCur = prevText.getY();
                        } else {
                            // If the last text is the c/r character, then its current position is at the start of below line.
                            xCur = 5;
                            yCur = prevText.getY() + Math.round(render.getAutoHeight().getLayoutBounds().getHeight());
                        }
                        // To align the curTextIterator and the currentPos.
                        curText = curTextIterator.next();
                        text.moveLeft();
                    } else {
                        xCur = curText.getX();
                        yCur = curText.getY();
                    }

                    // Main task, split into two parts, currentPos in the first line or not!
                    if (yCur != 0) {
                        // move back to the front of the current line.
                        while (curText.getX() >= 5.0 && curText.getY() == yCur) {
                            text.moveLeft();
                            curText = curTextIterator.next();
                        }
                        // move back to the closet word in the above line, and stop at the start of the word in the first line.
                        while (text.getCurrentPos() > 0 && curText.getX() > xCur
                                && curText.getY() == yCur - Math.round(render.getAutoHeight().getLayoutBounds().getHeight())) {
                            text.moveLeft();
                            if (curTextIterator.hasNext()) {
                                curText = curTextIterator.next();
                            }
                        }
                        // If the xCur is over the middle position of closest two words, then it means we need to turn back.
                        // But if the current line is longer than above line, then we don't need to turn back.
                        // The long -> short line cases:
                        // 1. The short line would be ended in the space character (due to word cannot fit in the current line and cause line break event)
                        // 2. or c/r character.
                        if ((curText.getX() + (curText.getX() + curText.getLayoutBounds().getWidth())) / 2 < xCur
                                && !curText.getText().equals(" ")
                                && !curText.getText().equals(System.getProperty("line.separator"))) {
                            text.moveRight();
                        }
                    } else {
                        while (text.getCurrentPos() > 0 && curText.getX() >= 5.0) {
                            // If event happens in the fist line, then we move back to the start.
                            text.moveLeft();
                        }
                    }
                }
                render.renderCursor();
                render.renderScrollBar();
            } else if (code == KeyCode.DOWN) {
                Iterator<Text> curTextIterator = text.iterator(text.getCurrentPos());
                if (curTextIterator.hasNext()) {
                    // Declaring and initialization
                    Text curText, prevText;
                    prevText = text.get(text.getCurrentPos() - 1);
                    curText = curTextIterator.next();
                    double xCur, yCur;
                    // Record current position
                    if (text.getCurrentPos() == text.size()) {
                        // If the current position is at the end of the line.
                        xCur = prevText.getX() + prevText.getLayoutBounds().getWidth();
                        yCur = prevText.getY();
                    } else {
                        xCur = curText.getX();
                        yCur = curText.getY();
                    }

                    if (yCur != text.getLast().getY()) {
                        // move forward to the tail of the current line.
                        while (curText.getY() == yCur) {
                            text.moveRight();
                            curText = curTextIterator.next();
                        }
                        // move forward to the closet word in the below line.
                        // and if the below line is the last line, then we need to stop at the end of word.
                        while (text.getCurrentPos() < text.size() && curText.getX() <= xCur
                                && curText.getY() == yCur + Math.round(render.getAutoHeight().getLayoutBounds().getHeight())) {
                            text.moveRight();
                            if (curTextIterator.hasNext()) {
                                curText = curTextIterator.next();
                            }
                        }
                        // If the xCur is over the middle position of closest two words, then it means we need to turn back.
                        // But if the current line is longer than below line, then we don't need to turn back. i.e. long line -> short line case.
                        prevText = text.get(text.getCurrentPos() - 1);
                        if ((prevText.getX() + (prevText.getX() + Math.round(prevText.getLayoutBounds().getWidth()))) / 2 >= xCur
                                || prevText.getY() < curText.getY()
                                || prevText.getText().equals(System.getProperty("line.separator"))) {
                            text.moveLeft();
                        }
                    } else {
                        while (text.getCurrentPos() < text.size() && curText.getX() <= render.getContentWidth()) {
                            // If event happens in the last line, then we move forward to the end.
                            text.moveRight();
                        }
                    }
                }
                render.renderCursor();
                render.renderScrollBar();
            } else if (code == KeyCode.BACK_SPACE) {
                if (text.size() > 0 && text.getCurrentPos() > 0) {
                    Text removeChar = text.remove();
                    root.getChildren().remove(removeChar);

                    if (undo.size() == 100) {
                        undo.remove(0);
                    }
                    undo.push(new Action(false, text.getCurrentPos(), text.getCurrentNode(), removeChar));
                    redo.clear();

                    render.renderAll();
                }
            } else if (code == KeyCode.ENTER) {
                text.add(new Text(System.getProperty("line.separator")));
                root.getChildren().add(text.getCurrentPos(), text.getCurrentNode().text);
                text.moveRight();

                if (undo.size() == 100) {
                    undo.remove(0);
                }
                undo.push(new Action(true, text.getCurrentPos(), text.getCurrentNode(), text.getCurrentNode().prev.text));
                redo.clear();

                render.renderAll();
            } else if (shortCut && code == KeyCode.EQUALS) {
                render.updateFont(render.getFontSize() + 4);
                render.renderAll();
            } else if (shortCut && code == KeyCode.MINUS) {
                render.updateFont(Math.max(4, render.getFontSize() - 4));
                render.renderAll();
            } else if (shortCut && code == KeyCode.Z) {
                // Undo
                if (!undo.isEmpty()) {
                    Action action = undo.pop();
                    if (action.getAction()) {
                        removeBack(action, redo);
                    } else {
                        addBack(action, redo);
                    }
                    render.renderAll();
                }
            } else if (shortCut && code == KeyCode.Y) {
                // Redo
                if (!redo.isEmpty()) {
                    Action action = redo.pop();
                    if (action.getAction()) {
                        removeBack(action, undo);
                    } else {
                        addBack(action, undo);
                    }
                    render.renderAll();
                }
            } else if (shortCut && code == KeyCode.P) {
                System.out.println("Cursor Position: " + (int) render.getCursor().getX() + ", " + (int) render.getCursor().getY());
            } else if (shortCut && code == KeyCode.S) {
                try {
                    File inputFile = new File(fileName);
                    FileWriter writer = new FileWriter(inputFile);
                    Iterator<Text> curTextIterator = text.iterator();
                    Text t = curTextIterator.next();
                    char c;
                    int i = 0;
                    int size = text.size();
                    while (i < size) {
                        c = t.getText().charAt(0);
                        writer.write(c);
                        t = curTextIterator.next();
                        i += 1;
                    }
                    writer.close();
                } catch (IOException e) {
                    System.out.println(fileName + " can't be saved.");
                    System.out.println("Error message: " + e.getMessage());
                    System.exit(0);
                }
            }
        }
    }

    public void removeBack(Action action, Stack<Action> stack) {
        text.setCurrentNode(action.getNode());
        text.setCurrentPos(action.getPosition());
        Text removeChar = text.remove();
        root.getChildren().remove(removeChar);
        if (stack.size() == 100) {
            stack.remove(0);
        }
        stack.push(new Action(false, text.getCurrentPos(), text.getCurrentNode(), removeChar));
    }

    public void addBack(Action action, Stack<Action> stack) {
        text.setCurrentNode(action.getNode());
        text.setCurrentPos(action.getPosition());
        text.add(action.getText());
        root.getChildren().add(action.getPosition(), action.getText());
        text.moveRight();
        if (stack.size() == 100) {
            stack.remove(0);
        }
        stack.push(new Action(true, text.getCurrentPos(), text.getCurrentNode(), text.getCurrentNode().prev.text));
    }
}
