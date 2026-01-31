package com.commentary.security;

import com.crystaldecisions.sdk.framework.IEnterpriseSession;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener zur Freigabe von SAP BO Ressourcen bei Sitzungsende.
 * 
 * Dieser Listener wird aufgerufen, wenn:
 * 1. Eine neue HTTP-Session erstellt wird (sessionCreated)
 * 2. Eine HTTP-Session beendet wird (sessionDestroyed)
 * 
 * Die Hauptaufgabe besteht darin, die SAP BO Verbindung ordnungsgemäß
 * abzumelden (logoff), um Ressourcen auf der CMS-Seite freizugeben.
 */
public class SAPSessionListener implements HttpSessionListener {
    
    private static final Logger LOGGER = Logger.getLogger(SAPSessionListener.class.getName());
    private static final String SESSION_ATTR = "SAP_BO_SESSION";

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // Session wurde erstellt
        LOGGER.fine("HTTP-Session erstellt: " + se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        LOGGER.fine("HTTP-Session wird zerstört: " + se.getSession().getId());
        
        // Hole die SAP BO Sitzung aus der Tomcat-Session
        IEnterpriseSession session = (IEnterpriseSession) se.getSession()
                .getAttribute(SESSION_ATTR);
        
        if (session != null) {
            // Versuche die Sitzung ordnungsgemäß abzumelden
            try {
                LOGGER.fine("Melde Benutzer ID " + session.getUserInfo().getUserID() + 
                           " von SAP BO ab");
                session.logoff();
                LOGGER.fine("SAP BO Sitzung erfolgreich beendet");
            } catch (Exception e) {
                // Fehler beim Logoff sollten protokolliert, aber nicht geworfen werden
                // da dies einen Container-Fehler verursachen würde
                LOGGER.log(Level.WARNING, 
                    "Fehler beim Abmelden von SAP BO: " + e.getMessage(), e);
            }
        } else {
            LOGGER.finest("Keine SAP BO Sitzung zum Abmelden vorhanden");
        }
    }
}