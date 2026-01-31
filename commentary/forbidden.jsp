<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isErrorPage="true" %>
<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>403 - Zugriff verweigert</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 20px;
        }
        
        .error-container {
            background: white;
            border-radius: 8px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
            max-width: 600px;
            padding: 40px;
            text-align: center;
        }
        
        .error-code {
            font-size: 72px;
            font-weight: bold;
            color: #667eea;
            margin-bottom: 20px;
        }
        
        h1 {
            font-size: 28px;
            color: #333;
            margin-bottom: 15px;
        }
        
        .error-message {
            font-size: 16px;
            color: #666;
            line-height: 1.6;
            margin-bottom: 30px;
        }
        
        .error-details {
            background: #f5f5f5;
            border-left: 4px solid #e74c3c;
            padding: 15px;
            margin-bottom: 30px;
            text-align: left;
            font-size: 14px;
            color: #555;
            border-radius: 4px;
        }
        
        .action-buttons {
            display: flex;
            gap: 15px;
            justify-content: center;
            flex-wrap: wrap;
        }
        
        .btn {
            padding: 12px 30px;
            border: none;
            border-radius: 4px;
            font-size: 16px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
            transition: all 0.3s ease;
        }
        
        .btn-primary {
            background: #667eea;
            color: white;
        }
        
        .btn-primary:hover {
            background: #5568d3;
        }
        
        .btn-secondary {
            background: #e0e0e0;
            color: #333;
        }
        
        .btn-secondary:hover {
            background: #d0d0d0;
        }
        
        .footer {
            margin-top: 30px;
            font-size: 12px;
            color: #999;
        }
    </style>
</head>
<body>
    <div class="error-container">
        <div class="error-code">403</div>
        <h1>Zugriff verweigert</h1>
        
        <div class="error-message">
            <p>Sie haben nicht die erforderliche Berechtigung, um diese Ressource zu öffnen.</p>
            <p>Dies kann geschehen, wenn:</p>
            <ul style="text-align: left; display: inline-block; margin-top: 10px;">
                <li>Sie nicht die Rolle <strong>ZPB_S.GF_COMMENT_BO</strong> haben</li>
                <li>Ihre Sitzung abgelaufen ist</li>
                <li>Sie nicht authentifiziert sind</li>
            </ul>
        </div>
        
        <div class="error-details">
            <strong>Fehlerdetails:</strong><br/>
            <%
                String message = (String) request.getAttribute("javax.servlet.error.message");
                if (message != null && !message.isEmpty()) {
                    out.println(message);
                } else {
                    out.println("Zugriff verweigert");
                }
            %>
        </div>
        
        <div class="action-buttons">
            <button class="btn btn-primary" onclick="window.location.href='/';">Zur Startseite</button>
            <button class="btn btn-secondary" onclick="window.history.back();">Zurück</button>
        </div>
        
        <div class="footer">
            <p>Wenn Sie glauben, dass dies ein Fehler ist, wenden Sie sich an den Administrator.</p>
        </div>
    </div>
</body>
</html>