package com.commentary.security;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.framework.ISessionMgr;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Filter zur Authentifizierung gegen SAP BusinessObjects und Rollenprüfung.
 * 
 * Dieser Filter:
 * 1. Prüft ob der Benutzer eine gültige SAP BO Sitzung hat
 * 2. Extrahiert den Logon-Token aus URL-Parametern oder Cookies
 * 3. Erstellt eine neue SAP BO Sitzung basierend auf dem Token
 * 4. Speichert die Sitzung in der HTTP-Session des Tomcat
 * 5. Prüft die erforderliche Benutzerrolle ZPB_S.GF_COMMENT_BO
 * 6. Leitet nicht autorisierte Benutzer auf die BI Launchpad Login-Seite um
 */
public class SAPAuthFilter implements Filter {
    
    private static final Logger LOGGER = Logger.getLogger(SAPAuthFilter.class.getName());
    
    private static final String SESSION_ATTR = "SAP_BO_SESSION";
    private static final String REQUIRED_ROLE = "ZPB_S.GF_COMMENT_BO";
    
    // Timeout für Versuche der Verbindung zum CMS in Millisekunden
    private static final long CMS_CONNECTION_TIMEOUT = 5000;
    
    // Maximale Länge für einen gültigen Token
    private static final int MIN_TOKEN_LENGTH = 20;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialisierung des Filters
        LOGGER.info("SAPAuthFilter initialisiert");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Sitzung von Tomcat erstellen/abrufen
        HttpSession httpSession = httpRequest.getSession(true);
        
        try {
            // Die Prüfung für die SDK-Testseite und Health-Endpoints überspringen
            if (isPublicEndpoint(httpRequest)) {
                LOGGER.finest("Öffentlicher Endpoint aufgerufen: " + httpRequest.getRequestURI());
                chain.doFilter(request, response);
                return;
            }
            
            // Prüfung, ob bereits eine valide Sitzung im HTTP-Session-Kontext existiert
            IEnterpriseSession enterpriseSession = (IEnterpriseSession) httpSession.getAttribute(SESSION_ATTR);
            
            // Wenn keine valide BO-Session existiert, versuche Token-Login
            if (enterpriseSession == null || !isSessionAlive(enterpriseSession)) {
                LOGGER.fine("Keine gültige SAP BO Sitzung vorhanden. Versuche Token-basierte Anmeldung.");
                
                String token = extractLogonToken(httpRequest);
                
                if (token == null || token.trim().isEmpty()) {
                    LOGGER.warning("Kein gültiger Logon-Token gefunden. Benutzer wird zur Login-Seite weitergeleitet.");
                    httpResponse.sendRedirect("/BOE/BI");
                    return;
                }
                
                // Token-basierte Anmeldung bei SAP BO
                enterpriseSession = performTokenLogin(token, httpResponse);
                if (enterpriseSession == null) {
                    // performTokenLogin hat bereits die Fehlerantwort gesendet
                    return;
                }
                
                // Sitzung in Tomcat HTTP-Session speichern, um Wiederanmeldungen zu vermeiden
                httpSession.setAttribute(SESSION_ATTR, enterpriseSession);
                LOGGER.info("Neue SAP BO Sitzung für Benutzer ID: " + 
                    enterpriseSession.getUserInfo().getUserID() + " erstellt.");
            }
            
            // Überprüfung der erforderlichen Benutzerrolle
            if (hasRequiredUserRole(enterpriseSession)) {
                LOGGER.fine("Benutzer hat erforderliche Rolle: " + REQUIRED_ROLE);
                // Zugriff gewährt, Anfrage weiterleiten
                chain.doFilter(request, response);
            } else {
                // Zugriff verweigern, wenn die Rolle fehlt
                int userId = enterpriseSession.getUserInfo().getUserID();
                LOGGER.warning("Benutzer ID " + userId + " hat nicht die erforderliche Rolle: " + REQUIRED_ROLE);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, 
                    "Zugriff verweigert: Fehlende Rolle " + REQUIRED_ROLE);
            }
            
        } catch (SDKException e) {
            // Fehlerbehandlung bei SDK-Problemen
            LOGGER.log(Level.SEVERE, "SAP BO SDK Fehler: " + e.getMessage(), e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Fehler bei der SAP BO Authentifizierung");
        }
    }

    /**
     * Prüft, ob der angeforderte Endpoint öffentlich erreichbar sein soll.
     * Dies sollte nur für Gesundheits- und Status-Checks verwendet werden.
     */
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Nur im Development-Modus erlaubt (sollte in Production deaktiviert sein)
        return false; // In Production immer false halten
    }

    /**
     * Prüft, ob die SAP BO Sitzung noch aktiv ist.
     * Dies wird durchgeführt, indem eine einfache Operation ausgeführt wird.
     * Wenn die Sitzung abgelaufen ist, wird eine SDKException geworfen.
     */
    private boolean isSessionAlive(IEnterpriseSession session) {
        if (session == null) {
            return false;
        }
        
        try {
            // Eine einfache Abfrage, die fehlschlägt, wenn die Sitzung ungültig ist
            int userId = session.getUserInfo().getUserID();
            LOGGER.finest("Sitzung ist noch aktiv. Benutzer ID: " + userId);
            return true;
        } catch (SDKException e) {
            LOGGER.log(Level.WARNING, "Sitzung ist nicht mehr aktiv oder ungültig: " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unerwarteter Fehler bei der Sitzungsprüfung: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extrahiert den Logon-Token aus URL-Parametern oder Cookies.
     * 
     * Suchfolge:
     * 1. URL-Parameter 'sToken'
     * 2. Cookie 'LogonToken'
     * 3. Cookie 'MYSAPSSO2'
     * 4. Cookie 'InfoView_WEBAPP_TOKEN'
     * 
     * @return Token-String oder null wenn nicht gefunden
     */
    private String extractLogonToken(HttpServletRequest request) {
        // Suche nach dem Token im URL-Parameter 'sToken'
        String token = request.getParameter("sToken");
        
        if (token != null && !token.trim().isEmpty()) {
            LOGGER.fine("Token aus URL-Parameter 'sToken' extrahiert");
            return token.trim();
        }
        
        // Suche in den Cookies, falls kein URL-Parameter gefunden wurde
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                String cookieName = cookie.getName();
                if ("LogonToken".equals(cookieName) || 
                    "MYSAPSSO2".equals(cookieName) || 
                    "InfoView_WEBAPP_TOKEN".equals(cookieName)) {
                    
                    String cookieValue = cookie.getValue();
                    if (cookieValue != null && !cookieValue.trim().isEmpty()) {
                        LOGGER.fine("Token aus Cookie '" + cookieName + "' extrahiert");
                        return cookieValue.trim();
                    }
                }
            }
        }
        
        LOGGER.warning("Kein gültiger Token in Parametern oder Cookies gefunden");
        return null;
    }

    /**
     * Führt die Token-basierte Anmeldung bei SAP BO durch.
     * Diese Methode behandelt alle Fehler und sendet die entsprechende HTTP-Antwort.
     * 
     * @param token Der Logon-Token
     * @param httpResponse Die HTTP-Response für Fehlerbehandlung
     * @return Die IEnterpriseSession oder null bei Fehler
     */
    private IEnterpriseSession performTokenLogin(String token, HttpServletResponse httpResponse) 
            throws IOException {
        
        // Validiere den Token vor der Verwendung
        if (!isValidTokenFormat(token)) {
            LOGGER.warning("Token-Format ungültig oder zu kurz: " + 
                (token != null ? token.length() : "null") + " Zeichen");
            httpResponse.sendRedirect("/BOE/BI");
            return null;
        }
        
        try {
            LOGGER.fine("Versuche Token-basierte Anmeldung bei SAP BO...");
            
            // Hole den SessionManager vom CrystalEnterprise
            ISessionMgr sessionMgr = CrystalEnterprise.getSessionMgr();
            
            if (sessionMgr == null) {
                LOGGER.severe("CMS SessionManager konnte nicht erhalten werden");
                httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, 
                    "SAP BO CMS ist derzeit nicht erreichbar");
                return null;
            }
            
            // Erstelle eine neue Sitzung basierend auf dem Token
            IEnterpriseSession enterpriseSession = sessionMgr.logonWithToken(token);
            
            LOGGER.info("Erfolgreich mit Token bei SAP BO angemeldet");
            return enterpriseSession;
            
        } catch (SDKException e) {
            LOGGER.log(Level.SEVERE, "Fehler bei der Token-Anmeldung: " + e.getMessage(), e);
            
            // Unterscheide zwischen verschiedenen Fehlertypen
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("connection") || 
                                         errorMessage.contains("timeout") ||
                                         errorMessage.contains("refused"))) {
                // CMS ist nicht erreichbar
                LOGGER.severe("CMS-Verbindungsfehler: " + errorMessage);
                httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, 
                    "SAP BO CMS ist derzeit nicht erreichbar");
            } else if (errorMessage != null && (errorMessage.contains("invalid") || 
                                                errorMessage.contains("expired") ||
                                                errorMessage.contains("unauthorized"))) {
                // Token ist ungültig oder abgelaufen
                LOGGER.warning("Token ist ungültig oder abgelaufen: " + errorMessage);
                httpResponse.sendRedirect("/BOE/BI");
            } else {
                // Unbekannter Fehler
                LOGGER.severe("Unbekannter SAP BO Fehler: " + errorMessage);
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Fehler bei der SAP BO Authentifizierung");
            }
            return null;
        }
    }

    /**
     * Validiert das Token-Format.
     * Ein gültiges Token sollte eine Mindestlänge haben und nicht null sein.
     */
    private boolean isValidTokenFormat(String token) {
        return token != null && !token.trim().isEmpty() && token.length() >= MIN_TOKEN_LENGTH;
    }

    /**
     * Prüft, ob der aktuelle Benutzer die erforderliche Rolle hat.
     * Dies wird durch eine CMS-Abfrage durchgeführt.
     * 
     * @param session Die SAP BO Sitzung
     * @return true wenn Benutzer die Rolle hat, false sonst
     */
    private boolean hasRequiredUserRole(IEnterpriseSession session) throws SDKException {
        try {
            // Abfrage des InfoStore, um die Gruppenmitgliedschaft zu prüfen
            IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
            
            if (infoStore == null) {
                LOGGER.severe("InfoStore konnte nicht erhalten werden");
                return false;
            }
            
            int userId = session.getUserInfo().getUserID();
            
            // Baue die CMS-Abfrage mit gesichertem Gruppennamen auf
            String escapedGroupName = escapeSAPQuery(REQUIRED_ROLE);
            String query = "SELECT SI_ID FROM CI_SYSTEMOBJECTS " +
                          "WHERE SI_KIND='UserGroup' " +
                          "AND SI_NAME='" + escapedGroupName + "' " +
                          "AND SI_GROUP_MEMBERS DESCENDANTS(" + userId + ")";
            
            LOGGER.fine("Führe Rollen-Abfrage durch: " + query);
            
            IInfoObjects results = infoStore.query(query);
            
            boolean hasRole = results != null && results.size() > 0;
            
            if (hasRole) {
                LOGGER.info("Benutzer ID " + userId + " hat Rolle: " + REQUIRED_ROLE);
            } else {
                LOGGER.warning("Benutzer ID " + userId + " hat NICHT die erforderliche Rolle: " + REQUIRED_ROLE);
            }
            
            return hasRole;
            
        } catch (SDKException e) {
            LOGGER.log(Level.SEVERE, "Fehler bei der Rollenprüfung: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Escapet Sonderzeichen im Gruppennamen für sichere Verwendung in CMS-Abfragen.
     * Dies verhindert Injection-Angriffe in der CMS-Abfrage.
     */
    private String escapeSAPQuery(String input) {
        if (input == null) {
            return "";
        }
        // Escape einfache Anführungszeichen durch Verdoppelung
        return input.replace("'", "''");
    }

    @Override
    public void destroy() {
        // Ressourcen beim Beenden des Filters freigeben
        LOGGER.info("SAPAuthFilter wird beendet");
    }
}