package p2p.research.ui;

import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class SWTUI {

    public Display display;
    public Shell shell;

    public SWTUI(int width, int height) {
        display = new Display();
        shell = new Shell(display);
        shell.setSize(width, height);
        shell.setLocation(150, 50);
    }

    public void setPaintListener(PaintListener paintListener) {
        shell.addPaintListener(paintListener);
    }

    public void show() {
        shell.open();

        // run the event loop as long as the window is open
        while (!shell.isDisposed()) {
            // read the next OS event queue and transfer it to a SWT event
            if (!display.readAndDispatch())
            {
                // if there are currently no other OS event to process
                // sleep until the next OS event is available
                display.sleep();
            }
        }

        // disposes all associated windows and their components
        display.dispose();
    }

    public void refresh() {
        display.syncExec(()->shell.redraw());
    }
}
