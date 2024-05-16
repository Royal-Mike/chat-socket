import javax.swing.*;

public class ChatPanelComp {
    protected JPanel panelMsg;
    protected JScrollPane paneMsg;
    protected JScrollBar paneMsgScroll;
    public ChatPanelComp(JPanel p, JScrollPane sp, JScrollBar sb) {
        panelMsg = p;
        paneMsg = sp;
        paneMsgScroll = sb;
    }
}
