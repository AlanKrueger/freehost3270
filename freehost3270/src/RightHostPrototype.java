import jclass.bwt.JCProgressMeter;
import java.applet.Applet;
import java.awt.*;

        
class aboutFrame extends Frame
{
    aboutFrame()
    {
        super("About");
        setBackground(Color.lightGray);
        setLayout(new BorderLayout());
        add("North", new Label("RightHost 3270, (c)1998", Label.CENTER));
        add("Center", new Label("Licensed to the CIT Group", Label.CENTER));
        Panel p = new Panel();
        p.setLayout(new FlowLayout());
        p.add(new Button("OK"));
        add("South", p);
        resize(200, 200);
        Dimension screen_size, dlg;
        screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        dlg = this.size();
        move((screen_size.width - dlg.width)/2, (screen_size.height - dlg.height)/2);
        show();
    }
    public boolean action(Event evt, Object arg)
    {
        if(evt.target instanceof Button)
            dispose();
        return true;
    }
}
        
