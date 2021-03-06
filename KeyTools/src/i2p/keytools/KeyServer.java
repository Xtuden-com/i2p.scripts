package i2p.keytools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.i2p.I2PException;
import net.i2p.client.I2PSessionException;
import net.i2p.data.DataFormatException;

public class KeyServer extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {        
        final String[] path = req.getRequestURI().split("/");
        // but you can't tell if it ends in a / ?
        //System.err.println("Path: "+Tools.join(" / ",path));
        if(path.length < 2) {
            resp.sendRedirect("/keys/");
            return;
        }

        final String info;
        if(path.length==2)
            info = null;
        else
            info = path[2];

        if(info==null) {
            showIndex(req,resp);
            return;
        } 
            
        System.err.println("Looking for "+info);
        if(info.length()<=Tools.NAME_LENGTH) {
            PublicKey key;
            try {
                key = KeyFinder.find(info);
            } catch (DataFormatException ex) {
                Logger.getLogger(KeyServer.class.getName()).log(Level.SEVERE, null, ex);
                resp.sendError(500,"Impossible error "+info);
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                resp.sendError(404,"Key not found");
                return;
            } catch (I2PException ex) {
                Logger.getLogger(KeyServer.class.getName()).log(Level.SEVERE, null, ex);
                resp.sendError(500,"I2P is fucking with me");
                throw new RuntimeException(ex);
            }
            // XXX: this never really happens...
            if(key==null) {
                resp.sendError(404,"Key not found "+info);
                return;
            }
            resp.setContentType("i2p/destination");
            resp.setContentLength(key.dest.size());
            resp.setStatus(200);
            OutputStream out = null;
            try {
                out = resp.getOutputStream();
                try {
                    key.export(out);
                } catch (DataFormatException ex) {
                    Logger.getLogger(KeyServer.class.getName()).log(Level.SEVERE, null, ex);
                    out.write("This is an impossible error. It can never happen.".getBytes());
                }                
            } finally {
                if(out != null)
                    out.close();
            }
            return;
        }

        resp.sendError(500,"Wrong length! "+info.length()+" > "+Tools.NAME_LENGTH);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(500, "Please don't give me that.");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.err.println("YAY GOT PUT");
        InputStream in = null;
        try {
            in = req.getInputStream();
            KeyFinder.importKey(in);
        } catch (DataFormatException ex) {
            Logger.getLogger(KeyServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(KeyServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (I2PSessionException ex) {
            Logger.getLogger(KeyServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (I2PException ex) {
            Logger.getLogger(KeyServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch(IOException ex) {
            Logger.getLogger(KeyServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if(in != null)
                in.close();
        }
    }

    private void showIndex(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = null;
        try {
            out = resp.getWriter();
            out.println("<html><head><title>Key Server</title></head><body>");
            out.println("<h1>Welcome to a keyserver!</h1><p>The following keys are known.</p>");
            String[] names = KeyFinder.location.list(NoDots.instance);
            if(names == null || names.length==0) {
                out.println("<p><i>No known keys, sorry.</i></p>");
            } else {
                out.println("<ul>");
                for(String name : KeyFinder.location.list(NoDots.instance)) {
                    out.print("<li><a href=\"");
                    out.print(name);
                    out.print("\">");
                    out.print(name);
                    out.println("</a></li>");
                }
                out.println("</ul>");
            }
            out.println("</body></html>");
        } finally {
            if (out != null) {
                out.close();
            }
        }

    }
}
