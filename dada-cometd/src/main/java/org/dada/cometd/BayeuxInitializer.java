package org.dada.cometd;

import java.io.IOException;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.cometd.Bayeux;

public class BayeuxInitializer extends GenericServlet
{
    public void init() throws ServletException
    {
        Bayeux bayeux = (Bayeux)getServletContext().getAttribute(Bayeux.ATTRIBUTE);
        new HelloService(bayeux);
    }

    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException
    {
        throw new ServletException();
    }
}
