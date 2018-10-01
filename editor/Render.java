package editor;

import javafx.scene.Group;
import javafx.geometry.VPos;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.control.ScrollBar;
import javafx.geometry.Orientation;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.Iterator;

/** Text editor layout render engine. */
public class Render {
    private int WINDOW_WIDTH;
    private int WINDOW_HEIGHT;
    private int MAX_LINE_WIDTH;

    private Group root;
    private Group textRoot;
    private TextBuffer text;
    private Cursor c;

    private ScrollBar sb;

    private int fontSize = 12;
    private static String fontName = "Verdana";
    private int spaceIndex;

    // To set the cursor initial height
    private Text autoHeight;

    public Render(final Group root, final Group textRoot, TextBuffer text, int window_width, int window_height) {
        WINDOW_WIDTH = window_width;
        WINDOW_HEIGHT = window_height;
        autoHeight = new Text();
        autoHeight.setTextOrigin(VPos.TOP);
        autoHeight.setFont(Font.font(fontName,  fontSize));

        this.root = root;
        this.textRoot = textRoot;
        this.text = text;
        this.spaceIndex = -1;

        // Initialize the cursor
        c = new Cursor();
        c.setX(5);
        c.setY(0);
        c.setHeight(autoHeight.getLayoutBounds().getHeight());
        textRoot.getChildren().add(c);

        // Scroll bar part
        // Make a vertical scroll bar on the right side of the screen.
        sb = new ScrollBar();
        sb.setOrientation(Orientation.VERTICAL);
        // Set the height of the scroll bar so that it fills the whole window.
        int usableScreenWidth = (int) (WINDOW_WIDTH - Math.round(sb.getLayoutBounds().getWidth()));
        sb.setLayoutX(usableScreenWidth);
        sb.setPrefHeight(window_height);
        // Set the range of the scroll bar.
        sb.setMin(0);
        sb.setMax(0);
        root.getChildren().add(sb);

        // Scroll bar listener
        // When the scroll bar changes position, change the content display.
        sb.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                Text lastWord = text.getLast();
                int height = (int) (lastWord.getY() + Math.round(lastWord.getLayoutBounds().getHeight()));
                // When scroll bar runs to the end, the last line of file is also in the bottom of the window,
                // therefore, the total movement is (height - window_height) pixels.
                // In addition, scroll bar height is equal to window_height,
                // so the portion movement in scroll bar is equal to the real shift pixels divided by (height - window_height).
                int shift = (int) ((double) newValue * (height - getWindowHeight()) / (double) getWindowHeight());
                // Change the content display
                textRoot.setLayoutY(-shift);
            }
        });
    }

    public void renderAll() {
        MAX_LINE_WIDTH = (int) (WINDOW_WIDTH - 10 - Math.round(sb.getLayoutBounds().getWidth()));
        renderContent();
        renderCursor();
        renderScrollBar();
    }

    public void renderContent() {
        // Initialization part
        Iterator<Text> curTextIterator = text.iterator();
        Iterator<Text> prevTextIterator = text.iterator();
        Text curText, prevText;
        spaceIndex = -1;
        int size = text.size();
        int xPos;

        // Set the text position.
        for (int i = 0; i < size; i+=1) {
            curText = curTextIterator.next();
            // Set the first character font size and font family.
            curText.setTextOrigin(VPos.TOP);
            curText.setFont(Font.font(fontName, fontSize));

            if (i == 0) {
                // Display the text in a window with a top and bottom margin of 0,
                // and a left and right margin of 5 pixels.
                xPos = 5;
                prevText = curText;
                // Record the first charcter in first line y's position.
                text.recordLine(0, 0);
            } else {
                prevText = prevTextIterator.next();
                xPos = (int) (prevText.getX() + Math.round(prevText.getLayoutBounds().getWidth()));
            }

            if (curText.getText().equals(" ")) {
                spaceIndex = i;
            }

            // If we add new character (exclude carriage/return character) and causes current position x in line over the window_width range,
            // then it will trigger wrapping line event.
            if (xPos + Math.round(curText.getLayoutBounds().getWidth()) > MAX_LINE_WIDTH
                && !curText.getText().equals(System.getProperty("line.separator"))) {
                wrapLine(curText, i);
            }
            else if (prevText.getText().equals(System.getProperty("line.separator"))) {
                // If the previous text is Enter (c/r) character, then we move into the next line.
                curText.setX(5);
                if (prevText == curText) {
                    // If we add the Enter node in the first (current) text of file.
                    curText.setY(0);
                } else {
                    // The Enter node boundary height is twice than normal text, so we set it as autoheight.
                    curText.setY(prevText.getY() + Math.round(autoHeight.getLayoutBounds().getHeight()));
                }
                text.recordLine((int) curText.getY(), i);
            } else {
                curText.setX(xPos);
                if (prevText == curText) {
                    // If the first (previous) text is Enter node and we remove it,
                    // then we need update the current position node to be the first text position.
                    curText.setY(0);
                } else {
                    curText.setY((int) prevText.getY());
                }
            }
        }
    }

    public void renderScrollBar() {
        Text lastWord = text.getLast();
        int bottomHeight = (int) (lastWord.getY() + Math.round(lastWord.getLayoutBounds().getHeight()));
        if (bottomHeight > WINDOW_HEIGHT) {
            sb.setMax(WINDOW_HEIGHT);
            int curHeight = (int) (c.getY() + Math.round(autoHeight.getLayoutBounds().getHeight()));
            if (curHeight + textRoot.getLayoutY() > WINDOW_HEIGHT) {
                sb.setValue((double) (curHeight - WINDOW_HEIGHT) * WINDOW_HEIGHT / (double) (bottomHeight - WINDOW_HEIGHT));
            } else if (c.getY() + textRoot.getLayoutY() < 0) {
                sb.setValue(c.getY() * WINDOW_HEIGHT / (double) (bottomHeight - WINDOW_HEIGHT));
            }
        } else {
            sb.setMax(0);
            sb.setValue(0);
        }
    }

    public void renderCursor() {
        int curPos = text.getCurrentPos();

        // Set the cursor position.
        // The cursor will cover the first vertical line of pixels in current node.
        if (curPos > 0) {
            Text curText = text.get(curPos);
            Text prevText = text.get(curPos - 1);
            int cursorX = (int) (prevText.getX() + Math.round(prevText.getLayoutBounds().getWidth()));

            if (curPos != text.size()) {
                c.setX(curText.getX());
                c.setY(curText.getY());
            } else {
                // Due to the Enter (c/r) is used to add at the end of line,
                // if it is appended at the end of file, then the cursor need to adjust its position.
                if (prevText.getText().equals(System.getProperty("line.separator"))) {
                    c.setX(5);
                    c.setY(prevText.getY() + Math.round(autoHeight.getLayoutBounds().getHeight()));
                } else {
                    c.setX(cursorX);
                    c.setY(prevText.getY());
                }
            }

            // Set the cursor height same as the previous text.
            if (!prevText.getText().equals(System.getProperty("line.separator"))) {
                c.setHeight(Math.round(prevText.getLayoutBounds().getHeight()));
            }
        } else {
            c.setX(5);
            c.setY(0);
        }
    }

    // When wrapping line events happen, there are three possible situations that will trigger event.
    // 1. Adding character in long word.
    // 2. Adding character.
    // 3. Adding whitespace.
    // Wrapping line method will rearrange the current node x, y position.
    public void wrapLine(Text curText, int curPos) {
        int prevPos = curPos -1;
        Text prevText = text.get(prevPos);
        // spaceText is used to compare to the prevText to check whether these two in the same line.
        Text spaceText = text.get(spaceIndex);

        if (spaceIndex == -1 || (spaceText.getY() < prevText.getY() && !prevText.getText().equals(" ") && !curText.getText().equals(" "))) {
            // If the long word is as long as the window_width, then the line should break in the middle of the word.
            // For two cases:
            // 1. long word beginning at the start of file.
            // 2. long word generating in the middle of file. The last whitespace and added character is in different line.
            curText.setX(5);
            curText.setY((int) (prevText.getY() + Math.round(curText.getLayoutBounds().getHeight())));
            text.recordLine((int) curText.getY(), curPos);
        } else if (spaceText.getY() == prevText.getY() && !prevText.getText().equals(" ") && !curText.getText().equals(" ")) {
            // If the normal length word is not finished in a line, then need to move to the next line.
            // The last whitespace and added character is in the same line.
            int lastWordPosInLine = spaceIndex + 1;
            Iterator<Text> curTextIterator = text.iterator(lastWordPosInLine);
            Iterator<Text> prevTextIterator = text.iterator(lastWordPosInLine);
            Text curChar, prevChar;

            // Setting the word's remaining characters position.
            for (int j = lastWordPosInLine; j <= curPos; j += 1) {
                curChar = curTextIterator.next();
                if (j == lastWordPosInLine) {
                    curChar.setX(5);
                    curChar.setY((int) (curChar.getY() + Math.round(curChar.getLayoutBounds().getHeight())));
                    text.recordLine((int) curChar.getY(), lastWordPosInLine);
                } else {
                    prevChar = prevTextIterator.next();
                    curChar.setX((int) (prevChar.getX() + Math.round(prevChar.getLayoutBounds().getWidth())));
                    curChar.setY((int) prevChar.getY());
                }
            }
        } else if (spaceIndex == curPos || (!prevText.getText().equals(" ") && curText.getText().equals(" "))) {
            // If whitespace is added and cause the break line event, then stay at the same line.
            // Two cases: char + whitespace, whitespace + whitespace
            curText.setX((int) (prevText.getX() + Math.round(prevText.getLayoutBounds().getWidth())));
            curText.setY((int) prevText.getY());
        } else if (prevText.getText().equals(" ") && !curText.getText().equals(" ")) {
            // If the whitespace is the last word at the end of line,
            // and cause the break line event after adding the new word,
            // then reallocate the new word in the next line.
            // Case: whitespace + char
            curText.setX(5);
            curText.setY((int) (prevText.getY() + Math.round(curText.getLayoutBounds().getHeight())));
            text.recordLine((int) curText.getY(), curPos);
        }
    }

    public void updateFont(int newFontSize) {
        fontSize = newFontSize;
        autoHeight.setFont(Font.font(fontName, newFontSize));
    }

    public void updateWindowWidth(double width) {
        updateWindowSize(width, -1);
    }

    public void updateWindowHeight(double height) {
        updateWindowSize(-1, height);
    }

    public void updateWindowSize(double width, double height) {
        if (width < 0) {
            WINDOW_HEIGHT = (int) height;
            sb.setPrefHeight(WINDOW_HEIGHT);
            sb.setMin(0);
        } else if (height < 0) {
            WINDOW_WIDTH = (int) width;
            int usableScreenWidth = (int) (WINDOW_WIDTH - Math.round(sb.getLayoutBounds().getWidth()));
            sb.setLayoutX(usableScreenWidth);
        }
        renderAll();
    }

    public int getFontSize() {
        return fontSize;
    }

    public Text getAutoHeight() {
        return autoHeight;
    }

    public int getWindowHeight() {
        return WINDOW_HEIGHT;
    }

    public int getContentWidth() {
        return MAX_LINE_WIDTH;
    }

    public Cursor getCursor() {
        return c;
    }
}
