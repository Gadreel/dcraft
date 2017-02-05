package dcraft.tool.ui;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListener;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.ComponentRenderer;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import dcraft.api.ApiSession;
import dcraft.hub.ILocalCommandLine;

public class Configure implements ILocalCommandLine {
	@Override
	public void run(Scanner scan, ApiSession client) throws Exception {
        // Setup terminal and screen layers
        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();
        
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));

        gui.getBackgroundPane().setComponent(new EmptySpace(TextColor.ANSI.BLUE) {
            @Override
            protected ComponentRenderer<EmptySpace> createDefaultRenderer() {
                return new ComponentRenderer<EmptySpace>() {
                    @Override
                    public TerminalSize getPreferredSize(EmptySpace component) {
                        return TerminalSize.ONE;
                    }

                    @Override
                    public void drawComponent(TextGUIGraphics graphics, EmptySpace component) {
                        graphics.setForegroundColor(TextColor.ANSI.CYAN);
                        graphics.setBackgroundColor(TextColor.ANSI.BLUE);
                        graphics.setModifiers(EnumSet.of(SGR.BOLD));
                        graphics.fill(' ');
                        //graphics.putString(3, 0, "Text GUI in 100% Java");
                        
                        TerminalSize ssize = screen.getTerminalSize();
                        
                        int xoff = (ssize.getColumns() - 40) / 2;
                        int yoff = 1; //(ssize.getRows() - 6) /2;
                        
                        graphics.putString(xoff, yoff + 0, "       _                      __  _   ");
                 		graphics.putString(xoff, yoff + 1, "      | |                    / _|| |  ");
           				graphics.putString(xoff, yoff + 2, "    __| |  ___  _ __   __ _ | |_ | |_ ");
   						graphics.putString(xoff, yoff + 3, "   / _` | / __|| '__| / _` ||  _|| __|");
						graphics.putString(xoff, yoff + 4, "  | (_| || (__ | |   | (_| || |  | |_ ");
						graphics.putString(xoff, yoff + 5, "   \\__,_| \\___||_|    \\__,_||_|   \\__|");
                    }
                };
            }
        });        

        // Create window to hold the panel
        BasicWindow window = new BasicWindow();
        
        //window.setSize(new TerminalSize(50,25));
        //window.setHints(Arrays.asList(Window.Hint.CENTERED));
        
        //Window.Hint.

        TerminalSize ssize = screen.getTerminalSize();
        
        window.setPosition(new TerminalPosition((ssize.getColumns() - 42) / 2, 8));
        
        window.addWindowListener(new WindowListener() {			
			@Override
			public void onUnhandledInput(Window arg0, KeyStroke arg1, AtomicBoolean arg2) {
			}
			
			@Override
			public void onInput(Window arg0, KeyStroke arg1, AtomicBoolean arg2) {
			}
			
			@Override
			public void onResized(Window arg0, TerminalSize arg1, TerminalSize arg2) {
		        TerminalSize ssize = screen.getTerminalSize();
		        
		        window.setPosition(new TerminalPosition((ssize.getColumns() - 42) / 2, 8));
			}
			
			@Override
			public void onMoved(Window arg0, TerminalPosition arg1, TerminalPosition arg2) {
			}
		});
        
        // Create panel to hold components
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        
        ActionListBox actionListBox = new ActionListBox(new TerminalSize(40, 12));

        actionListBox.addItem("Option 1", new Runnable() {
            @Override
            public void run() {
            	System.out.println("opt 1");
            }
        });

        actionListBox.addItem("Option 2", new Runnable() {
            @Override
            public void run() {
            	System.out.println("opt 2");
            }
        });

        actionListBox.addItem("Option 3", new Runnable() {
            @Override
            public void run() {
            	System.out.println("opt 3");
            }
        });

        actionListBox.addItem("Quit", new Runnable() {
            @Override
            public void run() {
            	window.close();
            	
                try {
					screen.stopScreen();
				} 
                catch (IOException x) {
                	System.out.println("Unable to close console. Error: " + x);
				}
            }
        });
            
        panel.addComponent(actionListBox);
        
        /*
        panel.addComponent(new Label("Forename"));
        TextBox fbox = new TextBox().addTo(panel);

        panel.addComponent(new Label("Surname"));
        TextBox lbox = new TextBox().addTo(panel);

        panel.addComponent(new Label("Message"));
        Label lblOutput = new Label("").addTo(panel);

        panel.addComponent(new EmptySpace(new TerminalSize(0,0))); // Empty space underneath labels
        
        new Button("Submit", new Runnable() {
            @Override
            public void run() {
                lblOutput.setText("Hello: " + fbox.getText() + " " + lbox.getText());
            }
        }).addTo(panel);

        panel.addComponent(new EmptySpace(new TerminalSize(0,0))); // Empty space underneath labels
        
        new Button("Close", new Runnable() {
            @Override
            public void run() {
                try {
					screen.stopScreen();
				} 
                catch (IOException x) {
                	System.out.println("Unable to close console. Error: " + x);
				}
            }
        }).addTo(panel);
        */
        
        window.setComponent(panel);

        // Create gui and start gui
        gui.addWindowAndWait(window);
	}
}
