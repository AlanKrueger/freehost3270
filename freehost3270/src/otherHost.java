import jclass.bwt.JCProgressMeter;
import java.applet.Applet;
import java.awt.*;

public class otherHost extends Frame
{
    public otherHost()
    {
        super("Host Connect");
        response = -1;
        setBackground(Color.lightGray);
        setLayout(new BorderLayout());
        add("Center", host = new TextField());
        Panel p = new Panel();
        p.setLayout(new FlowLayout());
        p.add(ok = new Button("OK"));
        p.add(cancel = new Button("Cancel"));
        add("South", p);
        add("North", new Label("Enter a host to connect to:"));
        pack();
        Dimension screen_size, dlg;
        screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        dlg = this.size();
        move((screen_size.width - dlg.width)/2, (screen_size.height - dlg.height)/2);
        show();
        requestFocus();
    }
    
    public boolean action(Event evt, Object arg)
    {
        if(evt.target instanceof Button)
        {
            if(arg.equals("OK"))
            {
                response = 0;
                dispose();
            }
            if(arg.equals("Cancel"))
            {
                response = 1;
                dispose();
            }
            return true;
        }
        return false;
    }
    int response;
    TextField host;
    Button ok, cancel;
}
        

        
